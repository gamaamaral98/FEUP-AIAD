import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/*
    Example on how to call a Client-Agent:
        jade.Boot atm:ATMs(2000,500,4000)
 */

public class ATMs extends Agent {

    //Amount of money the client wishes do withdraw
    private Integer moneyAvailable;
    private Integer maxAmountToWithdraw;
    private Integer maxRefillAmount;

    //Company responsible for the refill
    private AID responsibleCompany = new AID("company1", AID.ISLOCALNAME);

    protected void setup() {

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

        addBehaviour(new ATMStepBehaviour());
    }

    //Agent clean-up operations
    protected void takeDown(){

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

                    if(withdrawMsg != null){
                        if(withdrawMsg.getPerformative() == ACLMessage.REQUEST){

                            int moneyToWithdraw = Integer.parseInt(withdrawMsg.getContent());

                            if(moneyToWithdraw > maxAmountToWithdraw){
                                ACLMessage msg = withdrawMsg.createReply();
                                msg.setContent("You can't withdraw more than " + maxAmountToWithdraw.toString());
                                msg.setConversationId("response-client");
                                myAgent.send(msg);
                            }
                            else if(moneyToWithdraw > moneyAvailable){

                                ACLMessage cliMsg = withdrawMsg.createReply();
                                cliMsg.setContent("No money available, requesting refill");
                                cliMsg.setConversationId("response-client");
                                myAgent.send(cliMsg);

                                step = 1;
                                break;
                            }
                            else{
                                moneyAvailable -= moneyToWithdraw;
                                ACLMessage msg = withdrawMsg.createReply();
                                msg.setContent("You will receive the money");
                                msg.setConversationId("response-client");

                                myAgent.send(msg);
                            }
                        }


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
}
