import conn.Channel;
import json.Message;


public class Scheduler {
    private Channel channel;
    Action[] action;
    private Topo topo;

    /**
     * 一开始每个节点都知道有多少个点 能有多少条高速、普通通道、通道的时间、通道的message数
     * 通过这些每个节点用相同的算法算出哪些做super node，得到相同的topo.java 并开始建立通道
     * 初始化的拓扑结构是通过第一行的参数来影响的。
     *
     * 动态：1参数影响初始化的结构
     * 2super node的messageCount可能不足，需要建立更多的super node之间的通道
     *
     * node1->server type:sys taget2
     *
     * */

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
         if (message.errCode != Const.ERR_CODE_NONE) {
             return ;
         }

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
                if (message.state==Const.STATE_REFUSE){
                    topo.connCountToDoNumSubOne();
                    System.out.println("在拒绝onrefuce!!!!!!");
                }
                if (message.channelId != 0) {
                    action[message.sysMessage.target].onSucc(message);
                } else {
                    switch (message.state) {
                        case Const.STATE_NOTICE:
                            action[message.sysMessage.target].onRequest(message);
                            break;
                        case Const.STATE_REFUSE:
                            System.out.println("在拒绝onrefuce!!!!!!");
                            action[message.sysMessage.target].onRefuse();
                            break;
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
                        sendChannelBuild(targetId, Const.STATE_REQUEST, Const.ERR_CODE_NONE, Const.CHANNEL_TYPE_FAST);
                        topo.removeFirstNodeNeedToLinkButNot();
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
