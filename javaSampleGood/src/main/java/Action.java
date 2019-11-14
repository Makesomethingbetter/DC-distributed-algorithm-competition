import json.Message;
import json.config.ChannelDetialConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Action {
    private int target;
    private Scheduler scheduler;
    private Topo topo;

    public Action(int target, Scheduler scheduler,Topo topo) {
        this.target = target;
        this.topo=topo;
        this.scheduler = scheduler;
//        waitSendAndDelete = Main.config.channelConfig.highSpeed.lag*3;

    }

    public void doRequest(int channelType) {
        scheduler.sendChannelBuild(target, Const.STATE_REQUEST, Const.ERR_CODE_NONE, channelType);
    }

    public void onRequest(Message message) {
        scheduler.sendChannelBuild(target, Const.STATE_ACCEPT, Const.ERR_CODE_NONE, message.channelType);
    }

    public void onSucc(Message message) {
        topo.addNodeAfterBuildSuc(target,message.channelId);
        filterQueueAndSendSomeMessage();
    }

    public void onRefuse(Message message) {
    }



    public void onPrepare(Message message) {
            doRequest(Const.CHANNEL_TYPE_FAST);
    }

    /**
     *
     *
     * 先把sleep设置为0或者贼小，看看能否发了就删
     *
     *
     *
     * */
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
