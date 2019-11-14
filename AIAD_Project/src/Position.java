import java.util.Objects;
import java.util.Random;

public class Position {
    public Integer x;
    public Integer y;

    public Position(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public Position(String[] split){
        this.x =Integer.parseInt(split[0]);
        this.y = Integer.parseInt(split[1]);
    }

    public Position() {
        Random random = new Random();

        this.x = random.nextInt(100);
        this.y = random.nextInt(100);
    }

    public Position(String x, String y) {
        this.x = Integer.parseInt(x);
        this.y = Integer.parseInt(y);
    }

    public Position(String positionInString) {
        this.x = Integer.parseInt(positionInString.split(",")[0]);
        this.y = Integer.parseInt(positionInString.split(",")[1]);
    }

    public Integer getX() {
        return x;
    }

    public Position setX(Integer x) {
        this.x = x;
        return this;
    }

    public Integer getY() {
        return y;
    }

    public Position setY(Integer y) {
        this.y = y;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position position = (Position) o;
        return Objects.equals(getX(), position.getX()) &&
                Objects.equals(getY(), position.getY());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY());
    }

    public Integer getDistance(Position position) {
        return Math.abs(this.x - position.x) + Math.abs(this.y-position.y);
    }
}
