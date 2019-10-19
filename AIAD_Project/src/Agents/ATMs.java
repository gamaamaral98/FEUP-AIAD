package Agents;
import jade.core.AID;
import jade.core.Agent;

/*
    Example on how to call a Client-Agent:
        jade.Boot atm:ATMs(2000,500,4000)
 */

public class ATMs extends Agent {

    //Amount of money the client wishes do withdraw
    private Integer moneyAvailable;
    private Integer maxAmountToWithdraw;
    private Integer maxRefillAmount;

    //Company responsible for the refill
    private AID responsibleCompany = new AID("COMPANY1", AID.ISLOCALNAME);

    protected void setup() {

        System.out.println("Hello! ATM-Agent " + getAID().getName() + " is ready!");

        //Get the amount of money available, max amount to withdraw and the max refill amount
        Object[] args = getArguments();

        if(args != null && args.length > 0){

            String moneyAvailableAux = (String) args[0];
            String maxAmountToWithdrawAux = (String) args[1];
            String maxRefillAmountAux = (String) args[2];
            System.out.println("ATM has the following status: \n"
                + "Money Available: " + moneyAvailableAux + "\n"
                    + "Max Amount to Withdraw " + maxAmountToWithdrawAux + "\n"
                    + "Max Refill Amount " + maxRefillAmountAux + "\n"
            );

            moneyAvailable = Integer.parseInt(moneyAvailableAux);
            maxAmountToWithdraw = Integer.parseInt(maxAmountToWithdrawAux);
            maxRefillAmount = Integer.parseInt(maxRefillAmountAux);

        } else {
            System.out.println("No status specified!");
            doDelete();
        }
    }

    //Agent clean-up operations
    protected void takeDown(){

        System.out.println("ATM-Agent " + getAID().getName() + " terminating");

    }
}
