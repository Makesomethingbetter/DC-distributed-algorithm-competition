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

    /**
     * 通道建立成功
     * 1 临时的低速通道建立成功：
     * 1.1如果是直接连接的低速通道
     * 如果此时高速通道可达，（高速通道是否可达是在onSys中解决的）
     * 直接通过低速通道发送信息，如果在建立低速通道时可以通过superNode到达了，队列中就没有这个消息了
     * 1.2如果是和superNode的高速通道，不发送
     * 2 如果高速通道
     *
     * */
    public void onSucc(Message message) {
        //如果是低速通道
        if (message.channelType==Const.CHANNEL_TYPE_NORMAL){
            //如果我是superNode
            if (topo.isSuperNode()){
                //如果建立成功的低速通道是和我负责的nomalNode
                if (topo.isGroupMember(target)){
                    topo.setNodeId2SuperNodeId(target);//更改我的路由表
                    //告诉它我的路由表 未写
                    dealWithQueueMessageInGroupChannel(message);//处理应该发给它的信息
                }else {
                    topo.setDirectNormalChannelExits(target,message.channelId);
                    dealWithQueueMessageInChannelTemporary(message);
                }
            }else{
                //如果连接的不是自己的superNode
                if (topo.getHisSuperNodeId()!=target){
                    topo.setDirectNormalChannelExits(target,message.channelId);
                    dealWithQueueMessageInChannelTemporary(message);
                }
                //如果连接的是自己的superNode
                else{
                    topo.setConnectedWithSuperNode(true);
                    dealWithQueueMessageToMySuperNode(message);
                }
            }
        }
        //如果是高速通道建立成功 说明这是superNode之间的通道
        //
        else{
            //处理应该发给它的消息
            //告诉它我的路由表
        }
    }

    private void dealWithQueueMessageToMySuperNode(Message message){
        filterQueue();
        queue.forEach(msg -> dealWithQueueMessageToMySuperNode0(msg));
    }

    private void dealWithQueueMessageToMySuperNode0(Message message){
        if (target==topo.getHisSuperNodeId()){
            doSendToTagertDirectly(message);
            queue.remove(message);
        }
    }

    private void dealWithQueueMessageInGroupChannel(Message message){
        filterQueue();
        queue.forEach(msg -> dealWithQueueMessageInGroupChannel0(msg));
    }

    private void dealWithQueueMessageInGroupChannel0(Message message){
        if (topo.nodeId2SuperNodeId(target)==scheduler.getId()){
            doSendToTagertDirectly(message);
            queue.remove(message);
        }
    }

    private void dealWithQueueMessageInChannelTemporary(Message message){
        filterQueue();
        queue.forEach(msg -> dealWithQueueMessageInChannelTemporary0(msg));

    }

    private void dealWithQueueMessageInChannelTemporary0(Message message){
        if (topo.hasDirectNormalChannelTemporary(target)){
            doSendToTagertDirectly(message);
            queue.remove(message);
        }
    }

    /**
     *
     *
     *
     *
     * */
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

    /**
     *
     * onPrepare
     *
     *
     * */
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
    /**
     *
     * onSend
     *
     * */

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
        }else if (topo.hasDirectNormalChannelTemporary(target)){
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

    /**
     *
     * 其他功能函数
     *
     * */
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
