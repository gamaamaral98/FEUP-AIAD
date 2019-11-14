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

    protected void setup() {

        //Random pos
        //this.position = new Position();
        this.position = new Position(2,2);
        
        System.out.println("Hello! Worker-Agent " + getAID().getName() + " is ready!");

        //Get the ATM that needs to be refilled
        Object[] args = getArguments();

        if(args != null && args.length > 0){

            String moneyForRefillsAux = args[0].toString();
            this.company =  new AID(args[1].toString(),AID.ISLOCALNAME);

            if(this.company == null){
                System.out.println("Error resolving company AID ("+ this.company +") to worker (" + this.getAID() +")\n");
            }

            System.out.println("Money available for refills " + moneyForRefillsAux + "\n");
            moneyAvailable = Integer.parseInt(moneyForRefillsAux);

        } else {
            doDelete();
        }

        //Create middleware for yellow pages
        this.yellowPagesMiddleware = new YellowPagesMiddleware(this,"worker","worker");

        //Register company to yellow pages
        this.yellowPagesMiddleware.register();

        //addBehaviour(new refillATMBehaviour());
        addBehaviour(new registerToCompanyBehaviour());
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
            System.out.println("Worker company : " + worker.company);
            System.out.println("Companies:");
            for(AID aid :companies){
                System.out.println("AID: " + aid.toString() +
                        "; name: " + aid.getName() +
                        "; localname: " + aid.getLocalName());
            }
            if(companies.contains(worker.company)){
                String[] args = {worker.position.x.toString(),worker.position.y.toString(),worker.moneyAvailable.toString()};
                Utils.sendRequest(worker,ACLMessage.REQUEST,"register-worker",worker.company,Utils.createMessageString(args));
            }else{
                System.out.println("Tried to assign company " + worker.company +"to worker " + worker.getAID() + "but an error occurred");
            }
        }
    }
}