import json.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Topo {
    //<channelId,nodeId>
    private HashMap<Integer,Integer> node2ChannelId;
    private List<Message> queue;
    private List<Integer> nodeNeedToLinkButNot;
    private int maxConnCount;
    private List<Integer> targetLimitNodesList;

    public List<Integer> getTargetLimitNodesList() {
        return targetLimitNodesList;
    }

    public Topo(){
        node2ChannelId=new HashMap<>();
        queue = new ArrayList<>();
        maxConnCount=Main.config.maxChannelConn;
        nodeNeedToLinkButNot=new ArrayList<>();
        targetLimitNodesList=new ArrayList<>();
    }

    public void addTotargetLimitNodesList(int nodeId){
        targetLimitNodesList.add(nodeId);
    }

    public void removeFromTargetLimitNodesList(int nodeId){
        targetLimitNodesList.remove(new Integer(nodeId));
    }

    public int getTargetLimitNodesListSize(){
        return targetLimitNodesList.size();
    }


    public void removeFirstNodeNeedToLinkButNot(){
        nodeNeedToLinkButNot.remove(0);
    }

    public int getFirstNodeNeedToLinkButNot(){
        return nodeNeedToLinkButNot.get(0);
    }

    public boolean nodeNeedToLinkButNotIsEmpty(){
        return nodeNeedToLinkButNot.size()==0;
    }

    public boolean canLinkAnyMore(){
        return node2ChannelId.size()<maxConnCount;
    }

    public void addToNodeNeedToLinkButNot(int nodeId){
        nodeNeedToLinkButNot.add(nodeId);
    }


    public void deleteChannel(int channelId){
        node2ChannelId.remove(channelId);
    }

    public void addNodeAfterBuildSuc(int nodeId, int channelId){
            node2ChannelId.put(channelId,nodeId);
    }

    public boolean isLinkedWith(int nodeId){
        for (HashMap.Entry<Integer,Integer> entry:node2ChannelId.entrySet()){
            if (entry.getValue()==nodeId){
                return true;
            }
        }
        return false;
    }

    public int getChannelId(int nodeId){
        for (HashMap.Entry<Integer,Integer> entry:node2ChannelId.entrySet()){
            if (entry.getValue()==nodeId){
                return entry.getKey();
            }
        }
        return 0;
    }

    public void addToQueue(Message message){
        queue.add(message);
    }

    public List<Message> getQueue() {
        return queue;
    }

    public void setQueue(List<Message> queue) {
        this.queue = queue;
    }
}
