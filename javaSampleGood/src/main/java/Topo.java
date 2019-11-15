import json.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Topo {
    private int connCountToDoNum;
    //<channelId,nodeId>
    private HashMap<Integer,Integer> node2ChannelId;
    private List<Message> queue;
    private List<Integer> nodeNeedToLinkButNot;
    private int maxConnCount;
    private int sendNum;
    private int needSendNum;


    public Topo(){
        node2ChannelId=new HashMap<>();
        queue = new ArrayList<>();
        maxConnCount=Main.config.maxChannelConn;
        nodeNeedToLinkButNot=new ArrayList<>();
        sendNum=0;
        needSendNum=0;
        connCountToDoNum=0;
    }
    //什么时候++：我要和别人建立 对应的--：我被拒绝了或者通道被摧毁了
    //什么时候++；对面要和我连我同意了 对应的--：通道被摧毁了
    public void connCountToDoNumAddOne(){
        connCountToDoNum++;
    }

    public void connCountToDoNumSubOne(){
        connCountToDoNum--;
    }

    public void sendNumAddOne(){
        sendNum++;
    }

    public void needSendNumAddOne(){
        needSendNum++;
    }

    public int getSendNum(){
        return sendNum;
    }

    public int getNeedSendNum(){
        return needSendNum;
    }

    //////////////

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
        System.out.println("connCountToDoNum,maxConnCount："+connCountToDoNum+"!!"+maxConnCount);
        return connCountToDoNum<maxConnCount;
    }

    public void addToNodeNeedToLinkButNot(int nodeId){
        nodeNeedToLinkButNot.add(nodeId);
    }

    public void deleteOneChannelByNode(int nodeId){
        for (HashMap.Entry<Integer,Integer> entry:node2ChannelId.entrySet()){
            if (entry.getValue()==nodeId){
                deleteChannel(entry.getKey());
                break;
            }
        }
    }

    public void deleteChannel(int channelId){
        node2ChannelId.remove(channelId);
    }


    public int getMaxConnCount() {
        return maxConnCount;
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
        System.out.println("getChannelId in topo error return 0~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        return 0;
    }

    public void addToQueue(Message message){
        queue.add(message);
    }

    public List<Message> getQueue() {
        return queue;
    }



    public int getChannelCount(){
        return node2ChannelId.size();
    }

    public void setQueue(List<Message> queue) {
        this.queue = queue;
    }
}
