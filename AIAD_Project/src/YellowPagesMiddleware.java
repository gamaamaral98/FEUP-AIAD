import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class YellowPagesMiddleware {

    private final String name;
    private final String type;
    private  Agent agent;

    public YellowPagesMiddleware(Agent agent, String type, String name){
        this.agent = (Agent) agent;
        this.type = type;
        this.name=name;

    }

    public void register(){
        //Register agent to yellow pages
        DFAgentDescription agentDescription = new DFAgentDescription();
        agentDescription.setName(this.agent.getAID());
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(this.type);
        serviceDescription.setName(this.name);
        agentDescription.addServices(serviceDescription);

        try {
            DFService.register(this.agent,agentDescription);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    public void deregister(){
        //Deregister agent from the yellow pages
        try {
            DFService.deregister(this.agent);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    public AID[] getAgentList(String typeToSearch){
        AID[] agentsAIDList = new AID[0];

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(typeToSearch);
        template.addServices(sd);
        try {
            DFAgentDescription[] agentsList = DFService.search(this.agent, template);
            agentsAIDList = new AID[agentsList.length];
            for (int i = 0; i < agentsList.length; ++i) {
                agentsAIDList[i] = agentsList[i].getName();
            }
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        return agentsAIDList;
    }
}
