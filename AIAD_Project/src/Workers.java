import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Arrays;

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
    private Integer moneyAvailable;

    private YellowPagesMiddleware yellowPagesMiddleware;
    public Position position;

    public Workers(String workerName, String companyName, Position position, Integer moneyAvailable){
        this.position =position;
        this.company = new AID(companyName,AID.ISLOCALNAME);
        this.moneyAvailable = moneyAvailable;
    }

    protected void setup() {

        //Create middleware for yellow pages
        this.yellowPagesMiddleware = new YellowPagesMiddleware(this,"worker","worker");

        //Register company to yellow pages
        this.yellowPagesMiddleware.register();

        //addBehaviour(new refillATMBehaviour());
        addBehaviour(new registerToCompanyBehaviour());

        System.out.println("Created worker: " + this.toString());
    }

    //Agent clean-up operations
    protected void takeDown(){

        //Deregister from the yellow pages
        this.yellowPagesMiddleware.deregister();

        this.alertCompany();

        System.out.println("Worker-Agent " + getAID().getName() + " terminating");

    }

    private void alertCompany() {
        Utils.sendRequest(this,ACLMessage.CANCEL,"register-worker",company,"");
    }

    @Override
    public String toString() {
        return "Workers{" +
                " amountRefill=" + amountRefill +
                ", company=" + company +
                ", moneyAvailable=" + moneyAvailable +
                ", position=" + position +
                '}';
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

                if(amountRefill <= moneyAvailable){

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

    private class registerToCompanyBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            Workers worker = (Workers) myAgent;

            ArrayList<AID> companies = new ArrayList<>(Arrays.asList(worker.yellowPagesMiddleware.getAgentList("company")));

            if(companies.contains(worker.company)){
                String[] args = {worker.position.x.toString(),worker.position.y.toString(),worker.moneyAvailable.toString()};
                Utils.sendRequest(worker,ACLMessage.REQUEST,"register-worker",worker.company,Utils.createMessageString(args));
            }else{
                System.out.println("Tried to assign company " + worker.company +"to worker " + worker.getAID() + "but an error occurred");
            }
        }
    }
}