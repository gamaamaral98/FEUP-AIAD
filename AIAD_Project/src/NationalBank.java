import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.Random;

import static java.lang.Thread.sleep;

public class NationalBank {

    public AgentContainer container;
    public ProfileImpl profile;
    public Runtime jade ;

    public NationalBank(){
        //Get jade runtime
        this.jade = Runtime.instance();

        //Create default profile
        this.profile = new ProfileImpl(true);

        // Create a new non-main container, connecting to the default
        // main container (i.e. on this host, port 1099)
        //ContainerController cc = jade.createAgentContainer(p);

        this.container = jade.createMainContainer(this.profile);

        try {
            this.createAgents();
        } catch (StaleProxyException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void createAgents() throws StaleProxyException, InterruptedException {
        //10,100,new Position(2,2)
        String companiesNames[] = {
                "sibs",
                "banco de portugal"
        };

        Random random = new Random();

        for (String companyName:companiesNames) {

            Companies company = new Companies(companyName);
            AgentController companyController = this.container.acceptNewAgent(companyName,company);
            companyController.start();

            Integer workerNumber = 0;

            String workerName =companyName + "-worker-"+workerNumber++;
            Workers worker = new Workers(workerName,companyName,new Position(),4000);
            AgentController workerController = this.container.acceptNewAgent(workerName,worker);
            workerController.start();

            workerName =companyName + "-worker-"+workerNumber++;
            worker = new Workers(workerName,companyName,new Position(),4000);
            workerController = this.container.acceptNewAgent(workerName,worker);
            workerController.start();
        }

        String atmsNames[] = {
                "feup",
                "sao joao"
        };

        sleep(100);

        ArrayList<ATMs> atms = new ArrayList<>();

        for (String atmName:atmsNames) {
            ATMs atm = new ATMs(atmName,300,500,800,new Position());
            atms.add(atm);
            AgentController atmController = this.container.acceptNewAgent(atmName,atm);
            atmController.start();
        }

        sleep(1000);

        Clients client1 = new Clients("client1",500,1000,atms.get(0).position);
        AgentController clientController = this.container.acceptNewAgent("client1",client1);
        clientController.start();

        Clients client2 = new Clients("client2",200,500,atms.get(0).position);
        clientController = this.container.acceptNewAgent("client2",client2);
        clientController.start();



    }

    public static void main(String args[]){
        NationalBank nationalBank = new NationalBank();
    }
}
