import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
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
    public Position position;

    //List of known ATM machines
    private AID nearestATM = new AID("atm1", AID.ISLOCALNAME);
    private YellowPagesMiddleware yellowPagesMiddleware;

    public AID atmToWithdraw =null;

    protected void setup() {

        //Random pos
        //position = new Position();
        this.position = new Position(1,1);

        System.out.println("Hello! Client-Agent " + getAID().getName() + " is ready!");

        //Get the amount of money client wishes to withdraw as a start-up argument
        Object[] args = getArguments();

        if (args != null && args.length > 0) {

            String moneyAux = (String) args[0];
            String available = (String) args[1];
            System.out.println("Client wants to withdraw " + moneyAux);

            money = Integer.parseInt(moneyAux);
            wallet = Integer.parseInt(available);

            if(money>wallet){
                System.out.println("Money to withdraw bigger than bank account!");
                doDelete();

            }else{
                //Create middleware for yellow pages
                this.yellowPagesMiddleware = new YellowPagesMiddleware(this, "client", "client");

                //Register company to yellow pages
                this.yellowPagesMiddleware.register();

                addBehaviour(new atmResponse());
                addBehaviour(new withdrawMoneyBehaviour());
            }

        } else {
            System.out.println("No amount of money specified!");
            doDelete();
        }

    }

    //Agent clean-up operations
    protected void takeDown() {


        //Deregister from the yellow pages
        if(this.yellowPagesMiddleware!=null)
            this.yellowPagesMiddleware.deregister();


        System.out.println("Client-Agent " + getAID().getName() + " terminating");

    }

    /*
    This Behaviour simply represents the action for withdrawing money. */
    public class withdrawMoneyBehaviour extends OneShotBehaviour {
        public void action() {
            System.out.println("Client-Agent " + getAID().getName() + " is trying to withdraw " + money.toString());

            Clients client = ((Clients) myAgent);

            //Get the atms list
            AID[] atmsAgents = client.yellowPagesMiddleware.getAgentList("atm");

            //Get atms pos
            for (AID aid : atmsAgents) {
                String[] args = {money.toString(),position.x.toString(),position.y.toString()};
                Utils.sendRequest(
                        client,(ACLMessage.REQUEST), "withdraw-attempt", aid, Utils.createMessageString(args));
            }

        }
    }

    public class atmResponse extends CyclicBehaviour {
        @Override
        public void action() {

            MessageTemplate atmResponse = MessageTemplate.MatchConversationId("withdraw-attempt");
            ACLMessage atmReply = myAgent.receive(atmResponse);

            if(atmReply != null) {
                String content = atmReply.getContent();
                Clients client = (Clients) myAgent;
                System.out.println("Received atm response " + atmReply.getSender());
                System.out.println("Content: " + content);

                if(atmReply.getPerformative() == (ACLMessage.AGREE)) {
                    atmToWithdraw = (atmReply.getSender());

                    client.wallet += money;
                    System.out.println(client.getAID().getName() + "now has " + client.wallet.toString() + "\n");
                    doDelete();
                }else if(atmReply.getPerformative() == (ACLMessage.INFORM)){
                    System.out.println("Amount specified bigger than atm maximum withdraw amount ("
                            + content + ")\n");
                    doDelete();
                }else if(atmReply.getPerformative() == (ACLMessage.FAILURE)){
                    System.out.println("No money available, requesting refill. Come back later.\n");
                    doDelete();
                }
            }else{
                block();
            }

        }
    }
}

