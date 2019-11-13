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
    private AID responsibleCompany;

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
                            }
                            else if(moneyToWithdraw > moneyAvailable){
                                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                                msg.addReceiver(responsibleCompany);
                                msg.setConversationId("refill-request");
                                Integer amountNeeded = maxRefillAmount - moneyAvailable;
                                msg.setContent(amountNeeded.toString());
                                step = 1;
                                break;
                            }
                            else{
                                moneyAvailable -= moneyToWithdraw;
                                ACLMessage msg = withdrawMsg.createReply();
                                msg.setContent("You will receive the money");
                            }
                        }


                    }else{
                        block();
                    }
                    break;

                //case it doesn't have money
                case 1:
                    MessageTemplate responseFromCompany = MessageTemplate.MatchConversationId("workers.response");
                    ACLMessage msg = myAgent.receive(responseFromCompany);

                    if(msg != null){
                        if(msg.getContent() == "No workers"){
                            step = 2;
                            break;
                        }else{
                            int refill = Integer.parseInt(msg.getContent());
                            moneyAvailable += refill;
                        }
                    }
                    else{
                        block();
                    }


                //case it looks for a new company
                case 2:
                    //AQUI FAZER OS CONTRATOS NET PARA COMPETIÇAO
                    break;

            }

        }

        public boolean done() {
            return (step == 2);
        }
    }
}
