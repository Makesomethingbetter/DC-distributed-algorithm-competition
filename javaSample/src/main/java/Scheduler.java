import conn.Channel;
import json.Message;

import java.util.HashMap;
import java.util.Map;

public class Scheduler {
    private Channel channel;
    private Topo topo;
    Action[] action;
    Map<Integer, Integer> channelMap;
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
        //节点个数
        int N = Main.config.mainConfig.nodeCount;
        topo=new Topo(this);
        topo.topoInitialConnect();
        //它认为每个节点间只有一条channel，action[i]就是和target i之间的连接，如果action的channelState为CHANNEL_STATE_NONE那么就没有直接连接
        //我们的action是用来处理
        action = new Action[N+1];
        for (int i=1; i<=N; i++) {
            action[i] = new Action(i, this, topo);
        }
        //<channelID,tagertID>
        channelMap = new HashMap<>();
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
                action[message.sysMessage.target].onPrepare(message);
                break;
            case Const.CALL_TYPE_SEND:
                action[message.sysMessage.target].onSend(message);
                break;
            case Const.CALL_TYPE_SYS:
                topo.onSys(message);
                break;
            case Const.CALL_TYPE_CHANNEL_BUILD:
                //如果message.channelId!=则说明建立通道成功了
                if (message.channelId != 0) {
                    /*
                    * 如果是superNode:
                    * 1若建立的是和自己组的nomalNode：告诉自己组的nomalNode，告诉相连的superNode
                    * 2若建立的是不是自己组的nomalNode（发生在初始的时候）：不告诉所有人，处理消息队列
                    * 3若建立的是和superNode：告诉它自己组连上的nomalNode
                    *
                    * */
                    action[message.sysMessage.target].onSucc(message);
                } else {
                    switch (message.state) {
                        case Const.STATE_NOTICE:
                            action[message.sysMessage.target].onRequest(message);
                            break;
                        case Const.STATE_REFUSE:
                            action[message.sysMessage.target].onRefuse(message);
                            break;
                    }
                }
                break;
            case Const.CALL_TYPE_CHANNEL_DESTROY:
                int target = channelMap.getOrDefault(message.channelId, 0);
                if (target!=0) {
                    action[target].onDestroy(message);
                    channelMap.remove(message.channelId);
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
}
