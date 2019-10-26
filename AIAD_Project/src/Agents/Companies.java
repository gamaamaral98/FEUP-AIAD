package Agents;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class Companies extends Agent {

    //List of workers
    private AID[] workers;

    //List of ATMs that belong to the company
    private AID[] ATMs;

    //ATM that needs refill
    private AID refillATM;
    private Integer amountNeeded;

    private Integer requestWorkerFlag = 0;
    private Integer foundWorkerFlag = 0;

    protected void setup() {

        System.out.println("Hello! Company-Agent " + getAID().getName() + " is ready!");

    }

    //Agent clean-up operations
    protected void takeDown(){

        System.out.println("Company-Agent " + getAID().getName() + " terminating");

    }

}