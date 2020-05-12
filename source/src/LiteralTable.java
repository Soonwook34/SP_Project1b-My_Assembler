import java.util.ArrayList;

/**
 * literal과 관련된 데이터와 연산을 소유한다.
 * section 별로 하나씩 인스턴스를 할당한다.
 */
public class LiteralTable {
    ArrayList<String> literalList;
    ArrayList<Integer> locationList;

    ArrayList<Character> typeList;      //리터럴의 타입(X 또는 C)을 저장

    public LiteralTable() {
        literalList = new ArrayList<>();
        locationList = new ArrayList<>();
        typeList = new ArrayList<>();
    }

    /**
     * 새로운 Literal을 table에 추가한다.
     *
     * @param literal  : 새로 추가되는 literal의 label
     * @param location : 해당 literal이 가지는 주소값
     *                 주의 : 만약 중복된 literal이 putLiteral을 통해서 입력된다면 이는 프로그램 코드에 문제가 있음을 나타낸다.
     *                 매칭되는 주소값의 변경은 modifyLiteral()을 통해서 이루어져야 한다.
     */
    public void putLiteral(String literal, int location) {
        literalList.add(literal);
        locationList.add(location);
    }

    /**
     * 기존에 존재하는 literal 값에 대해서 가리키는 주소값을 변경한다.
     *
     * @param literal     : 변경을 원하는 literal의 label
     * @param newLocation : 새로 바꾸고자 하는 주소값
     */
    public void modifyLiteral(String literal, int newLocation) {
        locationList.set(literalList.indexOf(literal), newLocation);
    }

    /**
     * 인자로 전달된 literal이 어떤 주소를 지칭하는지 알려준다.
     *
     * @param literal : 검색을 원하는 literal의 label
     * @return literal이 가지고 있는 주소값. 해당 literal이 없을 경우 -1 리턴
     */
    public int search(String literal) {
        int address;
        //리터럴이 있는지 확인
        int index = literalList.indexOf(literal);
        //있다면 locationList에서 location 값 가져오기
        if (index == -1)
            address = -1;
        else
            address = locationList.get(index);
        return address;
    }
}
