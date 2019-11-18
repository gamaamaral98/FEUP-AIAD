import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MapPrinter extends Agent {

    public YellowPagesMiddleware yellowPagesMiddleware;
    Position atms[];
    Position companies[];

    String baseMap[][] = new String[Utils.mapSizeX][Utils.mapSizeY];

    public HashMap<Integer,Position> workers = new HashMap<>();

    public MapPrinter(Position atms[],Position companies[]){
        this.atms = atms;
        this.companies = companies;

        for(Position atm:atms){
            this.baseMap[atm.getX()][atm.getY()] = "A";
        }

        for(Position company:companies){
            this.baseMap[company.getX()][company.getY()] = "C";
        }



    }

    protected void setup() {

        this.workers = new HashMap<>();

        //Create middleware for yellow pages
        this.yellowPagesMiddleware = new YellowPagesMiddleware(this,"printer","printer");

        //Register company to yellow pages
        this.yellowPagesMiddleware.register();

        addBehaviour(new PrintMap(this,500));
        addBehaviour(new updateReceiver());
    }

    //Agent clean-up operations
    protected void takeDown(){

        //Deregister from the yellow pages
        this.yellowPagesMiddleware.deregister();


    }

    public class PrintMap extends TickerBehaviour{

        MapPrinter printer = (MapPrinter) myAgent;

        public PrintMap(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
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
                    if(workers.containsValue(new Position(i,j))) {
                        System.out.print("W");
                    }
                    else if (map[i][j] == null) {
                        System.out.print("_");
                    }else
                        System.out.print(map[i][j]);

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
                ((MapPrinter) myAgent).workers.remove(Integer.parseInt(update.getSender().getLocalName().split("-")[0]));
                ((MapPrinter) myAgent).workers.put(Integer.parseInt(update.getSender().getLocalName().split("-")[0]),new Position(update.getContent()));
            }else{
                block();
            }
        }
    }
}
