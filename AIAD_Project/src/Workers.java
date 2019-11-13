import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/*
    Example on how to call a Client-Agent:
        jade.Boot worker:Workers(ATM1,COMPANY1)
 */

public class Workers extends Agent {

    //Amount of time that the worker takes to reach a specific ATM
    private float timeToReachATM;

    //ATM that needs refill
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

            String moneyForRefillsAux = (String) args[0];
            System.out.println("Money available for refills " + moneyForRefillsAux + "\n");
            moneyForRefills = Integer.parseInt(moneyForRefillsAux);

        } else {
            doDelete();
        }

        addBehaviour(new refillATMBehaviour());
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
            ACLMessage msg = myAgent.receive();

            if(msg != null){

                amountRefill = Integer.parseInt(msg.getContent());

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);

                if(amountRefill <= moneyForRefills){

                    reply.setContent("Positive");
                    reply.setConversationId("response-company");
                    myAgent.send(reply);

                }else{

                    reply.setContent("Negative");
                    reply.setConversationId("response-company");
                    myAgent.send(reply);

                }

            }else{
                block();
            }
        }
    }
}