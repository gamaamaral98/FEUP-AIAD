package Agents;
import jade.core.AID;
import jade.core.Agent;

public class Clients extends Agent {
    protected void setup() {
        //String nickname = "ATM";
        //AID id = new AID(nickname, AID.ISLOCALNAME);
        System.out.println("Hello! Client-Agent " + getAID().getName() + " is ready!");
    }
}