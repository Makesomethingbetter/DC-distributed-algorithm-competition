import json.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Topo {
    private HashMap<Integer,Integer> node2ChannelId;
    private List<Message> queue;
    private List<Integer> nodeNeedToLinkButNot;
    private int maxConnCount;


    public Topo(){
        node2ChannelId=new HashMap<>();
        queue = new ArrayList<>();
        maxConnCount=Main.config.maxChannelConn;
        nodeNeedToLinkButNot=new ArrayList<>();
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

    public void deleteChannelByNode(int nodeId){
        node2ChannelId.remove(nodeId);
    }

    public void deleteChannel(int channelId){
        for (HashMap.Entry<Integer,Integer> entry:node2ChannelId.entrySet()){
            if (entry.getValue()==channelId){
                deleteChannelByNode(entry.getKey());
                break;
            }
        }
    }



    public void addNodeAfterBuildSuc(int nodeId,int channelId){
            node2ChannelId.put(nodeId,channelId);
    }

    public boolean isLinkedWith(int nodeId){
        return node2ChannelId.containsKey(nodeId);
    }

    public int getChannelId(int nodeId){
        return node2ChannelId.get(nodeId);
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
