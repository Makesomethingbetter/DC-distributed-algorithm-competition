import json.Message;
import json.config.ChannelDetialConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Action {
    private Topo topo;
    private int target;
    private Scheduler scheduler;
    public Action(int target, Scheduler scheduler) {
        this.topo=Main.topo;
        this.target = target;
        this.scheduler = scheduler;
    }


    public void onSYS(Message message){
        topo.dealWithDataInSYSMessage(message.sysMessage.data,target);
    }


    public void doRequest(int channelType) {
        scheduler.sendChannelBuild(target, Const.STATE_REQUEST, Const.ERR_CODE_NONE, channelType);
//        if (this.channelType != Const.CHANNEL_TYPE_ERROR) return ;
//        this.channelType = channelType;
//        channelState = Const.CHANNEL_STATE_REQUEST;
//        scheduler.sendChannelBuild(target, Const.STATE_REQUEST, Const.ERR_CODE_NONE, channelType);
    }

    /**
     *
     * 如果我还能连我就同意
     * 连接申请方判断此次连接的价值，所以我只管通过我能不能连接受或拒绝
     *
     * */
    public void onRequest(Message message) {
        int target = message.sysMessage.target;
        if (topo.canLinkAnyMore()){
            scheduler.sendChannelBuild(target, Const.STATE_ACCEPT, Const.ERR_CODE_NONE, message.channelType);
        }else{
            scheduler.sendChannelBuild(target, Const.STATE_REFUSE,
                        Const.ERR_CODE_CHANNEL_BUILD_TARGET_LIMIT, message.channelType);
        }
//        int target = message.sysMessage.target;
//        if (channelState != Const.CHANNEL_STATE_NONE) {
//            if (scheduler.getId() < target) {
//                scheduler.sendChannelBuild(target, Const.STATE_REFUSE,
//                        Const.ERR_CODE_CHANNEL_BUILD_TARGET_REFUSE, message.channelType);
//                return ;
//            }
//        }
//        scheduler.sendChannelBuild(target, Const.STATE_ACCEPT, Const.ERR_CODE_NONE, message.channelType);
//        channelType = message.channelType;
//        channelState = Const.CHANNEL_STATE_ACCEPT;
    }



    /**
     *
     * 先把点记录下来
     * 如果不是圆上的通道 那么我就向邻居发路由表
     *
     *
     * */
    public void onSucc(Message message) {
        int channelType=message.channelType;
        int taretId=message.sysMessage.target;
        topo.addNodeAfterBuildSuc(taretId,channelType);
        if (!(taretId== scheduler.getId()-1
                ||taretId==scheduler.getId()+1
                ||(scheduler.getId()==topo.getNodeCount()&&taretId==1)
                ||(scheduler.getId()==1&&taretId==topo.getNodeCount()))){
            topo.sendSysRouteToNodesDirectlyLinked();
        }
//        channelType = message.channelType;
//        channelState = Const.CHANNEL_STATE_SUCCESS;
//        channelId = message.channelId;
//        filterQueue();
//        queue.forEach(msg -> doSend(msg));
//        queue.clear();
    }



    /**
     *
     * 通过错误信息判断应该怎么办
     * JSTOR
     *
     * */
    public void onRefuse(Message message) {
        if (topo.canLinkAnyMore()){
            //对方拒绝了我 向另一个人建立
            if (message.errCode==Const.ERR_CODE_CHANNEL_BUILD_TARGET_REFUSE){
                topo.getNodeLongestTimeIgnoreOneNode(message.sysMessage.target);
            }
            //如果对方channel不够用了 向另一个人发
            else if (message.errCode==Const.ERR_CODE_CHANNEL_BUILD_TARGET_LIMIT){

            }
            //如果对方没响应 向另一个发
            else if (message.errCode==Const.ERR_CODE_CHANNEL_BUILD_TARGET_TIMEOUT){
                System.out.println("对方没响应你的建立请求");
            }
            else {
                System.out.println("没有处理的错误"+message.errCode);
            }
        }

//        if (channelState != Const.CHANNEL_STATE_SUCCESS) {
//            System.out.println("on refuse");
//            int next = getOtherType();
//            clearChannelInfo();
//            filterQueue();
//            if (waitingCount > 0 || queue.size() > 0) {
//                doRequest(next);
//            }
//        }
    }



    /**
     *
     * 目前我们不destroy通道
     *
     * */
    public void onDestroy(Message message) {
//        if (channelState == Const.CHANNEL_STATE_SUCCESS) {
//            System.out.println("on destroy");
//            clearChannelInfo();
//        }
//        filterQueue();
//        if (waitingCount > 0 || queue.size() > 0) {
//            doRequest(getOtherType());
//        }
    }


    /**
     *
     * 先看我们之间通信的时间
     * 如果时间短，就不管等send
     * 如果时间长，尝试建立通道
     *
     * */
    public void onPrepare(Message message) {
        float timeNeed = topo.indexToTimeNeed(target);
        float buildTime=(Main.config.channelConfig.highSpeed.buildTime+Main.config.channelConfig.normalSpeed.buildTime)/2;
        float lagTime=(Main.config.channelConfig.highSpeed.lag+Main.config.channelConfig.normalSpeed.lag);
        float longTime=buildTime+lagTime;
        //如果时间长 尝试建立通道
        if (timeNeed>longTime){
            if (topo.canLinkAnyMore()){
                doRequest(Const.CHANNEL_TYPE_NORMAL);
            }
        }
//        //老大让我准备发消息
//        //我设置好几点到期
//        timeout = Math.max(timeout, Main.curTime() + Main.config.mainConfig.timeOut);
//        waitingCount++;
//        //如果这条channel没法
//        if (channelState == Const.CHANNEL_STATE_NONE) {
//            doRequest(getOtherType());
//        }
    }

    /**
     *
     * server让我发送
     * 通过nodesChangedByWho发送
     *
     * */

    public void onSend(Message message) {
        if (scheduler.getId() == message.sysMessage.target) {
            System.out.println("succ received message: " + message.sysMessage.data);
            return;
        }
        int nextHop=topo.nodeId2NextHop(target);
        scheduler.doSend(message,target);
        //要不要判断自己的timeArray 我这里不判断是因为 别人的路由信息可能还没传过来 所以先传给两边
    }


//    //如果发送时间不够就不要这个消息了
//    public void filterQueue() {
//        ArrayList<Message> filtered = new ArrayList<>();
//        for (Message message : queue) {
//            ChannelDetialConfig selfConf = getConfig();
//            float lag = selfConf == null ? 0 : selfConf.lag;
//            if (message.recvTime + Main.config.mainConfig.timeOut >= Main.curTime() + lag) {
//                filtered.add(message);
//            }
//        }
//        queue = filtered;
//    }
//
//    public ChannelDetialConfig getConfig() {
//        switch (channelType) {
//            case Const.CHANNEL_TYPE_FAST :
//                return Main.config.channelConfig.highSpeed;
//            case Const.CHANNEL_TYPE_NORMAL:
//                return Main.config.channelConfig.normalSpeed;
//            default:
//                return null;
//        }
//    }
}
