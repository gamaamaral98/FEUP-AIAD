package Agents;
import jade.core.AID;
import jade.core.Agent;

public class Companies extends Agent {

    //List of workers
    private AID[] workers = {
            new AID("WORKER1", AID.ISLOCALNAME),
            new AID("WORKER2", AID.ISLOCALNAME),
            new AID("WORKER3", AID.ISLOCALNAME),
            new AID("WORKER4", AID.ISLOCALNAME),
            new AID("WORKER5", AID.ISLOCALNAME)
    };

    //List of ATMs that belong to the company
    private AID[] ATMs = {
            new AID("ATM1", AID.ISLOCALNAME),
            new AID("ATM2", AID.ISLOCALNAME),
            new AID("ATM3", AID.ISLOCALNAME)
    };

    protected void setup() {

        System.out.println("Hello! Company-Agent " + getAID().getName() + " is ready!");

    }

    //Agent clean-up operations
    protected void takeDown(){

        System.out.println("Company-Agent " + getAID().getName() + " terminating");

    }
}