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

    public NationalBank(int cenario){
        this.jade = Runtime.instance();

        this.profile = new ProfileImpl(true);

        this.container = jade.createMainContainer(this.profile);

        try {
            switch (cenario){
                case 1:
                    String companiesNames[] = {
                            "sibs",
                            "banco de portugal",
                            "novo banco",
                            "montepio"
                    };

                    Integer aggressiveness[] = {
                            40,
                            10,
                            10,
                            10,
                    };

                    for (int i = 0; i < companiesNames.length;i++) {
                        String companyName = companiesNames[i];
                        Integer agress = aggressiveness[i];
                        Companies company = new Companies(companyName,20000,agress);
                        AgentController companyController = this.container.acceptNewAgent(companyName,company);
                        companyController.start();

                        Integer workerNumber = 0;

                        String workerName = companyName + "-worker-" + workerNumber++;
                        Workers worker = new Workers(workerName,companyName,company.headQuarters,500, company.headQuarters);
                        AgentController workerController = this.container.acceptNewAgent(workerName,worker);
                        workerController.start();

                        String worker2Name = companyName + "-worker-" + workerNumber++;
                        Workers worker2 = new Workers(worker2Name,companyName,company.headQuarters,500, company.headQuarters);
                        AgentController worker2Controller = this.container.acceptNewAgent(worker2Name,worker2);
                        worker2Controller.start();
                    }

                    String atmsNames[] = {
                            "feup",
                            "sao joao",
                            "aliados",
                            "sao bento",
                            "feup2",
                            "sao joao2",
                            "aliados2",
                            "sao bento2",
                    };

                    sleep(1000);

                    ArrayList<ATMs> atms = new ArrayList<>();

                    for (String atmName:atmsNames) {
                        ATMs atm = new ATMs(atmName,1000,2000,5000,new Position());
                        atms.add(atm);
                        AgentController atmController = this.container.acceptNewAgent(atmName,atm);
                        atmController.start();
                    }

                    sleep(1000);

                    Random random = new Random();
                    int i = 0;
                    while (i<atmsNames.length){
                        Clients client1 = new Clients("client1."+i,random.nextInt(250)+750,1000,atms.get(i).position);
                        AgentController clientController = this.container.acceptNewAgent("client1."+i,client1);
                        clientController.start();

                        Clients client2 = new Clients("client2."+i,random.nextInt(250)+750,1000,atms.get(i).position);
                        clientController = this.container.acceptNewAgent("client2."+i,client2);
                        clientController.start();

                        i++;
                    }


            }
        } catch (StaleProxyException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void createAgents() throws StaleProxyException, InterruptedException {
        //10,100,new Position(2,2)
        String companiesNames[] = {
                "sibs",
                "banco de portugal",
                "novo banco",
                "montepio"
        };
        Integer aggressiveness[] = {
                10,
                20,
                30,
                40,
        };

        Random random = new Random();


        for (int i = 0; i < companiesNames.length;i++) {
            String companyName = companiesNames[i];
            Integer agress = aggressiveness[i];
            Companies company = new Companies(companyName,20000,agress);
            AgentController companyController = this.container.acceptNewAgent(companyName,company);
            companyController.start();

            Integer workerNumber = 0;

            String workerName = companyName + "-worker-" + workerNumber++;
            Workers worker = new Workers(workerName,companyName,new Position(),500, company.headQuarters);
            AgentController workerController = this.container.acceptNewAgent(workerName,worker);
            workerController.start();

          /*  workerName =companyName + "-worker-"+workerNumber++;
            worker = new Workers(workerName,companyName,new Position(),4000);
            workerController = this.container.acceptNewAgent(workerName,worker);
            workerController.start();*/
        }

        String atmsNames[] = {
                "feup",
                "sao joao"
        };

        sleep(1000);

        ArrayList<ATMs> atms = new ArrayList<>();

        for (String atmName:atmsNames) {
            ATMs atm = new ATMs(atmName,500,500,5000,new Position());
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
        NationalBank nationalBank = new NationalBank(1);
    }

}
