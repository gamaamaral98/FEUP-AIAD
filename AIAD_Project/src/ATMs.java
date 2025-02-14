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


import java.util.HashMap;

import static java.lang.Thread.sleep;

/*
    Example on how to call a Client-Agent:
        jade.Boot atm:ATMs(2000,500,4000)
 */

public class ATMs extends Agent {

    public String atmName;
    //Amount of money the client wishes do withdraw
    private Integer moneyAvailable;
    private Integer maxAmountToWithdraw;
    private Integer maxRefillAmount;
    public Position position;
    private YellowPagesMiddleware yellowPagesMiddleware;
    //Company responsible for the refill
    private AID responsibleCompany = new AID("company1", AID.ISLOCALNAME);
    private AID[] companies;
    public AID currentCompany = new AID("", AID.ISLOCALNAME);


    public ATMs(String atmName, Integer moneyAvailable, Integer maxAmountToWithdraw, Integer maxRefillAmount, Position position) {
        this.atmName = atmName;
        this.moneyAvailable = moneyAvailable;
        this.maxAmountToWithdraw = maxAmountToWithdraw;
        this.maxRefillAmount = maxRefillAmount;
        this.position = position;
    }

    protected void setup() {

        //Create middleware for yellow pages
        this.yellowPagesMiddleware = new YellowPagesMiddleware(this, "atm", "atm");

        //Register atm to yellow pages
        this.yellowPagesMiddleware.register();
        addBehaviour(new InitialCompanyBehaviour());

    }

    //Agent clean-up operations
    protected void takeDown() {

        //Deregister from the yellow pages
        this.yellowPagesMiddleware.deregister();

        if(Utils.debug)System.out.println("ATM-Agent " + getAID().getName() + " terminating");

    }

    public class ATMStepBehaviour extends Behaviour {
        private int step = 0;

        public void action() {

            ATMs atm = (ATMs) myAgent;
            if(Utils.debug)System.out.println(atm.atmName + ": " + atm.moneyAvailable);

            switch (step) {

                //the ATM awaits for withdraw attempts
                case 0:
                    MessageTemplate mt = MessageTemplate.MatchConversationId("withdraw-attempt");
                    ACLMessage withdrawMsg = myAgent.receive(mt);
                    if (withdrawMsg != null) {
                        if(Utils.debug)System.out.println("Received withdraw attempt");
                        String content = withdrawMsg.getContent();

                        Position positionOfClient = new Position(content.split(",")[1], content.split(",")[2]);

                        if (!positionOfClient.equals(atm.position)) {
                            //Not same position
                            break;
                        }

                        Integer moneyToWithdraw = Integer.parseInt(content.split(",")[0]);
                        AID sender = withdrawMsg.getSender();
                        String conversationID = "withdraw-attempt";

                        //Maximum amount to withdraw exceeded
                        if (moneyToWithdraw > atm.maxAmountToWithdraw) {
                            Utils.sendRequest(atm,
                                    ACLMessage.INFORM, conversationID,
                                    sender, atm.maxAmountToWithdraw.toString());
                            break;
                            //Needs refill
                        } else if (moneyToWithdraw > atm.moneyAvailable) {
                            Utils.sendRequest(atm,
                                    ACLMessage.FAILURE, conversationID,
                                    sender, "");
                            step = 1;
                            break;
                        }

                        //Proceed with transacton
                        atm.moneyAvailable -= moneyToWithdraw;
                        Utils.sendRequest(atm, ACLMessage.AGREE, conversationID, sender, "");

                        if(Utils.debug)System.out.println("ATM " + atm.getLocalName() + " now has " + atm.moneyAvailable + " available.\n");
                    } else {
                        block();
                    }
                    break;

                //case it doesn't have money
                case 1:

                    ((ATMs) myAgent).sendRequestToRefill();
                    step = 2;
                    break;

                //Received money
                case 2:

                    /*MessageTemplate refillTriggered = MessageTemplate.MatchConversationId("resolved-refill");
                    ACLMessage response = myAgent.receive(refillTriggered);*/
                    ACLMessage response = myAgent.receive();

                    if (response != null) {
                        if(response.getConversationId() == "resolved-refill"){
                            //Company sold rights to the atm, now needs to wait for another
                            if (response.getPerformative() == ACLMessage.PROPAGATE) {
                                //New company sends notice to propagate information
                                atm.currentCompany = response.getSender();
                                ((ATMs) myAgent).sendRequestToRefill();
                                block();
                                break;
                            }
                            //Worker refilled
                            else if (response.getPerformative() == ACLMessage.CONFIRM) {
                                atm.moneyAvailable += Integer.parseInt(response.getContent());
                                if(Utils.debug)System.out.println("ATM "+ myAgent.getName() + " now has "+ ((ATMs) myAgent).moneyAvailable );
                                step=0;
                            }
                        } else if(response.getConversationId() == "withdraw-attempt"){
                            Utils.sendRequest(atm,
                                    ACLMessage.FAILURE, "withdraw-attempt",
                                    response.getSender(), "");
                        }
                    } else {
                        block();
                    }
            }

        }

        public boolean done() {
            return (step == 3);
        }
    }

    private void sendRequestToRefill() {
        Integer moneyNeeded = (this.maxRefillAmount - this.moneyAvailable);

        //Inform company
        String args = this.position.toStringMsg() + "," + moneyNeeded.toString();
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Utils.sendRequest(
                this, ACLMessage.REQUEST, "refill-request",
                this.currentCompany, args);
        if(Utils.debug)System.out.println("ATM " + this.getName() + " sent refill request to " + this.currentCompany.getLocalName());
    }

    public class InitialCompanyBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            ATMs atm = (ATMs) myAgent;

            atm.companies = atm.yellowPagesMiddleware.getAgentList("company");

            int messageType = ACLMessage.REQUEST;
            String conversationID = "initial-company";
            Integer refillAmount = atm.maxRefillAmount - atm.moneyAvailable;

            String[] args = {refillAmount.toString(), atm.position.x.toString(), atm.position.y.toString()};

            atm.addBehaviour(new GetInitialProposalsBehaviour());

            for (AID aid : atm.companies) {
                Utils.sendRequest(atm, messageType, conversationID, aid, args);
            }

            if(Utils.debug)System.out.println("Sent request to initial company to " + atm.companies.length + " companies");

            atm.addBehaviour(new ATMStepBehaviour());
        }
    }

    public class GetInitialProposalsBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            if(Utils.debug)System.out.println("Starting recovery of initial company responses");

            ATMs atm = (ATMs) myAgent;
            HashMap<AID, ATMWorkerChoice> closestCompanyWorkerFromATM = new HashMap<>();
            Integer negativeRsp = 0;
            while (closestCompanyWorkerFromATM.size() + negativeRsp != companies.length) {
                MessageTemplate replyTemplate = MessageTemplate.MatchConversationId("initial-company");
                ACLMessage companyReply = myAgent.receive(replyTemplate);

                if (companyReply != null) {
                    AID company = companyReply.getSender();
                    Integer distance = Integer.MAX_VALUE;
                    AID worker = null;
                    if (companyReply.getPerformative() == ACLMessage.PROPOSE) {

                        worker = new AID(companyReply.getContent(), AID.ISGUID);
                        distance = Integer.parseInt(companyReply.getContent().split(",")[1]);

                    } else if (companyReply.getPerformative() == ACLMessage.FAILURE) {
                        negativeRsp++;
                    }

                    if (closestCompanyWorkerFromATM.put(company, new ATMWorkerChoice(worker, distance)) != null)
                        if(Utils.debug)System.out.println("In ATMs/GetInitialProposalsBehaviour - It seems that the same atm was registered "+ atm.getAID().getName()+"\n");

                    if(Utils.debug)System.out.println("Received response from " + company.toString());
                } else {
                    block();
                }
            }
            Integer lastDistance = Integer.MAX_VALUE;
            if(Utils.debug)System.out.println(closestCompanyWorkerFromATM);
            for (AID aid : closestCompanyWorkerFromATM.keySet()) {
                if(Utils.debug)System.out.println(aid);
                ATMWorkerChoice choice = closestCompanyWorkerFromATM.get(aid);
                if (choice == null)
                    continue;

                Integer dist = choice.getDistance();

                if (dist < lastDistance)
                    atm.currentCompany = aid;
            }


            if (atm.currentCompany == null) {
                if(Utils.debug)System.out.println("Couldn't assign a company, something unexpected happened, terminating atm\n");
                doDelete();
            } else {
                if(Utils.debug)System.out.println("Initial company to atm: " + atm.getAID() + "\n\t" + "Company: " + atm.currentCompany);
            }
        }

    }
}
