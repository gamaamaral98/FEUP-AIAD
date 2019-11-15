import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jdk.jshell.execution.Util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;

public class Companies extends Agent {

    public String id;
    //List of workers
    private HashMap<AID,WorkerInfo> workers = new HashMap<>();
    private YellowPagesMiddleware yellowPagesMiddleware;
    //List of ATMs that belong to the company
    //private AID[] ATMs;

    public Companies(String id){
        this.id=id;
    }

    protected void setup() {

        //Create middleware for yellow pages
        this.yellowPagesMiddleware = new YellowPagesMiddleware(this,"company","company");

        //Register company to yellow pages
        this.yellowPagesMiddleware.register();

        addBehaviour(new RequestPerformer());
        addBehaviour(new ListenForATMsBehaviour());
        addBehaviour(new WorkerRegistrationBehaviour());
        System.out.println("Created company: " + this.toString());
    }


    private void getNearestWorkerWithAmountAvailable(Integer refillAmount, Position atmPos) {
    }

    //Agent clean-up operations
    protected void takeDown(){

        //Deregister from the yellow pages
        this.yellowPagesMiddleware.deregister();

        System.out.println("Company-Agent " + getAID().getName() + " terminating");

    }

    @Override
    public String toString() {
        return "Companies{" +
                "id='" + id + '\'' +
                '}';
    }

    private class RequestPerformer extends Behaviour {

        private AID atm;
        private Integer amount;
        private int step = 0;

        private int negativeRsp = 0;

        public void action() {

            switch(step) {

                //Receives messages from ATMs asking for refill
                case 0:
                    MessageTemplate mt = MessageTemplate.MatchConversationId("refill-request");
                    ACLMessage refill = myAgent.receive(mt);

                    if(refill != null){

                        if(refill.getPerformative() == ACLMessage.REQUEST){

                            atm = refill.getSender();
                            amount = Integer.parseInt(refill.getContent());

                            // Send workers a message
                            ACLMessage refillRequest = new ACLMessage(ACLMessage.REQUEST);

                            for(AID worker:workers.keySet()){
                                refillRequest.addReceiver(worker);
                            }

                            refillRequest.setContent(amount.toString());
                            myAgent.send(refillRequest);

                            step = 1;
                            break;

                        }

                    }else{
                        block();
                    }

                //Awaits for workers messages
                case 1:

                    MessageTemplate mtCompany = MessageTemplate.MatchConversationId("response-company");
                    ACLMessage workersReply = myAgent.receive(mtCompany);

                    if(workersReply != null){
                        if(workersReply.getPerformative() == ACLMessage.INFORM && workersReply.getContent().equals("Positive")){
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(atm);
                            msg.setContent(amount.toString());
                            msg.setConversationId("company-response");
                            myAgent.send(msg);

                            step = 0;
                            break;
                        }else{
                            negativeRsp++;
                            if(negativeRsp == workers.size()){
                                negativeRsp = 0;
                                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                                msg.addReceiver(atm);
                                msg.setContent("No workers");
                                msg.setConversationId("company-response");
                                step = 0;
                                break;
                            }
                        }

                    }else{
                        block();
                    }

            }

        }

        public boolean done(){
            return (step == 2);
        }

    }

    private class ListenForATMsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate replyTemplate = MessageTemplate.MatchConversationId("initial-company");
            ACLMessage atmRequest = myAgent.receive(replyTemplate);

            if(atmRequest != null){

                Companies company = (Companies) myAgent;

                AID atm = atmRequest.getSender();
                Integer refillAmount = Integer.parseInt(atmRequest.getContent().split(",")[0]);
                Position atmPos = new Position(
                        Integer.parseInt(atmRequest.getContent().split(",")[1]),
                        Integer.parseInt(atmRequest.getContent().split(",")[2]));

                AID closestWorker=null;
                Integer bestDistance = Integer.MAX_VALUE;
                for(AID worker: workers.keySet()){
                    Integer distance = workers.get(worker).getDistance(atmPos);
                    if(bestDistance > distance){
                        bestDistance = distance;
                        closestWorker = worker;
                    }
                }

                if(closestWorker != null){
                    String[] args = {closestWorker.getName(),bestDistance.toString()};

                    Utils.sendRequest(company,ACLMessage.PROPOSE,"initial-company",atm,Utils.createMessageString(args));
                }else{
                    Utils.sendRequest(company,ACLMessage.FAILURE,"initial-company",atm,"");
                }


            }else{
                block();
            }
        }

    }

    private class WorkerRegistrationBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate replyTemplate = MessageTemplate.MatchConversationId("register-worker");
            ACLMessage workerRequest = myAgent.receive(replyTemplate);

            if(workerRequest != null){
                Companies company = (Companies) myAgent;
                AID worker = workerRequest.getSender();

                //Attention position has to be the first 2 arguments
                Position position = new Position(workerRequest.getContent());
                Integer moneyAvailable = Integer.parseInt(workerRequest.getContent().split(",")[2]);

                if(workerRequest.getPerformative() == ACLMessage.REQUEST){
                    company.workers.put(worker,new WorkerInfo(position,moneyAvailable));
                    System.out.println("Registered worker " + worker);
                }else if(workerRequest.getPerformative() == ACLMessage.CANCEL){
                    company.workers.remove(workerRequest.getSender());
                    System.out.println("Deregistered worker " + worker);
                }

            }else{
                block();
            }
        }
    }
}