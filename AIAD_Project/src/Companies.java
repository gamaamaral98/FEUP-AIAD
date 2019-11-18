import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

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

    public Position headQuarters = new Position();

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
        public HashMap<AID,Position> workersAvailable = new HashMap<>();
        private int negativeRsp = 0;
        public Position atmPos;

        public void action() {
            switch(step) {
                //Receives messages from ATMs asking for refill
                case 0:
                    MessageTemplate mt = MessageTemplate.MatchConversationId("refill-request");
                    ACLMessage refill = myAgent.receive(mt);

                    if(refill != null){
                        System.out.println("Received refill request in company " + refill.toString());
                        if(refill.getPerformative() == ACLMessage.REQUEST){

                            atm = refill.getSender();
                            amount = Integer.parseInt(refill.getContent().split(",")[2]);
                            atmPos = new Position(refill.getContent());

                            System.out.println("Company " + myAgent.getName() + " received request to refill " + atm.getName());

                            // Send workers a message
                            ACLMessage refillRequest = new ACLMessage(ACLMessage.REQUEST);

                            for(AID worker:workers.keySet()){
                                refillRequest.addReceiver(worker);
                            }

                            refillRequest.setPerformative(ACLMessage.PROPOSE);
                            refillRequest.setConversationId("refill-request");
                            refillRequest.setContent(amount.toString());
                            myAgent.send(refillRequest);

                            step = 1;
                            break;

                        }

                    }else{
                        block();
                        break;
                    }

                //Awaits for workers messages
                case 1:
                    System.out.println("Waiting worker message");
                    MessageTemplate mtCompany = MessageTemplate.MatchConversationId("company-response");
                    ACLMessage workersReply = myAgent.receive(mtCompany);

                    if(workersReply != null){
                        if(workersReply.getPerformative() == ACLMessage.CONFIRM){
                            System.out.println("Received confirm from worker to refill atm");
                            AID worker = workersReply.getSender();
                            Position workerPosition = new Position(workersReply.getContent());
                            workersAvailable.put(worker,workerPosition);
                        }else if(workersReply.getPerformative() == ACLMessage.CANCEL){
                            negativeRsp++;
                        }

                        if(workersAvailable.size() + negativeRsp == workers.size()){

                            //Companies company = (Companies) myAgent;
                            AID selectedWorker = selectWorker();

                            if(selectedWorker == null){
                                System.out.println("No worker available, auctioning atm " + atm.getLocalName());
                                myAgent.addBehaviour(new AuctionATM());
                            }else{
                                //inform worker to refill
                                Utils.sendRequest(myAgent,ACLMessage.CONFIRM,"refill-request",selectedWorker,atm.getName() + "\\" + atmPos.toStringMsg());
                                System.out.println("Worker " + selectedWorker + " selected to refill atm ");
                            }

                            //Clear workers available for next refill iteration
                            workersAvailable.clear();
                            step=0;
                            System.out.println("Ended search for workers");
                        }

                        break;
                    }else{
                        block();
                        break;
                    }

            }

        }

        public  AID selectWorker() {
            AID bestWorker = null;
            Integer bestDist = Integer.MAX_VALUE;

            for(AID worker:workersAvailable.keySet()){
                Integer dist = workersAvailable.get(worker).getDistance(atmPos);
                if(dist < bestDist){
                    bestDist = dist;
                    bestWorker = worker;
                }
            }

            return bestWorker;
        }

        public boolean done(){
            return (step == 2);
        }

    }

    private class AuctionATM extends Behaviour{
        public Integer step = 0;
        @Override
        public void action() {
            switch (step){
                case 0:
                    //Send messages to companies to initiate auction
                    Companies company = (Companies) myAgent;
                    AID companies[] = company.yellowPagesMiddleware.getAgentList("company");
                    System.out.println("Found " + companies.length + " companies");
                    step =1;
                    break;
                case 1:
                    //Dormir 1 seg
                    //Se tiver mensagem processar e guardar
                    //Senão, terminar leilão
                    //sleep(1000);

            }
        }

        @Override
        public boolean done() {
            return step==1;
        }
    }

    private class ListenForATMsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate replyTemplate = MessageTemplate.MatchConversationId("initial-company");
            ACLMessage atmRequest = myAgent.receive(replyTemplate);

            if(atmRequest != null){
                //System.out.println("Company received initial-company message");
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
