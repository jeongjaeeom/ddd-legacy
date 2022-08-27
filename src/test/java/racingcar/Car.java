package racingcar;

public class Car {

    public static final int MAXIMUM_NAME_LENGTH = 5;
    public static final int MOVING_CONDITION = 4;
    private final String name;
    private int position;

    public Car(String name) {
        this(name, 0);
    }

    public Car(String name, int position) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("자동차의 이름은 비어 있을 수 없습니다.");
        }

        if (name.length() > MAXIMUM_NAME_LENGTH) {
            throw new IllegalArgumentException("자동차의 이름은 5글자를 넘을 수 없습니다.");
        }
        this.name = name;
        this.position = position;
    }

    public void move(MovingStrategy strategy) {
        if (strategy.movable()) {
            position++;
        }
    }

    public int getPosition() {
        return position;
    }
}
