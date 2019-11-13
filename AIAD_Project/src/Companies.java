import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Queue;

public class Companies extends Agent {

    //List of workers
    private AID[] workers = {
            new AID("worker1", AID.ISLOCALNAME),
            new AID("worker2", AID.ISLOCALNAME)
    };

    //List of ATMs that belong to the company
    //private AID[] ATMs;

    protected void setup() {

        System.out.println("Hello! Company-Agent " + getAID().getName() + " is ready!\n");

        addBehaviour(new RequestPerformer());

    }

    //Agent clean-up operations
    protected void takeDown(){

        System.out.println("Company-Agent " + getAID().getName() + " terminating");

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

                            for(int i = 0; i < workers.length; i++){
                                refillRequest.addReceiver(workers[i]);
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
                            if(negativeRsp == workers.length){
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

}