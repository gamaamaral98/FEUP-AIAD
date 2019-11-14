import jade.core.AID;

public class ATMWorkerChoice {
    private final Integer distance;
    private final AID worker;

    public ATMWorkerChoice(AID worker, Integer distance) {
        this.worker = worker;
        this.distance = distance;
    }

    public Integer getDistance() {
        return distance;
    }

    public AID getWorker() {
        return worker;
    }
}
