public class WorkerInfo {
    public Integer moneyAvailable;
    public Position position;

    public WorkerInfo(Position position, Integer moneyAvailable) {
        this.position = position;
        this.moneyAvailable = moneyAvailable;
    }

    public Integer getMoneyAvailable() {
        return moneyAvailable;
    }

    public WorkerInfo setMoneyAvailable(Integer moneyAvailable) {
        this.moneyAvailable = moneyAvailable;
        return this;
    }

    public Position getPosition() {
        return position;
    }

    public WorkerInfo setPosition(Position position) {
        this.position = position;
        return this;
    }

    public Integer getDistance(Position atmPos) {
        return this.position.getDistance(atmPos);
    }
}
