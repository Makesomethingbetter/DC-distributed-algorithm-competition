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
        topo.connCountToDoNumAddOne();
    }

    public void onRequest(Message message) {
        //如果我还可以连，我就连
        if (topo.canLinkAnyMore()){
            scheduler.sendChannelBuild(target, Const.STATE_ACCEPT, Const.ERR_CODE_NONE, message.channelType);
            topo.connCountToDoNumAddOne();
        }else {
            System.out.println("我拒绝别人啦");
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
            System.out.println("此时目标："+target+"，目前连接数"+topo.getChannelCount()+"最大连接数："+topo.getMaxConnCount());
            topo.addToNodeNeedToLinkButNot(target);
        }
    }

    public void onSend(Message message) {
        if (scheduler.getId() == message.sysMessage.target) {
            scheduler.sendChannelDestory(message.channelId);
            return ;
        }
        topo.needSendNumAddOne();
        if (topo.isLinkedWith(target)) {
            doSend(message);
            return;
        } else {
            topo.addToQueue(message);
        }
    }

    public void doSend(Message message) {
            System.out.println("进入dosend 发送数目为:"+topo.getSendNum());
            message.channelId = topo.getChannelId(target);
            scheduler.doSend(message, message.sysMessage.target);
            topo.sendNumAddOne();
            System.out.println("发送数目为:"+topo.getSendNum());
            System.out.println("需要发送的数目为:"+topo.getNeedSendNum());
            System.out.println("queueSize为："+topo.getQueue().size());
            if (topo.getQueue().size()>0){
                System.out.println(topo.getQueue().get(0).sysMessage.target);
            }
    }

    public void filterQueueAndSendSomeMessage() {
        if (topo.getQueue().size()==0){
            return;
        }
        System.out.println("进入filter且此时size大于0");
        ArrayList<Message> filtered = new ArrayList<>();
        for (Message message : topo.getQueue()) {
            if (topo.isLinkedWith(message.sysMessage.target)){
                System.out.println("准备调用dosend 因为有直接相连的");
                doSend(message);
            }else {
                filtered.add(message);
            }
        }

        topo.setQueue(filtered);
    }

}
