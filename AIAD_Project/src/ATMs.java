import jade.core.AID;
import jade.core.Agent;
import jade.core.Service;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.Property;
import jdk.jshell.execution.Util;

import java.util.HashMap;

/*
    Example on how to call a Client-Agent:
        jade.Boot atm:ATMs(2000,500,4000)
 */

public class ATMs extends Agent {

    //Amount of money the client wishes do withdraw
    private Integer moneyAvailable;
    private Integer maxAmountToWithdraw;
    private Integer maxRefillAmount;
    private Position position;
    private YellowPagesMiddleware yellowPagesMiddleware;
    //Company responsible for the refill
    private AID responsibleCompany = new AID("company1", AID.ISLOCALNAME);
    private AID[] companies;
    public AID currentCompany = new AID("", AID.ISLOCALNAME);

    protected void setup() {

        //Random pos
        //this.position = new Position();
        this.position = new Position(1,1);
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

        //Create middleware for yellow pages
        this.yellowPagesMiddleware = new YellowPagesMiddleware(this,"atm","atm");

        //Register atm to yellow pages
        this.yellowPagesMiddleware.register();
        addBehaviour(new InitialCompanyBehaviour());

    }

    //Agent clean-up operations
    protected void takeDown(){

        //Deregister from the yellow pages
        this.yellowPagesMiddleware.deregister();

        System.out.println("ATM-Agent " + getAID().getName() + " terminating");

    }

    public class ATMStepBehaviour extends Behaviour {
        private int step = 0;
        public void action() {

            switch (step) {

                //the ATM awaits for withdraw attempts
                case 0:
                    MessageTemplate mt = MessageTemplate.MatchConversationId("withdraw-attempt");
                    ACLMessage withdrawMsg = myAgent.receive(mt);
                    System.out.println(withdrawMsg);
                    if(withdrawMsg != null){

                        ATMs atm = (ATMs) myAgent;

                        String content = withdrawMsg.getContent();

                        Position positionOfClient = new Position(content.split(",")[1],content.split(",")[2]);

                        if(!positionOfClient.equals(atm.position)){
                            //Not same position
                            break;
                        }

                        Integer moneyToWithdraw = Integer.parseInt(content.split(",")[0]);
                        AID sender = withdrawMsg.getSender();
                        String conversationID = "withdraw-attempt";

                        //Maximum amount to withdraw exceeded
                        if(moneyToWithdraw> atm.maxAmountToWithdraw){
                            Utils.sendRequest(atm,
                                    ACLMessage.INFORM,conversationID,
                                    sender,atm.maxAmountToWithdraw.toString());
                            break;
                        //Needs refill
                        }else if(moneyToWithdraw > atm.moneyAvailable){
                            Utils.sendRequest(atm,
                                    ACLMessage.FAILURE,conversationID,
                                    sender,"");
                            step = 1;
                            break;
                        }

                        //Proceed with transacton
                        atm.moneyAvailable -= moneyToWithdraw;
                        Utils.sendRequest(atm,ACLMessage.AGREE,conversationID,sender,"");

                        System.out.println("ATM " + atm.getLocalName() + " now has " + atm.moneyAvailable +" available.\n");
                    }else{
                        block();
                    }
                    break;

                //case it doesn't have money
                case 1:

                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

                    msg.addReceiver(responsibleCompany);
                    msg.setConversationId("refill-request");
                    Integer amountNeeded = maxRefillAmount - moneyAvailable;
                    msg.setContent(amountNeeded.toString());

                    myAgent.send(msg);

                    step = 2;
                    break;

                //Response from company
                case 2:

                    MessageTemplate responseFromCompany = MessageTemplate.MatchConversationId("company-response");
                    ACLMessage response = myAgent.receive(responseFromCompany);

                    if(response != null){
                        if(response.getContent().equals("No workers")){
                            step = 3;
                            break;
                        }else{
                            int refill = Integer.parseInt(response.getContent());
                            moneyAvailable += refill;
                            step = 0;
                            break;
                        }
                    }
                    else{
                        block();
                    }
                    break;

                case 3:
                    System.out.println("COMECEM A FAZER CONTRATOS");
                    break;

            }

        }

        public boolean done() {
            return (step == 3);
        }
    }

    public class InitialCompanyBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            ATMs atm = (ATMs) myAgent;

            atm.companies = atm.yellowPagesMiddleware.getAgentList("company");

            int messageType = ACLMessage.REQUEST;
            String conversationID = "initial-company";
            Integer refillAmount = atm.maxRefillAmount-atm.moneyAvailable;

            String[] args = {refillAmount.toString(),atm.position.x.toString(),atm.position.y.toString()};

            for (AID aid:atm.companies) {
                Utils.sendRequest(atm,messageType,conversationID,aid,args);
            }

            System.out.println("Sent request to initial company");

            atm.addBehaviour(new GetInitialProposalsBehaviour());
            atm.addBehaviour(new ATMStepBehaviour());
        }
    }

    public class GetInitialProposalsBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            System.out.println("Starting recovery of initial company responses");

            ATMs atm = (ATMs) myAgent;
            HashMap<AID,ATMWorkerChoice> closestCompanyWorkerFromATM = new HashMap<>();

            while(closestCompanyWorkerFromATM.size() != companies.length){
                MessageTemplate replyTemplate = MessageTemplate.MatchConversationId("initial-company");
                ACLMessage companyReply = myAgent.receive(replyTemplate);

                if(companyReply != null){
                    AID company = companyReply.getSender();
                    Integer distance= Integer.MAX_VALUE;
                    AID worker = null;
                    if(companyReply.getPerformative() == ACLMessage.PROPOSE){

                        worker  = new AID(companyReply.getContent(),AID.ISGUID);
                        distance  = Integer.parseInt(companyReply.getContent().split(",")[1]);

                    }else if(companyReply.getPerformative() == ACLMessage.FAILURE){

                    }

                    if(closestCompanyWorkerFromATM.put(worker,new ATMWorkerChoice(worker,distance)) != null)
                        System.out.println("In ATMs/GetInitialProposalsBehaviour - It seems that the same atm was registered\n");

                    System.out.println("Received response from " + company.toString());
                }else{
                    block();
                }
            }
            Integer lastDistance = Integer.MAX_VALUE;
            for (AID aid:closestCompanyWorkerFromATM.keySet()) {
                ATMWorkerChoice choice = closestCompanyWorkerFromATM.get(aid);
                if(choice == null)
                    continue;

                Integer dist = choice.getDistance();

                if(dist < lastDistance)
                    atm.currentCompany = aid;
            }

            if(atm.currentCompany == null){
                System.out.println("Couldn't assign a company, something unexpected happened, terminating atm\n");
                doDelete();
            }else{
                System.out.println("Initial company to atm: " + atm.getAID() + "\n\t" + "Company: "+ atm.currentCompany );
            }
        }

    }
}
