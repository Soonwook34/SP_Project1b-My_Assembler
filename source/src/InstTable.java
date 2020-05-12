import java.io.*;
import java.util.HashMap;


/**
 * 모든 instruction의 정보를 관리하는 클래스. instruction data들을 저장한다
 * 또한 instruction 관련 연산, 예를 들면 목록을 구축하는 함수, 관련 정보를 제공하는 함수 등을 제공 한다.
 */
public class InstTable {
    /**
     * inst.data 파일을 불러와 저장하는 공간.
     * 명령어의 이름을 집어넣으면 해당하는 Instruction의 정보들을 리턴할 수 있다.
     */
    HashMap<String, Instruction> instMap;

    /**
     * 클래스 초기화. 파싱을 동시에 처리한다.
     *
     * @param instFile : instuction에 대한 명세가 저장된 파일 이름
     */
    public InstTable(String instFile) {
        instMap = new HashMap<>();
        openFile(instFile);
    }

    /**
     * 입력받은 이름의 파일을 열고 해당 내용을 파싱하여 instMap에 저장한다.
     */
    public void openFile(String fileName) {
        try {
            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            //한 줄씩 instruction 정보 가져와서 저장
            while ((line = bufferedReader.readLine()) != null) {
                Instruction inst = new Instruction(line);
                instMap.put(inst.instruction, inst);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
/**
 * 명령어 하나하나의 구체적인 정보는 Instruction클래스에 담긴다.
 * instruction과 관련된 정보들을 저장하고 기초적인 연산을 수행한다.
 */
class Instruction {

    String instruction; //명령어 이름
    int opcode;         //명령어 Opcode
    int operandNum;     //명령어 피연산자 개수

    /**
     * instruction이 몇 바이트 명령어인지 저장. 이후 편의성을 위함
     */
    int format;

    /**
     * 클래스를 선언하면서 일반문자열을 즉시 구조에 맞게 파싱한다.
     *
     * @param line : instruction 명세파일로부터 한줄씩 가져온 문자열
     */
    public Instruction(String line) {
        parsing(line);
    }

    /**
     * 일반 문자열을 파싱하여 instruction 정보를 파악하고 저장한다.
     *
     * @param line : instruction 명세파일로부터 한줄씩 가져온 문자열
     */
    public void parsing(String line) {
        //탭(\t)으로 분리
        String[] info = line.split("\t");
        if (info.length == 4) {
            instruction = info[0];
            format = Integer.parseInt(info[1]);
            opcode = Integer.parseInt(info[2], 16);
            operandNum = Integer.parseInt(info[3]);
        }
    }
}
