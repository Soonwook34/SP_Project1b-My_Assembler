import java.util.ArrayList;
import java.util.Arrays;

/**
 * 사용자가 작성한 프로그램 코드를 단어별로 분할 한 후, 의미를 분석하고, 최종 코드로 변환하는 과정을 총괄하는 클래스이다. <br>
 * pass2에서 object code로 변환하는 과정은 혼자 해결할 수 없고 symbolTable과 instTable의 정보가 필요하므로 이를 링크시킨다.<br>
 * section 마다 인스턴스가 하나씩 할당된다.
 *
 */
public class TokenTable {
    public static final int MAX_OPERAND=3;

    /* bit 조작의 가독성을 위한 선언 */
    public static final int nFlag=32;
    public static final int iFlag=16;
    public static final int xFlag=8;
    public static final int bFlag=4;
    public static final int pFlag=2;
    public static final int eFlag=1;

    /* Token을 다룰 때 필요한 테이블들을 링크시킨다. */
    SymbolTable symTab;
    LiteralTable literalTab;
    InstTable instTab;


    /** 각 line을 의미별로 분할하고 분석하는 공간. */
    ArrayList<Token> tokenList;

    /**
     * 초기화하면서 symTable과 instTable을 링크시킨다.
     * @param symTab : 해당 section과 연결되어있는 symbol table
     * @param instTab : instruction 명세가 정의된 instTable
     */
    public TokenTable(SymbolTable symTab, InstTable instTab) {
        this.symTab = symTab;
        this.instTab = instTab;
        tokenList = new ArrayList<>();
    }

    /**
     * 초기화하면서 literalTable과 instTable을 링크시킨다.
     * @param literalTab : 해당 section과 연결되어있는 literal table
     * @param instTab : instruction 명세가 정의된 instTable
     */
    public TokenTable(LiteralTable literalTab, InstTable instTab) {
        this.literalTab = literalTab;
        this.instTab = instTab;
        tokenList = new ArrayList<>();
    }

    /**
     * 일반 문자열을 받아서 Token단위로 분리시켜 tokenList에 추가한다.
     * @param line : 분리되지 않은 일반 문자열
     */
    public void putToken(String line) {
        tokenList.add(new Token(line));
    }

    /**
     * tokenList에서 index에 해당하는 Token을 리턴한다.
     * @param index : 리턴할 tokenList의 index
     * @return : index번호에 해당하는 코드를 분석한 Token 클래스
     */
    public Token getToken(int index) {
        return tokenList.get(index);
    }

    /**
     * Pass2 과정에서 사용한다.
     * instruction table, symbol table literal table 등을 참조하여 objectcode를 생성하고, 이를 저장한다.
     * @param index objectCode를 생성할 tokenList의 index
     */
    public void makeObjectCode(int index){
        Token token = this.getToken(index);
        Instruction inst = this.instTab.instMap.get(token.operator.replace("+", ""));
        //START나 CSECT
        if(token.operator.equals("START") || token.operator.equals("CSECT")) {
            token.record = 'H';
            token.objectCode = String.format("%-6s%06X%06X", token.label, 0, token.location);
        }
        //EXTDEF
        else if(token.operator.equals("EXTDEF")) {
            token.record = 'D';
            for(String extdef : token.operand)
                token.objectCode = token.objectCode.concat(String.format("%-6s%06X", extdef, this.symTab.search(extdef)));
        }
        //EXTREF
        else if(token.operator.equals("EXTREF")) {
            token.record = 'R';
            for(String extref : token.operand)
                token.objectCode = token.objectCode.concat(String.format("%-6s", extref));
        }
        //일반 명령어
        else if(inst != null) {
            token.record = 'T';
            //nixbpe비트를 채우고 displacement 구하기
            int displacement = setNixbpe(token, inst);
            //format에 따라 objectCode 저장
            switch(token.byteSize) {
                case 2:
                    token.objectCode = String.format("%04X", ((inst.opcode<<4 | token.nixbpe) << 4) | displacement);
                    break;
                case 3:
                    token.objectCode = String.format("%06X", ((inst.opcode<<4 | token.nixbpe) << 12) | (displacement & 0xFFF));
                    break;
                case 4:
                    token.objectCode = String.format("%08X", ((inst.opcode<<4 | token.nixbpe) << 20) | (displacement & 0xFFFFF));
                    break;
            }
        }
        //LTORG나 END
        else if(token.operator.equals("LTORG") || token.operator.equals("END")) {
            token.record = 'T';
            LiteralTable littab = this.literalTab;
            for(int i=0; i<littab.literalList.size(); i++) {
                String literal = littab.literalList.get(i);
                int value = 0;
                switch(littab.typeList.get(i)) {
                    case 'X':
                        value = Integer.parseInt(literal, 16);
                        break;
                    case 'C':
                        for(int j=0; j<literal.length(); j++) {
                            value = value << 8;
                            value |= literal.charAt(j);
                        }
                        break;
                }
                token.objectCode = token.objectCode.concat(String.format("%0"+token.byteSize*2+"X", value));
            }
        }
        //BYTE
        else if(token.operator.equals("BYTE")) {
            token.record = 'T';
            String value = token.operand[0].replace("'", "");
            int intValue = 0;
            //X이면
            if(value.startsWith("X"))
                intValue = Integer.parseInt(value.replace("X", ""), 16);
            //C이면
            else {
                value = value.replace("C", "");
                for(int i=0; i<value.length(); i++) {
                    intValue = intValue << 8;
                    intValue |= value.charAt(i);
                }
            }
            token.objectCode = String.format("%0"+token.byteSize*2+"X", intValue);
        }
        //WORD
        else if(token.operator.equals("WORD")) {
            token.record = 'T';
            int value;
            //다항 연산이면(-)
            if(token.operand[0].contains("-")) {
                String[] operand = token.operand[0].split("-");
                int addr1 = this.symTab.search(operand[0]);
                int addr2 = this.symTab.search(operand[1]);
                if(addr1 != -1 && addr2 != -1)
                    value = addr2 - addr1;
                //외부 참조이면 M레코드 추가
                else {
                    Token mToken = new Token('M', String.format("%06X06+%s", token.location, operand[0]));
                    this.tokenList.add(mToken);
                    mToken = new Token('M', String.format("%06X06-%s", token.location, operand[1]));
                    this.tokenList.add(mToken);
                    value = 0;
                }
            }
            //단항이면
            else {
                int addr = symTab.search(token.operand[0]);
                //외부 참조이면 M레코드 추가
                if(addr == -1) {
                    value = 0;
                    Token mToken = new Token('M', String.format("%06X06+%s", token.location, token.operand[0]));
                    this.tokenList.add(mToken);
                }
                else
                    value = addr;
            }
            token.objectCode = String.format("%0"+token.byteSize*2+"X", value);
        }
        //section의 마지막이면 E record 추가
        if(token.record != 'E' && index == this.tokenList.size() - 1) {
            if(this.getToken((0)).operator.equals("START")) {
                Token eToken = new Token('E', String.format("%06X", 0));
                this.tokenList.add(eToken);
            }
            else {
                Token eToken = new Token('E', "");
                this.tokenList.add(eToken);
            }
        }
    }

    /**
     * 주어진 토큰 라인이 명령어인 경우 nixbpe를 설정하고 displacement를 리턴한다
     * @param token : nixbpe를 설정할 토큰
     * @return : displacement
     */
    private int setNixbpe(Token token, Instruction inst) {
        int displacement = 0;
        //ni비트
        //2byte format이면(레지스터 연산)
        if(inst.format == 2) {
            //n = 0, i = 0
            token.setFlag(nFlag, 0);
            token.setFlag(iFlag, 0);
        }
        //3~4byte format이면
        else {
            //n = 1, i = 1으로 초기화
            token.setFlag(nFlag, 1);
            token.setFlag(iFlag, 1);
            if(inst.operandNum > 0) {
                //immediate addressing이면 n = 0
                if(token.operand[0].startsWith("#"))
                    token.setFlag(nFlag, 0);
                    //indirect addressing이면 i = 0
                else if(token.operand[0].startsWith("@"))
                    token.setFlag(iFlag, 0);
            }
        }
        //x비트
        if(token.operand.length > 1 && token.operand[1].equals("X"))
            token.setFlag(xFlag, 1);
        else
            token.setFlag(xFlag, 0);
        //bp비트
        //2byte format이면(레지스터 연산)
        if(token.byteSize == 2) {
            token.setFlag(bFlag, 0);
            token.setFlag(pFlag, 0);
            ArrayList<String> rList = new ArrayList<>(Arrays.asList("A", "X", "L", "B", "S", "T", "F", "", "PC", "SW"));
            switch(token.operand.length) {
                case 2:
                    displacement = rList.indexOf(token.operand[1]);
                case 1:
                    displacement |= rList.indexOf(token.operand[0]) << 4;
                    break;
            }
        }
        else {
            //immediate addressing 이면 b = 0, p = 0
            if(token.getFlag(nFlag | iFlag) == iFlag) {
                token.setFlag(bFlag, 0);
                token.setFlag(pFlag, 0);
                displacement = Integer.parseInt(token.operand[0].replace("#", ""));
            }
            //일반적인 3-4byte format
            else {
                token.setFlag(bFlag, 0);
                token.setFlag(pFlag, 1);
                //symtab, littab에서 검색
                int target = this.symTab.search(token.operand[0].replace("@", ""));
                //symtab에 있으면 displacement를 계산하여 b,p비트 입력
                if(target != -1) {
                    if (Math.abs(target - (token.location + token.byteSize)) <= 0x7FF)
                        displacement = target - (token.location + token.byteSize);
                    else {
                        token.setFlag(bFlag, 1);
                        token.setFlag(pFlag, 0);
                    }
                }
                //littab에 있으면 displacement를 계산하여 b,p비트 입력
                else {
                    String literal = token.operand[0];
                    literal = literal.replace("=C'", "");
                    literal = literal.replace("=X'", "");
                    literal = literal.replace("'", "");
                    target = this.literalTab.search(literal);
                    if(target != -1) {
                        if (Math.abs(target - (token.location + token.byteSize)) <= 0x7FF)
                            displacement = target - (token.location + token.byteSize);
                        else {
                            token.setFlag(bFlag, 1);
                            token.setFlag(pFlag, 0);
                        }
                    }
                    else {
                        token.setFlag(pFlag, 0);
                        if(literal.length() > 0) {
                            Token mToken = new Token('M', String.format("%06X05+%s", token.location + 1, literal));
                            this.tokenList.add(mToken);
                        }
                    }
                }
            }
        }
        //e비트
        if(token.byteSize == 4)
            token.setFlag(eFlag, 1);
        else
            token.setFlag(eFlag, 0);

        return displacement;
    }

    /**
     * index번호에 해당하는 object code를 리턴한다.
     * @param index : 원하는 object code의 index
     * @return : object code
     */
    public String getObjectCode(int index) {
        return getToken(index).objectCode;
    }

}

/**
 * 각 라인별로 저장된 코드를 단어 단위로 분할한 후  의미를 해석하는 데에 사용되는 변수와 연산을 정의한다.
 * 의미 해석이 끝나면 pass2에서 object code로 변형되었을 때의 바이트 코드 역시 저장한다.
 */
class Token {
    //의미 분석 단계에서 사용되는 변수들
    int location;
    String label;
    String operator;
    String[] operand;
    String comment;
    char nixbpe;

    // object code 생성 단계에서 사용되는 변수들
    String objectCode;
    int byteSize;   //생성되는 object code의 byte 크기
    char record;    //토큰의 레코드 정보(H, D, R, T, M, E)

    /**
     * 클래스를 초기화 하면서 바로 line의 의미 분석을 수행한다.
     *
     * @param line 문장단위로 저장된 프로그램 코드
     */
    public Token(String line) {
        //초기화
        label = "";
        operator = "";
        operand = new String[0];
        comment = "";
        objectCode = "";
        parsing(line);
    }

    /**
     * M, E레코드를 추가하는 경우의 생성자
     *
     * @param record     M 또는 E 레코드
     * @param objectCode 생성된 objectCode
     */
    public Token(char record, String objectCode) {
        this.record = record;
        this.objectCode = objectCode;
        label = "";
        operator = "";
        operand = new String[0];
        comment = "";
    }

    /**
     * line의 실질적인 분석을 수행하는 함수. Token의 각 변수에 분석한 결과를 저장한다.
     *
     * @param line 문장단위로 저장된 프로그램 코드.
     */
    public void parsing(String line) {
        //분리
        String[] info = line.split("\t");
        //입력
        switch (info.length) {
            case 4:
                comment = info[3];
            case 3:
                operand = info[2].split(",");
            case 2:
                operator = info[1];
            case 1:
                label = info[0];
                break;
        }
    }

    /**
     * n,i,x,b,p,e flag를 설정한다.
     * <p>
     * 사용 예 : setFlag(nFlag, 1);
     * 또는     setFlag(TokenTable.nFlag, 1);
     *
     * @param flag  : 원하는 비트 위치
     * @param value : 집어넣고자 하는 값. 1또는 0으로 선언한다.
     */
    public void setFlag(int flag, int value) {
        //먼저 비트를 채우고
        nixbpe |= flag;
        //value가 0이면 다시 빼준다
        if (value == 0)
            nixbpe -= flag;
    }

    /**
     * 원하는 flag들의 값을 얻어올 수 있다. flag의 조합을 통해 동시에 여러개의 플래그를 얻는 것 역시 가능하다
     * <p>
     * 사용 예 : getFlag(nFlag)
     * 또는     getFlag(nFlag|iFlag)
     *
     * @param flags : 값을 확인하고자 하는 비트 위치
     * @return : 비트위치에 들어가 있는 값. 플래그별로 각각 32, 16, 8, 4, 2, 1의 값을 리턴할 것임.
     */
    public int getFlag(int flags) {
        return nixbpe & flags;
    }
}