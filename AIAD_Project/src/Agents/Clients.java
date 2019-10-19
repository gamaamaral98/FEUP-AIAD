package Agents;
import jade.core.AID;
import jade.core.Agent;

/*
    Example on how to call a Client-Agent:
        jade.Boot client:Clients(200)
 */

public class Clients extends Agent {

    //Amount of money the client wishes do withdraw
    private Integer money;

    //List of known ATM machines
    private AID[] nearATMs = {
            new AID("ATM1", AID.ISLOCALNAME),
            new AID("ATM2", AID.ISLOCALNAME)
    };

    protected void setup() {

        System.out.println("Hello! Client-Agent " + getAID().getName() + " is ready!");

        //Get the amount of money client wishes to withdraw as a start-up argument
        Object[] args = getArguments();

        if(args != null && args.length > 0){

            String moneyAux = (String) args[0];
            System.out.println("Client wants to withdraw " + moneyAux);

            money = Integer.parseInt(moneyAux);

        } else {
            System.out.println("No amount of money specified!");
            doDelete();
        }
    }

    //Agent clean-up operations
    protected void takeDown(){

        System.out.println("Client-Agent " + getAID().getName() + " terminating");

    }
}