import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.sql.Array;
import java.util.*;

public class Companies extends Agent {
    public Integer money ;
    public String id;
    //List of workers
    public HashMap<AID,WorkerInfo> workers = new HashMap<>();
    private YellowPagesMiddleware yellowPagesMiddleware;
    //List of ATMs that belong to the company
    //private AID[] ATMs;
    public Integer aggressiveness;
    public Position headQuarters;

    public Companies(String id, Integer money, Integer aggressiveness, Position headQuarters){
        this.id=id;
        this.money = money;
        this.aggressiveness = aggressiveness;
        this.headQuarters = headQuarters;

    }

    protected void setup() {

        //Create middleware for yellow pages
        this.yellowPagesMiddleware = new YellowPagesMiddleware(this,"company","company");

        //Register company to yellow pages
        this.yellowPagesMiddleware.register();


        addBehaviour(new WorkerMessageReceiver());
        addBehaviour(new RemoveMoney(this,Utils.MILLISSECONDS));
        addBehaviour(new AuctionHandler());
        addBehaviour(new RequestPerformer());
        addBehaviour(new ListenForATMsBehaviour());
        addBehaviour(new WorkerRegistrationBehaviour());
        if(Utils.debug)System.out.println("Created company: " + this.toString());
    }


    private void getNearestWorkerWithAmountAvailable(Integer refillAmount, Position atmPos) {
    }

    public class WorkerMessageReceiver extends CyclicBehaviour{

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchConversationId("refill-success");
            ACLMessage refill = myAgent.receive(mt);

            if(refill != null){
                ((Companies) myAgent).money +=Integer.parseInt(refill.getContent());
            }else{
                block();
            }

        }
    }
    //Agent clean-up operations
    protected void takeDown(){

        for(AID worker:this.workers.keySet()){
            Utils.sendRequest(this,ACLMessage.PROPAGATE,"bankrupt",worker,"");
        }

        AID printer =  this.yellowPagesMiddleware.getAgentList("printer")[0];
        Utils.sendRequest(this,ACLMessage.INFORM,"bankrupt",printer,this.headQuarters.toStringMsg());

        //Deregister from the yellow pages
        this.yellowPagesMiddleware.deregister();

        if(Utils.debug)System.out.println("Company-Agent " + getAID().getName() + " terminating");

    }

    @Override
    public String toString() {
        return "Companies{" +
                "id='" + id + '\'' +
                '}';
    }

    public class RemoveMoney extends TickerBehaviour{

        public RemoveMoney(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {

            Companies company = (Companies) myAgent;

            if(Utils.debug)System.out.println("Company " + company.getAID().getLocalName() + " with money=" + company.money);
            company.money-= company.workers.size()*20;
            if(Utils.debug)System.out.println("Company " + company.getAID().getLocalName() + "  with money=" + company.money);
            if(company.money < 0){
                System.out.println("Company " + company.getAID().getLocalName() + " went bankrupt with money=" + money);
                company.doDelete();
            }
        }
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
                        if(Utils.debug)System.out.println("Received refill request in company " + refill.toString());
                        if(refill.getPerformative() == ACLMessage.REQUEST){
                            Companies company = (Companies) myAgent;

                            company.sendRefillRequest(refill);
                            atm = refill.getSender();
                            atmPos = new Position(refill.getContent());
                            amount = Integer.parseInt(refill.getContent().split(",")[2]);
                            step = 1;
                            break;

                        }

                    }else{
                        block();
                        break;
                    }

                //Awaits for workers messages
                case 1:
                    if(Utils.debug)System.out.println("Waiting worker message");
                    MessageTemplate mtCompany = MessageTemplate.MatchConversationId("company-response");
                    ACLMessage workersReply = myAgent.receive(mtCompany);

                    if(workersReply != null){
                        AID worker = workersReply.getSender();
                        if(workersReply.getPerformative() == ACLMessage.CONFIRM){
                            Position workerPosition = new Position(workersReply.getContent());
                            if(Utils.debug)System.out.println("Received confirm from worker to refill atm");
                            workersAvailable.put(worker,workerPosition);
                        }else if(workersReply.getPerformative() == ACLMessage.CANCEL){
                            negativeRsp++;
                        }

                        if(workersAvailable.size() + negativeRsp == workers.size()){

                            //Companies company = (Companies) myAgent;
                            AID selectedWorker = selectWorker();

                            if(selectedWorker == null){
                                if(Utils.debug)System.out.println("No worker available, auctioning atm " + atm.getLocalName());
                                myAgent.addBehaviour(new WakerAuctionATM(myAgent,1000,atm));
                            }else{
                                //inform worker to refill
                                Utils.sendRequest(myAgent,ACLMessage.CONFIRM,"refill-request",selectedWorker,atm.getName() + "\\" + atmPos.toStringMsg());
                                if(Utils.debug)System.out.println("Worker " + selectedWorker + " selected to refill atm ");
                            }

                            //Clear workers available for next refill iteration
                            workersAvailable.clear();
                            step=0;
                            if(Utils.debug)System.out.println("Ended search for workers");
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

    private void sendRefillRequest(ACLMessage refill) {
        AID atm = refill.getSender();
        Integer amount = Integer.parseInt(refill.getContent().split(",")[2]);
        Position atmPos = new Position(refill.getContent());

        if(Utils.debug)System.out.println("Company " + this.getName() + " received request to refill " + atm.getName());

        // Send workers a message
        ACLMessage refillRequest = new ACLMessage(ACLMessage.REQUEST);

        for(AID worker:workers.keySet()){
            refillRequest.addReceiver(worker);
        }

        refillRequest.setPerformative(ACLMessage.PROPOSE);
        refillRequest.setConversationId("refill-request");
        refillRequest.setContent(amount.toString());
        this.send(refillRequest);

    }

    public class AuctionHandler extends CyclicBehaviour{
         public Integer step = 0;
        public AID atmAID;
        @Override
        public void action() {
            switch (step){
                case 0:
                    //receber mensagem de auction e enviar primeira auction
                    MessageTemplate template = MessageTemplate.MatchConversationId("initiate-auction");
                    ACLMessage msg = myAgent.receive(template);

                    if(msg != null){
                        if(Utils.debug)System.out.println("Received initiate-auction in " + myAgent.getAID().getLocalName());
                        Integer bid = this.decide(0);
                        if(bid > 0){
                            step=1;
                            atmAID = new AID(msg.getContent(),AID.ISLOCALNAME);
                            Utils.sendRequest(myAgent,ACLMessage.PROPOSE,"auction-response",msg.getSender(),bid.toString());
                            if(Utils.debug)System.out.println("Sending auction-response");
                        }else{
                            //Backed out
                            //Do nothing
                            if(Utils.debug)System.out.println(myAgent.getAID().getLocalName() + " backed out in 1st bid (" + msg.getContent() + ")");
                        }
                    }else{
                        block();
                    }
                    break;
                case 1:
                    //Receber resposta,
                    //  Se negativa decidir se envia nova
                    //  Se positiva meter step 2
                    MessageTemplate template2 = MessageTemplate.MatchConversationId("auction-response");
                    ACLMessage res = myAgent.receive(template2);

                    if(res != null){
                        if(res.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
                            Utils.sendRequest(myAgent,ACLMessage.PROPAGATE,"resolved-refill",atmAID,"");
                            ((Companies) myAgent).money -= Integer.parseInt(res.getContent());
                            step=2;
                        }else if(res.getPerformative() == ACLMessage.REJECT_PROPOSAL){
                            Integer lastBid = Integer.parseInt(res.getContent());
                            Integer bid = this.decide(lastBid);
                            if(bid> lastBid){
                                Utils.sendRequest(myAgent,ACLMessage.PROPOSE,"auction-response",res.getSender(),bid.toString());

                            }else{
                                //Backed out
                                //Do nothing
                                if(Utils.debug)System.out.println(myAgent.getAID().getLocalName() + " backed out in bid (" + res.getContent() + ")");
                                step=0;
                            }
                        }
                    }else{
                        block();
                    }

                    break;
                case 2:
                    //Atualizar current company e falar com a atm para dar refill
                    atmAID = null;
                    step=0;
                    break;
            }
        }

        private Integer decide(Integer lastBid) {
            Companies company = (Companies) myAgent;
            Integer maxBid = (((Companies) myAgent).money*20/100)*(100+ company.aggressiveness)/100;
            if(Utils.debug)System.out.println(myAgent.getName()+" max bid: " + maxBid);

            if(lastBid< maxBid){
                return Math.min(maxBid,lastBid+100);
            }else{
                return 0;
            }
        }
    }

    public class AuctionATM extends TickerBehaviour{
        private final AID atmAID;
        Integer step = 0;
        AID bestCompanyProposalAID;
        Integer bestBid;
        Integer tries= 0;

        public AuctionATM(Agent a, long period, AID atmAID) {
            super(a, period);
            this.atmAID = atmAID;
            this.bestBid = 0;
            this.bestCompanyProposalAID = myAgent.getAID();

        }

        @Override
        protected void onTick() {
            switch (step) {
                case 0:
                    //Send messages to companies to initiate auction
                    Companies company = (Companies) myAgent;
                    ArrayList<AID> companies =new ArrayList<AID>(Arrays.asList(company.yellowPagesMiddleware.getAgentList("company")));
                    companies.remove(myAgent.getAID());
                    if(Utils.debug)System.out.println("Found " + companies.size() + " companies to auction atm " + atmAID.getLocalName() + " which was from " + myAgent.getAID().getLocalName());

                    for(AID companyAID:companies){
                       Utils.sendRequest(myAgent,ACLMessage.CFP,"initiate-auction",companyAID,atmAID.getLocalName());
                    }
                    step = 1;
                    break;
                case 1:
                    //Receive proposals
                    MessageTemplate proposalTemplate = MessageTemplate.MatchConversationId("auction-response");
                    ACLMessage companyProposal = myAgent.receive(proposalTemplate);

                    if(companyProposal != null){
                        if(companyProposal.getPerformative() == ACLMessage.PROPOSE){
                            Integer bid = Integer.parseInt(companyProposal.getContent());
                            if(bid > bestBid){
                                if(bestBid!=0)
                                    Utils.sendRequest(myAgent,ACLMessage.REJECT_PROPOSAL,"auction-response",bestCompanyProposalAID,bid.toString());
                                bestBid = bid;
                                bestCompanyProposalAID = companyProposal.getSender();
                                if(Utils.debug)System.out.println("Received new best proposal from " + bestCompanyProposalAID.getLocalName() + " with value " + bestBid);
                            }else{
                                if(Utils.debug)System.out.println("Sending reject proposal to " + companyProposal.getSender().getLocalName());
                                Utils.sendRequest(myAgent,ACLMessage.REJECT_PROPOSAL,"auction-response",companyProposal.getSender(),bestBid.toString());
                            }
                        }
                        tries=0;
                    }else if(tries >20){
                        Utils.sendRequest(myAgent,ACLMessage.ACCEPT_PROPOSAL,"auction-response",bestCompanyProposalAID,bestBid.toString());
                        System.out.println("Ended auction giving atm " + atmAID.getLocalName() + " to " + bestCompanyProposalAID.getLocalName() +" for " + bestBid);
                        this.stop();
                    }
                    break;

            }
            tries++;
        }
    }

    private class WakerAuctionATM extends WakerBehaviour{
        public Integer step = 0;
        public AID atmAID;

        public WakerAuctionATM(Agent a, long timeout, AID atmAID) {
            super(a, timeout);
            this.atmAID = atmAID;
        }

        protected void onWake(){
            addBehaviour(new AuctionATM(myAgent,5,atmAID));
        }


    }

    private class ListenForATMsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate replyTemplate = MessageTemplate.MatchConversationId("initial-company");
            ACLMessage atmRequest = myAgent.receive(replyTemplate);

            if(atmRequest != null){
                //if(Utils.debug)System.out.println("Company received initial-company message");
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

                if(workerRequest.getPerformative() == ACLMessage.REQUEST){
                    //Attention position has to be the first 2 arguments
                    Position position = new Position(workerRequest.getContent());
                    Integer moneyAvailable = Integer.parseInt(workerRequest.getContent().split(",")[2]);

                    company.workers.put(worker,new WorkerInfo(position,moneyAvailable));
                    if(Utils.debug)System.out.println("Registered worker " + worker);
                }else if(workerRequest.getPerformative() == ACLMessage.CANCEL){
                    company.workers.remove(workerRequest.getSender());
                    if(Utils.debug)System.out.println("Deregistered worker " + worker);
                }



            }else{
                block();
            }
        }
    }
}
