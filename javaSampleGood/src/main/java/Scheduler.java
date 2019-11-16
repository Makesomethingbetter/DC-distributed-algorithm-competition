import conn.Channel;
import json.Message;


public class Scheduler {
    private Channel channel;
    Action[] action;
    private Topo topo;


    public Scheduler(Channel channel) {
        this.channel = channel;
        int N = Main.config.mainConfig.nodeCount;
        topo=new Topo();
        action = new Action[N+1];
        for (int i=1; i<=N; i++) {
            action[i] = new Action(i, this,topo);
        }
    }

    public int getId() {
        return channel.getId();
    }

    public void onRecv(Message message) {

        switch (message.callType) {
            case Const.CALL_TYPE_PREPARE:
                action[message.sysMessage.target].onPrepare();
                break;
            case Const.CALL_TYPE_SEND:
                action[message.sysMessage.target].onSend(message);
                break;
            case Const.CALL_TYPE_SYS:
                break;
            case Const.CALL_TYPE_CHANNEL_BUILD:
                if (message.channelId != 0) {
                    action[message.sysMessage.target].onSucc(message);
                } else {
                    if (message.state==2){
                        action[message.sysMessage.target].onRefuse(message.errCode);
                    }else if(message.state==Const.STATE_NOTICE){
                        action[message.sysMessage.target].onRequest(message);
                    }
                }
                break;
            case Const.CALL_TYPE_CHANNEL_DESTROY:
                    topo.deleteChannel(message.channelId);
                    topo.connCountToDoNumSubOne();
                    //如果我还又需要建立的连接，我就建立，因为这时我的连接数小于我的maxChannelConn
                    //这时我可以连接通道，但是我不知道对方能不能连
                    //如果对方不能连，对方发送拒绝信息（其实也可以不发），且对方记录下来我们之间需要channel
                    //在对方能连的时候再向我发建立连接
                    if (!topo.nodeNeedToLinkButNotIsEmpty()){
                        int targetId=topo.getFirstNodeNeedToLinkButNot();
                        topo.removeFirstNodeNeedToLinkButNot();
                        topo.connCountToDoNumAddOne();//新加的一行
                        sendChannelBuild(targetId, Const.STATE_REQUEST, Const.ERR_CODE_NONE, Const.CHANNEL_TYPE_FAST);

                    }
                break;
        }
    }

    //向server发message
    public void sendChannelBuild(int target, int state, int errCode, int channelType) {
        Message message = Const.GetEmptyMessage();
        message.callType = Const.CALL_TYPE_CHANNEL_BUILD;
        message.state = state;
        message.sysMessage.target = target;
        message.errCode = errCode;
        message.channelType = channelType;
        doSend(message, 0);
    }

    public void doSend(Message message, int target) {
        try {
            channel.send(message, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendChannelDestory(int channelId){
        Message message=Const.GetEmptyMessage();
        message.callType=Const.CALL_TYPE_CHANNEL_DESTROY;
        message.channelId=channelId;
        message.state=Const.STATE_REQUEST;
        message.errCode=Const.ERR_CODE_NONE;
        doSend(message,0);
    }
}
