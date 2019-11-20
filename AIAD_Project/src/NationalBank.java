import jade.core.AID;
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

                    Position CompaniesPos[] = {
                            new Position(),
                            new Position(),
                            new Position(),
                            new Position(),
                    };

                    Position ATMPos[] = {
                            new Position(),
                            new Position(),
                            new Position(),
                            new Position(),
                            new Position(),
                            new Position(),
                            new Position(),
                            new Position(),
                    };

                    MapPrinter printer = new MapPrinter(ATMPos,CompaniesPos);
                    AgentController printerController = this.container.acceptNewAgent("printer",printer);
                    printerController.start();

                    sleep(1000);

                    for (int i = 0; i < companiesNames.length;i++) {
                        String companyName = companiesNames[i];
                        Integer agress = aggressiveness[i];
                        Companies company = new Companies(companyName,20000,agress,CompaniesPos[i]);
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

                    for (int i = 0; i < atmsNames.length; i++) {
                        ATMs atm = new ATMs(atmsNames[i],1000,2000,5000,ATMPos[i]);
                        atms.add(atm);
                        AgentController atmController = this.container.acceptNewAgent(atmsNames[i],atm);
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
                    break;
                case 2:
                     String companiesNames2[] = {
                            "sibs",
                            "banco de portugal",
                            "novo banco",
                            "montepio"
                    };

                    Integer aggressiveness2[] = {
                            10,
                            10,
                            10,
                            10,
                    };

                    Position CompaniesPos2[] = {
                            new Position(),
                            new Position(),
                            new Position(),
                            new Position(),
                    };

                    Position ATMPos2[] = {
                            new Position(),
                            new Position(),
                            new Position(),
                            new Position(),
                            new Position(),
                            new Position(),
                            new Position(),
                            new Position(),
                    };

                    MapPrinter printer2 = new MapPrinter(ATMPos2,CompaniesPos2);
                    AgentController printerController2 = this.container.acceptNewAgent("printer",printer2);
                    printerController2.start();

                    sleep(1000);

                    for (int j = 0; j < companiesNames2.length;j++) {
                        String companyName = companiesNames2[j];
                        Integer agress = aggressiveness2[j];
                        Companies company = new Companies(companyName,1000,agress, CompaniesPos2[j]);
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

                        if(j == companiesNames2.length-1){
                            String worker3Name = companyName + "-worker-" + workerNumber++;
                            Workers worker3 = new Workers(worker2Name,companyName,company.headQuarters,500, company.headQuarters);
                            AgentController worker3Controller = this.container.acceptNewAgent(worker3Name,worker3);
                            worker3Controller.start();

                            String worker4Name = companyName + "-worker-" + workerNumber++;
                            Workers worker4 = new Workers(worker4Name,companyName,company.headQuarters,500, company.headQuarters);
                            AgentController worker4Controller = this.container.acceptNewAgent(worker4Name,worker4);
                            worker4Controller.start();
                        }
                    }

                    String atmsNames2[] = {
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

                    ArrayList<ATMs> atms2 = new ArrayList<>();

                    for (int j = 0; j < atmsNames2.length; j++) {
                        ATMs atm = new ATMs(atmsNames2[j],1000,2000,5000,ATMPos2[j]);
                        atms2.add(atm);
                        AgentController atmController = this.container.acceptNewAgent(atmsNames2[j],atm);
                        atmController.start();
                    }

                    sleep(1000);

                    Random random2 = new Random();
                    int j = 0;
                    while (j<atmsNames2.length){
                        Clients client1 = new Clients("client1."+j,random2.nextInt(250)+750,1000,atms2.get(j).position);
                        AgentController clientController = this.container.acceptNewAgent("client1."+j,client1);
                        clientController.start();

                        Clients client2 = new Clients("client2."+j,random2.nextInt(250)+750,1000,atms2.get(j).position);
                        clientController = this.container.acceptNewAgent("client2."+j,client2);
                        clientController.start();

                        j++;
                    }

                case 3:
                    String companiesNames3[] = {
                            "sibs",
                            "banco de portugal",
                            "novo banco",
                            "montepio"
                    };

                    Integer aggressiveness3[] = {
                            10,
                            10,
                            10,
                            10,
                    };

                    Position CompaniesPos3[] = {
                            new Position(2,5),
                            new Position(8,5),
                            new Position(2,13),
                            new Position(8,13),
                    };

                    Position ATMPos3[] = {
                            new Position(2,4),
                            new Position(8,4),
                            new Position(2,14),
                            new Position(8,14),
                    };

                    MapPrinter printer3 = new MapPrinter(ATMPos3,CompaniesPos3);
                    AgentController printerController3 = this.container.acceptNewAgent("printer",printer3);
                    printerController3.start();

                    sleep(1000);

                    for (int k = 0; k < companiesNames3.length;k++) {
                        String companyName = companiesNames3[k];
                        Integer agress = aggressiveness3[k];
                        Companies company = new Companies(companyName,1000,agress, CompaniesPos3[k]);
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

                        if(k == companiesNames3.length-1){
                            String worker3Name = companyName + "-worker-" + workerNumber++;
                            Workers worker3 = new Workers(worker2Name,companyName,company.headQuarters,500, company.headQuarters);
                            AgentController worker3Controller = this.container.acceptNewAgent(worker3Name,worker3);
                            worker3Controller.start();

                            String worker4Name = companyName + "-worker-" + workerNumber++;
                            Workers worker4 = new Workers(worker4Name,companyName,company.headQuarters,500, company.headQuarters);
                            AgentController worker4Controller = this.container.acceptNewAgent(worker4Name,worker4);
                            worker4Controller.start();

                            String worker5Name = companyName + "-worker-" + workerNumber++;
                            Workers worker5 = new Workers(worker5Name,companyName,company.headQuarters,500, company.headQuarters);
                            AgentController worker5Controller = this.container.acceptNewAgent(worker5Name,worker5);
                            worker5Controller.start();

                            String worker6Name = companyName + "-worker-" + workerNumber++;
                            Workers worker6 = new Workers(worker6Name,companyName,company.headQuarters,500, company.headQuarters);
                            AgentController worker6Controller = this.container.acceptNewAgent(worker6Name,worker6);
                            worker6Controller.start();
                        }
                    }

                    String atmsNames3[] = {
                            "feup",
                            "sao joao",
                            "aliados",
                            "sao bento",
                    };

                    sleep(1000);

                    ArrayList<ATMs> atms3 = new ArrayList<>();

                    for (int k = 0; k < atmsNames3.length; k++) {
                        ATMs atm = new ATMs(atmsNames3[k],1000,2000,5000,ATMPos3[k]);
                        atms3.add(atm);
                        AgentController atmController = this.container.acceptNewAgent(atmsNames3[k],atm);
                        atmController.start();
                    }

                    sleep(1000);

                    Random random3 = new Random();
                    int k = 0;
                    while (k<atmsNames3.length){
                        Clients client1 = new Clients("client1."+k,random3.nextInt(250)+750,1000,atms3.get(k).position);
                        AgentController clientController = this.container.acceptNewAgent("client1."+k,client1);
                        clientController.start();

                        Clients client2 = new Clients("client2."+k,random3.nextInt(250)+750,1000,atms3.get(k).position);
                        clientController = this.container.acceptNewAgent("client2."+k,client2);
                        clientController.start();

                        if(k == atmsNames3.length-1){
                            Clients client3 = new Clients("client3."+k,random3.nextInt(250)+750,1000,atms3.get(k).position);
                            clientController = this.container.acceptNewAgent("client3."+k,client3);
                            clientController.start();

                            Clients client4 = new Clients("client4."+k,random3.nextInt(250)+750,1000,atms3.get(k).position);
                            clientController = this.container.acceptNewAgent("client4."+k,client4);
                            clientController.start();
                        }

                        k++;
                    }

            }
        } catch (StaleProxyException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void createAgents() throws StaleProxyException, InterruptedException {


        //10,100,new Position(2,2)
        String companiesNames[] = {
                "1",
                "2",
                "3",
                "4"
        };
        Integer aggressiveness[] = {
                10,
                20,
                30,
                40,
        };

        Position ATMPos[] = {
                new Position(),
                new Position(),
                new Position(),
                new Position(),
        };
        Position CompaniesPos[] = {
                new Position(),
                new Position(),
                new Position(),
                new Position(),
        };


          MapPrinter printer = new MapPrinter(ATMPos,CompaniesPos);
          AgentController printerController = this.container.acceptNewAgent("printer",printer);
          printerController.start();

        Random random = new Random();


        for (int i = 0; i < companiesNames.length;i++) {
            String companyName = companiesNames[i];
            Integer agress = aggressiveness[i];
            Companies company = new Companies(companyName,20000,agress,CompaniesPos[i]);
            AgentController companyController = this.container.acceptNewAgent(companyName,company);
            companyController.start();

            Integer workerNumber = 0;

            String workerName = companyName + "-worker-" + workerNumber++;
            Workers worker = new Workers(workerName,companyName,new Position(),8000, company.headQuarters);
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
            ATMs atm = new ATMs(atmName,1000,2000,5000,new Position());
            atms.add(atm);
            AgentController atmController = this.container.acceptNewAgent(atmName,atm);
            atmController.start();
        }

        sleep(1000);

        Clients client1 = new Clients("client1",500,1000,atms.get(0).position);
        AgentController clientController = this.container.acceptNewAgent("client1",client1);
        clientController.start();

        Clients client2 = new Clients("client2",500,1000,atms.get(1).position);
        clientController = this.container.acceptNewAgent("client2",client2);
        clientController.start();



    }

    public static void main(String args[]){
        NationalBank nationalBank = new NationalBank(2);
    }

}
