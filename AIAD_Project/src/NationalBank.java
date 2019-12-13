import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import javafx.concurrent.Worker;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import static java.lang.Thread.sleep;

public class NationalBank {

    public AgentContainer container;
    public ProfileImpl profile;
    public Runtime jade ;
    public ArrayList<Companies> companiesLogs = new ArrayList<>();
    public ArrayList<Clients> clientsLogs = new ArrayList<>();
    public ArrayList<Workers> workersLogs = new ArrayList<>();
    public ArrayList<ATMs> atmsLogs = new ArrayList<>();
    public ArrayList<MapPrinter> mapsLogs = new ArrayList<>();
    public ArrayList<AgentController> agentControllers = new ArrayList<>();
    public Boolean reset = true;

    public static void writeToFile(String str) throws IOException {
        File file = new File ("./data.csv");
        FileWriter writer;

        if (file.exists()){
            writer = new FileWriter(file.getPath(),true);
        }
        else{
            file.createNewFile();
            writer = new FileWriter(file);
        }

        PrintWriter printWriter = new PrintWriter(writer);

        writer.append(str);
        printWriter.append('\n');

        printWriter.close();
    }

    public void runNationalBank() throws InterruptedException, StaleProxyException {

        while(true){

            if(reset){
                System.out.println("entrei");
                for(int i = 0; i < companiesLogs.size(); i++){
                    companiesLogs.get(i).doDelete();
                }
                for(int i = 0; i < workersLogs.size(); i++){
                    workersLogs.get(i).doDelete();
                }
                for(int i = 0; i < atmsLogs.size(); i++){
                    atmsLogs.get(i).doDelete();
                }
                for(int i = 0; i < clientsLogs.size(); i++){
                    clientsLogs.get(i).doDelete();
                }
                for(int i = 0; i < mapsLogs.size(); i++){
                    mapsLogs.get(i).doDelete();
                }
                if(container != null) container.kill();

                reset = false;

                this.jade = Runtime.instance();

                this.profile = new ProfileImpl(true);

                this.container = jade.createMainContainer(this.profile);

                try {
                    String companiesNames[] = {
                            "sibs",
                            "banco de portugal",
                            "novo banco",
                            "montepio"
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
                    mapsLogs.add(printer);

                    AgentController printerController = this.container.acceptNewAgent("printer",printer);
                    printerController.start();
                    agentControllers.add(printerController);

                    sleep(1000);

                    for (int i = 0; i < companiesNames.length;i++) {

                        String log = "";
                        Random random = new Random();

                        String companyName = companiesNames[i];
                        log += companyName + ", " +  "false, ";

                        Integer money = 100 + random.nextInt(20000);
                        log += money.toString() + ", ";

                        Integer agress = random.nextInt(50) + 1;
                        log += agress.toString() + ", ";

                        Companies company = new Companies(companyName, money, agress, CompaniesPos[i]);
                        companiesLogs.add(company);

                        AgentController companyController = this.container.acceptNewAgent(companyName,company);
                        agentControllers.add(companyController);
                        companyController.start();

                        Integer workerNumber = 0;

                        for(int j = 0; j < 1 + random.nextInt(10); j++){
                            String workerName = companyName + "-worker-" + workerNumber++;
                            Workers worker = new Workers(workerName,companyName,company.headQuarters,500, company.headQuarters);
                            workersLogs.add(worker);

                            AgentController workerController = this.container.acceptNewAgent(workerName,worker);
                            agentControllers.add(workerController);
                            workerController.start();
                        }

                        //Number of clients hardcoded for 2, maybe change later or not
                        log += workerNumber.toString() + ", " + "2";

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

                    for (int i = 0; i < atmsNames.length; i++) {
                        ATMs atm = new ATMs(atmsNames[i],1000,2000,5000,ATMPos[i]);
                        atmsLogs.add(atm);

                        AgentController atmController = this.container.acceptNewAgent(atmsNames[i],atm);
                        agentControllers.add(atmController);
                        atmController.start();
                    }

                    sleep(1000);

                    Random random = new Random();
                    int i = 0;
                    while (i < atmsNames.length){
                        Clients client1 = new Clients("client1."+i,random.nextInt(250)+750,1000,atmsLogs.get(i).position);
                        clientsLogs.add(client1);

                        AgentController clientController = this.container.acceptNewAgent("client1."+i, client1);
                        agentControllers.add(clientController);
                        clientController.start();

                        Clients client2 = new Clients("client2."+i,random.nextInt(250)+750,1000,atmsLogs.get(i).position);
                        clientsLogs.add(client2);

                        clientController = this.container.acceptNewAgent("client2."+i, client2);
                        clientController.start();

                        i++;
                    }

                    Thread check = new Thread(new checkBankrupcy());
                    check.start();

                } catch (StaleProxyException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            sleep(100);
        }
    }

    public NationalBank(int scenario) throws IOException, InterruptedException, StaleProxyException {
        runNationalBank();
    }

    public class checkBankrupcy implements Runnable{

        @Override
        public void run() {
            Boolean exit = false;
            while(!exit){
                Integer count = 0, lastCompany = null;
                for(int i = 0; i < companiesLogs.size(); i++){
                    if(!companiesLogs.get(i).bankrupt){
                        lastCompany = i;
                    }else{
                        count++;
                    }
                }
                if(count == 3){
                    String str = "";
                    for(int i = 0; i < companiesLogs.size(); i++){
                        str += companiesLogs.get(i).getLocalName() + ", "
                                + companiesLogs.get(i).bankrupt.toString() + ", "
                                + companiesLogs.get(i).money.toString() + ", "
                                + companiesLogs.get(i).aggressiveness.toString() + ", "
                                + companiesLogs.get(i).workers.size()
                                + ", 2";

                        if(i != 3) str += "\n";
                    }

                    try {
                        writeToFile(str);
                        reset = true;
                        exit = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void main(String args[]) throws IOException, StaleProxyException, InterruptedException {
        writeToFile("");
        NationalBank nationalBank = new NationalBank(1);
    }

}
