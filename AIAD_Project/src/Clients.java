import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/*
    Example on how to call a Client-Agent:
        jade.Boot client:Clients(200,0)
 */

public class Clients extends Agent {

    //Amount of money the client wishes do withdraw
    private Integer money;
    private Integer wallet;

    //List of known ATM machines
    private AID nearestATM = new AID("atm1", AID.ISLOCALNAME);

    protected void setup() {

        System.out.println("Hello! Client-Agent " + getAID().getName() + " is ready!");

        //Get the amount of money client wishes to withdraw as a start-up argument
        Object[] args = getArguments();

        if(args != null && args.length > 0){

            String moneyAux = (String) args[0];
            String available = (String) args[1];
            System.out.println("Client wants to withdraw " + moneyAux);

            money = Integer.parseInt(moneyAux);
            wallet = Integer.parseInt(available);

        } else {
            System.out.println("No amount of money specified!");
            doDelete();
        }

        addBehaviour(new withdrawMoneyBehaviour());
        addBehaviour(new atmResponse());

    }

    //Agent clean-up operations
    protected void takeDown(){

        System.out.println("Client-Agent " + getAID().getName() + " terminating");

    }

    /*
    This Behaviour simply represents the action for withdrawing money. The message goes to the nearest ATM.
    I don't know how we will determinate the nearest ATM but that will be for later.
     */
    public class withdrawMoneyBehaviour extends OneShotBehaviour {
        public void action() {
            System.out.println("Client-Agent " + getAID().getName() + " is trying to withdraw " + money.toString());

            //Create the message for the ATM, using REQUEST
            ACLMessage req = new ACLMessage((ACLMessage.REQUEST));

            req.setConversationId("withdraw-attempt");
            req.addReceiver(nearestATM);
            req.setContent(money.toString());

            myAgent.send(req);
        }
    }

    public class atmResponse extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate atmResponse = MessageTemplate.MatchConversationId("response-client");
            ACLMessage atmReply = myAgent.receive(atmResponse);

            if(atmReply != null){

                System.out.println(atmReply.getContent());

                if(atmReply.getContent().equals("You will receive the money")){

                    wallet += money;
                    System.out.println(getAID().getName() + "now has " + wallet.toString() + "\n");
                    takeDown();

                }else if(atmReply.getContent().equals("No money available, requesting refill")){

                    System.out.println("No money available, requesting refill. Come back later.\n");
                    takeDown();

                }else{

                    System.out.println("Your value is above the maximum amount to withdraw.");
                    takeDown();

                }
            }else{
                block();
            }
        }
    }

}