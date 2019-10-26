package Agents;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import org.omg.CORBA.INTERNAL;

/*
    Example on how to call a Client-Agent:
        jade.Boot worker:Workers(ATM1,COMPANY1)
 */

public class Workers extends Agent {

    //Amount of time that the worker takes to reach a specific ATM
    private float timeToReachATM;

    //ATM that needs refill
    private AID ATMtoRefill;
    private Integer amountRefill;

    //Company he works for;
    private AID company;

    //Amount available in the van
    private Integer moneyForRefills;

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

            String moneyForRefillsAux = (String) args[2];
            System.out.println("Money available for refills " + moneyForRefillsAux);
            moneyForRefills = Integer.parseInt(moneyForRefillsAux);

        } else {
            System.out.println("No Company or ATM specified");
            doDelete();
        }
    }

    //Agent clean-up operations
    protected void takeDown(){

        System.out.println("Worker-Agent " + getAID().getName() + " terminating");

    }


    /*
        Worker receives msg from the company with the ATM and amount to refill.
        Worker sees if he has enough money.
        If he doesn't have, simply sends a reply to the company saying Negative.
        If he does have, replies with Positive to the company and sends a msg to the target ATM to initiate the refill.
     */

    public class refillATMBehaviour extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive();

            if(msg != null){

                //Message will have amount/ATM
                String fullMessage = msg.getContent();
                String[] split = fullMessage.split("/");

                amountRefill = Integer.parseInt(split[1]);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);

                if(amountRefill <= moneyForRefills){

                    reply.setContent("Positive");
                    send(reply);

                    ATMtoRefill = new AID(split[2], AID.ISGUID);

                    //Message to the ATM
                    ACLMessage msgATM = new ACLMessage(ACLMessage.INFORM);

                    msg.addReceiver(ATMtoRefill);
                    msg.setContent(amountRefill.toString());

                    send(msgATM);

                }else{

                    reply.setContent("Negative");
                    send(reply);

                }

            }else{
                block();
            }
        }
    }
}