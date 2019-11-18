import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import java.lang.Thread.*;

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
    public AID company;

    //Amount available in the van
    private Integer moneyAvailable;

    private YellowPagesMiddleware yellowPagesMiddleware;

    public Position position;
    public Position headQuarters;
    private Position destiny;

    private Boolean occupied = false;

    public Workers(String workerName, String companyName, Position position, Integer moneyAvailable, Position pos){
        this.position =position;
        this.company = new AID(companyName,AID.ISLOCALNAME);
        this.moneyAvailable = moneyAvailable;
        this.headQuarters = pos;
        this.destiny = position;
    }

    protected void setup() {

        //Create middleware for yellow pages
        this.yellowPagesMiddleware = new YellowPagesMiddleware(this,"worker","worker");

        //Register company to yellow pages
        this.yellowPagesMiddleware.register();

        //addBehaviour(new refillATMBehaviour());
        addBehaviour(new registerToCompanyBehaviour());
        addBehaviour(new refillATMBehaviour());
        addBehaviour(new UpdatePrinter(this,500));
        if(Utils.debug)System.out.println("Created worker: " + this.toString());
    }

    //Agent clean-up operations
    protected void takeDown(){

        //Deregister from the yellow pages
        this.yellowPagesMiddleware.deregister();

        this.alertCompany();

        if(Utils.debug)System.out.println("Worker-Agent " + getAID().getName() + " terminating");

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


    public class Travelling extends TickerBehaviour {
        private final Integer amoutRefill;
        private final AID atmAID;

        public Travelling(Agent a, long period,AID atmAID, Integer amountRefill, Position pos) {
            super(a, period);
            this.atmAID = atmAID;
            this.amoutRefill = amountRefill;
            destiny = pos;
        }

        @Override
        protected void onTick() {
            if(destiny.getX() != position.getX() || destiny.getY() != position.getY()){
                if(destiny.getX() != position.getX() && destiny.getY() != position.getY()){
                    Random r = new Random();
                    if(r.nextInt() % 2 == 0){
                        changeX();
                    }else
                        changeY();
                }else if (destiny.getX() != position.getX()){
                    changeX();
                }else {
                    changeY();
                }
                if(Utils.debug)System.out.println(myAgent.getName() +  " postion" + position + " destiny: "+ destiny);
            }else{

                if(destiny == headQuarters){
                    moneyAvailable = 5000;
                    if(Utils.debug)System.out.println("Worker " + myAgent.getName() + "refilled Van!\n");

                }else{
                    Utils.sendRequest(myAgent,ACLMessage.INFORM,"refill-success",((Workers) myAgent).company,this.amoutRefill.toString());
                    Utils.sendRequest(myAgent, ACLMessage.CONFIRM, "resolved-refill", atmAID, amountRefill.toString());
                }
                occupied = false;
                this.stop();
            }
        }
    }

    /*
        Worker receives msg from the company with the ATM and amount to refill.
        Worker sees if he has enough money.
        If he doesn't have, simply sends a reply to the company saying Negative.
        If he does have, replies with Positive to the company and sends a msg to the target ATM to initiate the refill.
     */

    public class refillATMBehaviour extends CyclicBehaviour {
        public void action() {
            if(!occupied) {

                if (moneyAvailable <= 500) {
                    occupied = true;
                    Travelling travelling = new Travelling(myAgent, 500, null, amountRefill, headQuarters);
                    myAgent.addBehaviour(travelling);
                } else {
                    MessageTemplate refill = MessageTemplate.MatchConversationId("refill-request");
                    ACLMessage msg = myAgent.receive(refill);

                    if (msg != null) {

                        if (msg.getPerformative() == ACLMessage.PROPOSE) {

                            if(Utils.debug)System.out.println("Worker " + myAgent.getName() + " received message to refill");
                            AID company = msg.getSender();
                            Workers worker = (Workers) myAgent;
                            amountRefill = Integer.parseInt(msg.getContent());

                            if (amountRefill <= moneyAvailable && destiny.equals(position)) {
                                Utils.sendRequest(myAgent, ACLMessage.CONFIRM, "company-response", company, worker.position.toStringMsg());
                            } else {
                                Utils.sendRequest(myAgent, ACLMessage.CANCEL, "company-response", company, "");
                            }
                        } else if (msg.getPerformative() == ACLMessage.CONFIRM) {
                            occupied = true;
                            AID atm = new AID(msg.getContent(), AID.ISLOCALNAME);
                            if(Utils.debug)System.out.println("Worker " + myAgent.getName() + " refilling " + atm.getName());

                            int sep = msg.getContent().indexOf("\\");
                            if (sep == -1) {
                                if(Utils.debug)System.out.println("Error unknown message type");
                            }

                            destiny = new Position(msg.getContent().substring(sep + 1));


                            AID atmAID = new AID(msg.getContent().substring(0, sep), AID.ISGUID);


                            Travelling travelling = new Travelling(myAgent, 500, atmAID, amountRefill, destiny);
                            myAgent.addBehaviour(travelling);
                        }

                    } else {
                        block();
                    }
                }
            }else{

                MessageTemplate refill = MessageTemplate.MatchConversationId("refill-request");
                ACLMessage msg = myAgent.receive(refill);

                if (msg != null) {

                    if (msg.getPerformative() == ACLMessage.PROPOSE) {

                        if(Utils.debug)System.out.println("Worker " + myAgent.getName() + " received message to refill but he is occupied!");
                        AID company = msg.getSender();

                        Utils.sendRequest(myAgent, ACLMessage.CANCEL, "company-response", company, "");

                    }

                } else {
                    block();
                }
            }
        }
    }

    private void changeX(){
        if(destiny.getX() > position.getX()){
            position.setX(position.getX()+1);
        }else
            position.setX(position.getX()-1);
    }
    private void changeY(){
        if(destiny.getY() > position.getY()){
            position.setY(position.getY()+1);
        }else
            position.setY(position.getY()-1);
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
                if(Utils.debug)System.out.println("Tried to assign company " + worker.company +"to worker " + worker.getAID() + "but an error occurred");
            }
        }
    }

    public class UpdatePrinter extends TickerBehaviour {

        public UpdatePrinter(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            //worker-update
            Workers worker = ((Workers) myAgent);
            AID printer = worker.yellowPagesMiddleware.getAgentList("printer")[0];
            Utils.sendRequest(myAgent,ACLMessage.INFORM,"worker-update",printer,worker.position.toStringMsg());
        }
    }
}
