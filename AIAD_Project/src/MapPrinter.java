import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class MapPrinter extends Agent {

    public YellowPagesMiddleware yellowPagesMiddleware;
    ArrayList<Position> atms = new ArrayList<>();
    ArrayList<Position> companies = new ArrayList<>();

    String baseMap[][] = new String[Utils.mapSizeX][Utils.mapSizeY];
    public ArrayList<String> workID = new ArrayList<>();

    public HashMap<String,Position> workers = new HashMap<>();

    public MapPrinter(Position atms[],Position companies[]){
        this.atms = new ArrayList<>(Arrays.asList(atms));
        this.companies = new ArrayList<>(Arrays.asList(companies));

        for(Position atm:atms){
            this.baseMap[atm.getX()][atm.getY()] = "A";
        }

        for(Position company:companies){
            this.baseMap[company.getX()][company.getY()] = "C";
        }

        addBehaviour(new ReceiveBankrupt());

    }

    protected void setup() {

        this.workers = new HashMap<>();

        //Create middleware for yellow pages
        this.yellowPagesMiddleware = new YellowPagesMiddleware(this,"printer","printer");

        //Register company to yellow pages
        this.yellowPagesMiddleware.register();

        addBehaviour(new PrintMap(this,1000));
        addBehaviour(new updateReceiver());
    }

    //Agent clean-up operations
    protected void takeDown(){

        //Deregister from the yellow pages
        this.yellowPagesMiddleware.deregister();


    }

    public class ReceiveBankrupt extends CyclicBehaviour{

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchConversationId("bankrupt");
            ACLMessage msg = myAgent.receive(mt);

            if(msg != null){

                if(msg.getContent().equals("worker")) {
                    if(Utils.debug)System.out.println("Removed worker " + msg.getSender().getLocalName()+" from printer");
                    workers.remove(msg.getSender().getLocalName());
                }else {
                    if(Utils.debug)System.out.println("Removed company " + msg.getSender().getLocalName()+ " from printer");
                    companies.remove(new Position(msg.getContent()));
                }

                ((MapPrinter) myAgent).updateMap();

            }else{
                block();
            }

        }
    }

    private void updateMap() {


        String baseMapNew[][] = new String[Utils.mapSizeX][Utils.mapSizeY];

        for(Position atm:atms){
            this.baseMap[atm.getX()][atm.getY()] = "A";
        }

        for(Position company:companies){
            this.baseMap[company.getX()][company.getY()] = "C";
        }

        this.baseMap = baseMapNew;
    }

    public class PrintMap extends TickerBehaviour{

        MapPrinter printer = (MapPrinter) myAgent;

        public PrintMap(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            System.out.println(printer.companies);
            String map[][] = ((MapPrinter) myAgent).baseMap.clone();
            for(Position atm:printer.atms){
                map[atm.getX()][atm.getY()] = "A";
            }

            for(Position company:printer.companies){
                map[company.getX()][company.getY()] = "C";
            }

            System.out.println("\n\n");

            for(int i = 0; i < map.length;i++){
                for(int j=0; j < map[i].length;j++){
                    Position current = new Position(i,j);
                    if(companies.contains(current)){
                        System.out.print("C");
                    }
                    else if(atms.contains(current)){
                        System.out.print("A");
                    }
                    else if(workers.containsValue(current)) {
                        System.out.print("W");
                    }
                    else {
                        System.out.print("_");
                    }

                }
                System.out.println("\n");
            }
        }
    }


    public class updateReceiver extends CyclicBehaviour {


        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchConversationId("worker-update");
            ACLMessage update = myAgent.receive(mt);

            if(update != null){
                ((MapPrinter) myAgent).workers.remove(update.getSender().getLocalName());
                ((MapPrinter) myAgent).workers.put(update.getSender().getLocalName(),new Position(update.getContent()));
            }else{
                block();
            }
        }
    }
}
