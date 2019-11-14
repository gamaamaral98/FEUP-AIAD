import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;


public class Utils {

    public static enum withdrawActions{
        NO_MONEY_ATM,
        NO_MONEY_CLIENT;
    }


    public static String createMessageString(String[] args){
        String message = new String();
        for (int i = 0; i < args.length; i++) {
            message +=args[i];
            if(i < args.length-1) message+=",";
        }
        return message;
    }


    public static void sendRequest(Agent sender, int messageType, String conversationID, AID agent, String content) {
        ACLMessage req = new ACLMessage(messageType);
        req.setConversationId(conversationID);
        req.addReceiver(agent);
        req.setContent(content);

        sender.send(req);
    }
}
