import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jdk.jshell.execution.Util;

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
    private AID company;

    //Amount available in the van
    private Integer moneyAvailable;

    private YellowPagesMiddleware yellowPagesMiddleware;

    public Position position;
    private Position destiny;
    
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
        addBehaviour(new refillATMBehaviour());
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
            MessageTemplate refill = MessageTemplate.MatchConversationId("refill-request");
            ACLMessage msg = myAgent.receive(refill);

            if(msg != null){

                if(msg.getPerformative() == ACLMessage.PROPOSE){
                    System.out.println("Worker " + myAgent.getName() + " received message to refill");
                    AID company = msg.getSender();
                    Workers worker = (Workers) myAgent;
                    int sep = msg.getContent().indexOf("\\");
                    if(sep == -1){
                        System.out.println("Error unknown message type");
                    }
                    amountRefill = Integer.parseInt(msg.getContent().substring(0,sep));

                    if(amountRefill <= moneyAvailable){
                        Utils.sendRequest(myAgent,ACLMessage.CONFIRM,"company-response",company,worker.position.toStringMsg());
                        destiny = new Position(msg.getContent().substring(sep+1));
                        try {
                            travelling();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        System.out.println("Dar o dinheiro");

                    }else{
                        Utils.sendRequest(myAgent,ACLMessage.CANCEL,"company-response",company,"");
                    }
                }else if(msg.getPerformative() == ACLMessage.CONFIRM){
                    AID atm = new AID(msg.getContent(),AID.ISLOCALNAME);
                    System.out.println("Worker " + myAgent.getName() + " refilling " + atm.getName());
                    //TO_DO cenas para dizer que tem de ir e sleep e assim
                }

            }else{
                block();
            }
        }
    }
    
    private void travelling() throws InterruptedException{
        while(destiny.getX() != position.getX() || destiny.getY() != position.getY()){
            if(destiny.getX() != position.getX() && destiny.getY() != position.getY()){

                Random r = new Random();
                if(r.nextInt() % 2 == 0){
                    this.changeX();
                }else
                    this.changeY();
            }else if (destiny.getX() != position.getX()){
                this.changeX();
            }else {
                this.changeY();
            }
            System.out.println("postion" + position + " destiny: "+ destiny);

            try{
                Thread.sleep(500);
            }catch (InterruptedException e){
                e.printStackTrace();
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
                System.out.println("Tried to assign company " + worker.company +"to worker " + worker.getAID() + "but an error occurred");
            }
        }
    }
}
