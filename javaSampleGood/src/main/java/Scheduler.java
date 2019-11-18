import conn.Channel;
import json.Message;


public class Scheduler {
    private Channel channel;
    Action[] action;
    private Topo topo;


    public Scheduler(Channel channel,Topo topo) {
        this.channel = channel;
        int N = Main.config.mainConfig.nodeCount;
        this.topo=topo;
        action = new Action[N+1];
        // 其实不需要创建这么多对象，因为每一个action对象只有一个int target属性不同
        // 不过无伤大雅毕竟毕竟服务器牛逼
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
                    if (message.state==Const.STATE_REFUSE){
                        action[message.sysMessage.target].onRefuse(message.errCode,message.sysMessage.target);
                    }else if(message.state==Const.STATE_NOTICE){
                        action[message.sysMessage.target].onRequest(message);
                    }
                }
                break;
            case Const.CALL_TYPE_CHANNEL_DESTROY:
                topo.deleteChannel(message.channelId);
                    //通道destory说明我起码能再与对方创建一条通道
                    //如果我有需要建立的连接，我就建立，因为这时我的连接数小于我的maxChannelConn
                    //这时我可以连接通道，但是我不知道对方能不能连
                    //如果对方不能连，server发来ERR_CODE_CHANNEL_BUILD_TARGET_LIMIT,将对方放入TargetLimitNodesList
                    //TargetLimitNodesList将在main函数里进行轮询 见main第38行
                    if (!topo.nodeNeedToLinkButNotIsEmpty()){
                        int targetId=topo.getFirstNodeNeedToLinkButNot();
                        topo.removeFirstNodeNeedToLinkButNot();
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
