import json.Message;
import json.config.ChannelDetialConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Action {
    private int target;
    private double timeout;
    private int waitingCount;
    private List<Message> queue;
    private Scheduler scheduler;
    private Topo topo;

    public Action(int target, Scheduler scheduler,Topo topo) {
        this.target = target;
        this.scheduler = scheduler;
        this.topo=topo;
        timeout = 0;
        waitingCount = 0;
        queue = new ArrayList<>();

    }




    public void doRequest(int channelType) {
//        if (this.channelType != Const.CHANNEL_TYPE_ERROR) return ;
//        this.channelType = channelType;
//        channelState = Const.CHANNEL_STATE_REQUEST;
        scheduler.sendChannelBuild(target, Const.STATE_REQUEST, Const.ERR_CODE_NONE, channelType);
    }

    public void onRequest(Message message) {
        int target = message.sysMessage.target;
        //判断是否拒绝请求 一般不拒绝
        //if ()
        scheduler.sendChannelBuild(target, Const.STATE_ACCEPT, Const.ERR_CODE_NONE, message.channelType);
//        channelType = message.channelType;
//        channelState = Const.CHANNEL_STATE_ACCEPT;
    }

    //收到了通道建立成功的消息
    public void onSucc(Message message) {

        dealWithQueueMessage(message);
    }

    private void tell

    private void dealWithQueueMessage(Message message){
        filterQueue();
        queue.forEach(msg -> dealWithQueueMessage0(msg));

    }

    private void dealWithQueueMessage0(Message message){
        if (topo.hasDirectNormalChannel(target)){
            doSendToTagertDirectly(message);
            queue.remove(message);
        }
    }

    public void onRefuse(Message message) {
//        if (channelState != Const.CHANNEL_STATE_SUCCESS) {
//            System.out.println("on refuse");
//            int next = getOtherType();
//            clearChannelInfo();
//            filterQueue();
//            if (waitingCount > 0 || queue.size() > 0) {
//                doRequest(next);
//            }
//        }
        //建立被拒绝怎么办
    }

    public void onDestroy(Message message) {
        if (channelState == Const.CHANNEL_STATE_SUCCESS) {
            System.out.println("on destroy");
            clearChannelInfo();
        }
        filterQueue();
        if (waitingCount > 0 || queue.size() > 0) {
            doRequest(getOtherType());
        }
    }

    public void onPrepare(Message message) {
        //老大让我准备发消息
        //我设置好几点到期
//        timeout = Math.max(timeout, Main.curTime() + Main.config.mainConfig.timeOut);
//        waitingCount++;
//        //如果这条channel没法
//        if (channelState == Const.CHANNEL_STATE_NONE) {
//            doRequest(getOtherType());
//        }

        if (topo.canReachBySuperNode(target)){

        }else {
            //方法1：建立直接慢速通道
            //方法2：建立直接快速通道
            //方法3：等superNode建立好
            //我们使用方法1
            doRequest(Const.CHANNEL_TYPE_NORMAL);
        }
        waitingCount++;

    }

    public void onSend(Message message) {
        if (scheduler.getId() == message.sysMessage.target) {
            System.out.println("succ received message: " + message.sysMessage.data);
            return ;
        }

        waitingCount--;
        if (topo.canReachBySuperNode(target)) {
            System.out.println("send directory");
            doSendBySuperNode(message);
            return;
        }else if (topo.hasDirectNormalChannel(target)){
            doSendToTagertDirectly(message);
            return;
        }
        else {
            System.out.println("add into cache");
            queue.add(message);
        }
    }

    public void doSendToTagertDirectly(Message message){
        scheduler.doSend(message, target);
    }

    public void doSendBySuperNode(Message message) {
//        if (message.recvTime + Main.config.mainConfig.timeOut >= Main.curTime() + getConfig().lag) {
//
//        }
        if (!topo.isSuperNode()){
            scheduler.doSend(message, topo.getHisSuperNodeId());
        } else {
            int hisSuperNodeId=topo.nodeId2SuperNodeId(target);
            //如果目标的superNodeId就是自己，直接把消息发给它
            if (hisSuperNodeId==scheduler.getId()){
                doSendToTagertDirectly(message);
            }else{
                scheduler.doSend(message,hisSuperNodeId);
            }
        }
    }

    //如果发送时间不够就不要这个消息了
    public void filterQueue() {
        ArrayList<Message> filtered = new ArrayList<>();
        for (Message message : queue) {
            ChannelDetialConfig selfConf = getConfig();
            float lag = selfConf == null ? 0 : selfConf.lag;
            if (message.recvTime + Main.config.mainConfig.timeOut >= Main.curTime() + lag) {
                filtered.add(message);
            }
        }
        queue = filtered;
    }

    public ChannelDetialConfig getConfig() {
        switch (channelType) {
            case Const.CHANNEL_TYPE_FAST :
                return Main.config.channelConfig.highSpeed;
            case Const.CHANNEL_TYPE_NORMAL:
                return Main.config.channelConfig.normalSpeed;
            default:
                return null;
        }
    }
}
