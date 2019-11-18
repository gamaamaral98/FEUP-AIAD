import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;


public class Utils {

    public static int MILLISSECONDS = 1000;

    public static Integer mapSizeX = 10;
    public static Integer mapSizeY = 100;

    public static boolean debug = true;


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


    public static void sendRequest(Agent sender, int messageType, String conversationID, AID receiverAID, String content) {
        ACLMessage req = new ACLMessage(messageType);
        req.setConversationId(conversationID);
        req.addReceiver(receiverAID);
        req.setContent(content);

        sender.send(req);
    }

    public static void sendRequest(Agent sender, int messageType, String conversationID, AID receiverAID, String[] args) {
        ACLMessage req = new ACLMessage(messageType);
        req.setConversationId(conversationID);
        req.addReceiver(receiverAID);
        req.setContent(Utils.createMessageString(args));

        sender.send(req);
    }

}
