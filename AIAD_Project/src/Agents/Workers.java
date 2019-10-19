package Agents;
import jade.core.AID;
import jade.core.Agent;

/*
    Example on how to call a Client-Agent:
        jade.Boot worker:Workers(ATM1,COMPANY1)
 */

public class Workers extends Agent {

    //Amount of time that the worker takes to reach a specific ATM
    private float timeToReachATM;
    //ATM that needs refill
    private AID ATMtoRefill;
    //Company he works for;

    //Company he works for
    private AID company;

    protected void setup() {

        System.out.println("Hello! Worker-Agent " + getAID().getName() + " is ready!");

        //Get the ATM that needs to be refilled
        Object[] args = getArguments();

        if(args != null && args.length > 0){

            String ATMtoRefillAux = (String) args[0];
            System.out.println("ATM that needs refill: " + ATMtoRefillAux);
            ATMtoRefill = new AID(ATMtoRefillAux, AID.ISLOCALNAME);

            String CompanyAux = (String) args[1];
            System.out.println("Company Responsible: " + CompanyAux);
            company = new AID(CompanyAux, AID.ISLOCALNAME);

        } else {
            System.out.println("No Company or ATM specified");
            doDelete();
        }
    }

    //Agent clean-up operations
    protected void takeDown(){

        System.out.println("Worker-Agent " + getAID().getName() + " terminating");

    }
}