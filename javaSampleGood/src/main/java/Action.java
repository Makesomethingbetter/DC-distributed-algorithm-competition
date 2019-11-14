import json.Message;
import java.util.ArrayList;

public class Action {
    private int target;
    private Scheduler scheduler;
    private Topo topo;

    public Action(int target, Scheduler scheduler,Topo topo) {
        this.target = target;
        this.topo=topo;
        this.scheduler = scheduler;

    }

    public void doRequest(int channelType) {
        scheduler.sendChannelBuild(target, Const.STATE_REQUEST, Const.ERR_CODE_NONE, channelType);
    }

    public void onRequest(Message message) {
        //如果我还可以连，我就连
        if (topo.canLinkAnyMore()){
            scheduler.sendChannelBuild(target, Const.STATE_ACCEPT, Const.ERR_CODE_NONE, message.channelType);
        }else {
            scheduler.sendChannelBuild(target,Const.STATE_REFUSE,Const.ERR_CODE_NONE,message.channelType);
            topo.addToNodeNeedToLinkButNot(target);
        }
    }

    public void onSucc(Message message) {
        topo.addNodeAfterBuildSuc(target,message.channelId);
        filterQueueAndSendSomeMessage();
    }

    public void onRefuse() {
        System.out.println("我要建立连接被拒绝啦");
    }



    public void onPrepare() {
        if (topo.canLinkAnyMore()){
            doRequest(Const.CHANNEL_TYPE_FAST);
        }else {
            topo.addToNodeNeedToLinkButNot(target);
        }
    }

    public void onSend(Message message) {
        if (scheduler.getId() == message.sysMessage.target) {
            scheduler.sendChannelDestory(message.channelId);
            return ;
        }
        if (topo.isLinkedWith(target)) {
            doSend(message);
            return;
        } else {
            topo.addToQueue(message);
        }
    }

    public void doSend(Message message) {
            message.channelId = topo.getChannelId(target);
            scheduler.doSend(message, message.sysMessage.target);
    }

    public void filterQueueAndSendSomeMessage() {
        if (topo.getQueue().size()==0){
            return;
        }
        ArrayList<Message> filtered = new ArrayList<>();
        ArrayList<Integer> nodeId2ChannelArray=new ArrayList<>();
        for (Message message : topo.getQueue()) {
            if (topo.isLinkedWith(message.sysMessage.target)){
                doSend(message);
                nodeId2ChannelArray.add(message.sysMessage.target);
            }else {
                filtered.add(message);
            }
        }

        topo.setQueue(filtered);
    }

}
