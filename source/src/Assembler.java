import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Assembler :
 * 이 프로그램은 SIC/XE 머신을 위한 Assembler 프로그램의 메인 루틴이다.
 * 프로그램의 수행 작업은 다음과 같다.
 * 1) 처음 시작하면 Instruction 명세를 읽어들여서 assembler를 세팅한다.
 * 2) 사용자가 작성한 input 파일을 읽어들인 후 저장한다.
 * 3) input 파일의 문장들을 단어별로 분할하고 의미를 파악해서 정리한다. (pass1)
 * 4) 분석된 내용을 바탕으로 컴퓨터가 사용할 수 있는 object code를 생성한다. (pass2)
 *
 *
 * 작성중의 유의사항 :
 *  1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 안된다.
 *  2) 마찬가지로 작성된 코드를 삭제하지 않으면 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.
 *  3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.
 *  4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)
 *
 *
 *  + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class Assembler {
    /**
     * instruction 명세를 저장한 공간
     */
    InstTable instTable;
    /**
     * 읽어들인 input 파일의 내용을 한 줄 씩 저장하는 공간.
     */
    ArrayList<String> lineList;
    /**
     * 프로그램의 section별로 symbol table을 저장하는 공간
     */
    ArrayList<SymbolTable> symtabList;
    /**
     * 프로그램의 section별로 literal table을 저장하는 공간
     */
    ArrayList<LiteralTable> literaltabList;
    /**
     * 프로그램의 section별로 프로그램을 저장하는 공간
     */
    ArrayList<TokenTable> TokenList;
    /**
     * Token, 또는 지시어에 따라 만들어진 오브젝트 코드들을 출력 형태로 저장하는 공간.
     * 필요한 경우 String 대신 별도의 클래스를 선언하여 ArrayList를 교체해도 무방함.
     */
    ArrayList<String> codeList;

    /**
     * 클래스 초기화. instruction Table을 초기화와 동시에 세팅한다.
     *
     * @param instFile : instruction 명세를 작성한 파일 이름.
     */
    public Assembler(String instFile) {
        instTable = new InstTable(instFile);
        lineList = new ArrayList<>();
        symtabList = new ArrayList<>();
        literaltabList = new ArrayList<>();
        TokenList = new ArrayList<>();
        codeList = new ArrayList<>();
    }

    /**
     * 어셈블러의 메인 루틴
     */
    public static void main(String[] args) {

        Assembler assembler = new Assembler("inst.data");
        assembler.loadInputFile("input.txt");
        assembler.pass1();

        assembler.printSymbolTable("symtab_20160290.txt");
        assembler.printLiteralTable("literaltab_20160290.txt");
        assembler.pass2();
        assembler.printObjectCode("output_20160290.txt");

    }

    /**
     * inputFile을 읽어들여서 lineList에 저장한다.
     *
     * @param inputFile : input 파일 이름.
     */
    private void loadInputFile(String inputFile) {
        try {
            File file = new File(inputFile);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null)
                lineList.add(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * pass1 과정을 수행한다.
     * 1) 프로그램 소스를 스캔하여 토큰단위로 분리한 뒤 토큰테이블 생성
     * 2) label을 symbolTable에 정리
     * <p>
     * 주의사항 : SymbolTable과 TokenTable은 프로그램의 section별로 하나씩 선언되어야 한다.
     */
    private void pass1() {
        //TokenList 먼저 채우기
        getTokenList();

        //TokenList를 이용하여 symtabList와 literaltabList 완성하기
        int locctr, prevLoc;
        boolean hasLiteral = false;
        //한 줄씩 읽으면서
        for (TokenTable section : TokenList) {
            locctr = prevLoc = 0;
            for (Token line : section.tokenList) {
                //주소 입력 및 계산
                line.location = prevLoc;
                line.byteSize = getLength(line);
                locctr += line.byteSize;

                //symtab 저장
                if (line.label.length() > 0)
                    addSymbol(TokenList.indexOf(section), line);

                //littab 임시 저장
                if (line.operand.length > 0 && line.operand[0].startsWith("=")) {
                    hasLiteral = true;
                    String literal = line.operand[0];
                    literal = literal.replace("=C'", "");
                    literal = literal.replace("=X'", "");
                    literal = literal.replace("'", "");
                    if (section.literalTab.search(literal) == -1) {
                        if (line.operand[0].charAt(1) == 'X') {
                            section.literalTab.putLiteral(literal, 1);
                        } else      //'C'
                            section.literalTab.putLiteral(literal, 2);
                    }
                }
                //littab 저장
                if (hasLiteral && (line.operator.equals("LTORG") || line.operator.equals("END"))) {
                    hasLiteral = false;
                    line.byteSize = addLiteral(TokenList.indexOf(section), locctr);
                    locctr += line.byteSize;
                }
                prevLoc = locctr;
            }
            //Section이 바뀔 때 첫 라인에 Section의 길이 저장
            section.tokenList.get(0).location = locctr;
        }
    }

    /**
     * TokenList를 채우는 함수
     */
    private void getTokenList() {
        int section = -1;
        for (String line : lineList) {
            //Section 분리를 위한 임시 분리
            String[] arr = line.split("\t");
            //주석 무시
            if (arr[0].equals("."))
                continue;
            //새로운 Section이 시작되면 새로 할당
            if (arr[1].equals("START") || arr[1].equals("CSECT")) {
                section++;
                symtabList.add(new SymbolTable());
                literaltabList.add(new LiteralTable());
                TokenList.add(new TokenTable(symtabList.get(section), instTable));
                TokenList.get(section).literalTab = literaltabList.get(section);
            }
            //토큰화
            TokenList.get(section).putToken(line);
        }
    }

    /**
     * 토큰을 가져와 토큰이 차지하는 메모리의 크기를 리턴
     *
     * @param token : 메모리 크기를 계산할 토큰
     * @return : 토큰이 차지하는 메모리 크기
     */
    private int getLength(Token token) {
        int locctr = 0;
        //instTable에 있는 명령어라면 (주소 계산)
        String op = token.operator.replace("+", "");
        if (instTable.instMap.get(op) != null) {
            locctr += instTable.instMap.get(op).format;
            if (token.operator.charAt(0) == '+')
                locctr++;
        } else if (token.operator.equals("RESW"))
            locctr += Integer.parseInt(token.operand[0]) * 3;
        else if (token.operator.equals("RESB"))
            locctr += Integer.parseInt(token.operand[0]);
        else if (token.operator.equals("WORD"))
            locctr += 3;
        else if (token.operator.equals("BYTE")) {
            if (token.operand[0].charAt(0) == 'X')
                locctr += (token.operand[0].length() - 3) / 2;
            else    //C인 경우
                locctr += token.operand[0].length() - 3;
        }
        return locctr;
    }

    /**
     * SymbolTable에 주어진 symbol을 저장한다
     *
     * @param line : 현재 라인
     */
    private void addSymbol(int section, Token line) {
        SymbolTable symtab = TokenList.get(section).symTab;
        symtab.putSymbol(line.label, 0);
        //EQU이고
        if (line.operator.equals("EQU")) {
            //-연산을 하면
            if (line.operand[0].contains("-")) {
                String[] operand = line.operand[0].split("-");
                int addr1 = symtab.search(operand[0]);
                int addr2 = symtab.search(operand[1]);
                if (addr1 != -1 && addr2 != -1) {
                    symtab.modifySymbol(line.label, addr1 - addr2);
                } else
                    symtab.modifySymbol(line.label, line.location);
                line.location = symtab.search(line.label);
            }
            //현재 주소 저장(*)이면
            else if (line.operand[0].equals("*")) {
                symtab.modifySymbol(line.label, line.location);
            }
            //단항이면
            else {
                int addr = symtab.search(line.operand[0]);
                //외부 참조이면
                if (addr == -1)
                    addr = 0;
                symtab.modifySymbol(line.label, addr);
            }
        } else
            symtab.modifySymbol(line.label, line.location);
    }

    /**
     * 임시 저장된 LiteralTable의 리터럴에 주소를 할당한다.
     *
     * @param locctr : 현재 주소
     * @return : 리터럴이 차지한 메모리 크기
     */
    private int addLiteral(int section, int locctr) {
        int size = 0;
        LiteralTable littab = TokenList.get(section).literalTab;
        //현재 섹션에 임시 저장되어있던 리터럴들에게 주소 할당
        for (String literal : littab.literalList) {
            switch (littab.search(literal)) {
                case 1:     //X인 경우
                    littab.modifyLiteral(literal, locctr + size);
                    size += literal.length() / 2;
                    littab.typeList.add('X');
                    break;
                case 2:     //C인 경우
                    littab.modifyLiteral(literal, locctr + size);
                    size += literal.length();
                    littab.typeList.add('C');
                    break;
            }
        }
        return size;
    }

    /**
     * 작성된 SymbolTable들을 출력형태에 맞게 출력한다.
     *
     * @param fileName : 저장되는 파일 이름
     */
    private void printSymbolTable(String fileName) {
        try {
            File file = new File(fileName);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            //SYMTAB 출력
            if (file.isFile() && file.canWrite()) {
                for (SymbolTable section : symtabList) {
                    for (int i = 0; i < section.symbolList.size(); i++)
                        bufferedWriter.write(String.format("%-6s\t%04X\n", section.symbolList.get(i), section.locationList.get(i)));
                    bufferedWriter.newLine();
                }
                bufferedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 작성된 LiteralTable들을 출력형태에 맞게 출력한다.
     *
     * @param fileName : 저장되는 파일 이름
     */
    private void printLiteralTable(String fileName) {
        try {
            File file = new File(fileName);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            //LITTAB 출력
            if (file.isFile() && file.canWrite()) {
                for (LiteralTable section : literaltabList) {
                    for (int i = 0; i < section.literalList.size(); i++)
                        bufferedWriter.write(String.format("%-6s\t%04X\n", section.literalList.get(i), section.locationList.get(i)));
                    bufferedWriter.newLine();
                }
                bufferedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * pass2 과정을 수행한다.
     * 1) 분석된 내용을 바탕으로 object code를 생성하여 codeList에 저장.
     */
    private void pass2() {
        //한 줄씩 읽으면서
        for (TokenTable section : TokenList) {
            for (int i = 0; i < section.tokenList.size(); i++)
                section.makeObjectCode(i);
        }
    }

    /**
     * 작성된 codeList를 출력형태에 맞게 출력한다.
     *
     * @param fileName : 저장되는 파일 이름
     */
    private void printObjectCode(String fileName) {
        //최종 object code를 만들어 codeList에 저장
        makeCodeList();
        //만들어진 최종 object code 출력
        try {
            File file = new File(fileName);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            if (file.isFile() && file.canWrite()) {
                for (String finalCode : codeList)
                    bufferedWriter.write(finalCode);
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 최종 object code를 만들어 codeList에 저장한다.
     */
    private void makeCodeList() {
        String objectCode = "";
        int locctr, length, index;
        //codeList에 최종 object code 생성
        for (TokenTable section : TokenList) {
            for (int i = 0; i < section.tokenList.size(); i++) {
                Token token = section.getToken(i);
                switch (token.record) {
                    case 'H':
                    case 'D':
                    case 'R':
                    case 'M':
                    case 'E':
                        objectCode = String.format("%c%s\n", token.record, section.getObjectCode(i));
                        break;
                    case 'T':
                        //입력 전에 미리 길이 계산
                        locctr = token.location + token.byteSize;
                        length = token.byteSize;
                        for (index = i + 1; index < section.tokenList.size(); index++) {
                            token = section.getToken(index);
                            if (token.record == 0)
                                continue;
                            else if (token.record != 'T')        //T 레코드가 아니면
                                break;
                            if (token.location != locctr)        //서로 떨어져있으면
                                break;
                            if (length + token.byteSize > 0x1E)  //제한된 길이를 초과하면
                                break;
                            locctr += token.byteSize;
                            length += token.byteSize;
                        }
                        token = section.getToken(i);
                        objectCode = String.format("%c%06X%02X", token.record, token.location, length);
                        //T레코드 입력
                        for (int j = i; j < index; j++)
                            objectCode = objectCode.concat(String.format("%s", section.getObjectCode(j)));
                        objectCode = objectCode.concat("\n");
                        i = index - 1;
                        break;
                }
                //codeList에 저장
                this.codeList.add(objectCode);
            }
            this.codeList.add("\n");
        }
    }
}