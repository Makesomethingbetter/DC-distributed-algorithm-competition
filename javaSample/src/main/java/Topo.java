import json.Message;

import java.util.HashMap;

public class Topo {
    private Scheduler scheduler;

    private int nodeId;
    private int nodeCount;
    private int finalSuperNodeId;
    private int groupSize;
    private int hisSuperNodeId; //superNode的此值无效，设为-1
    private boolean isSuperNode;

    private boolean  isConnectedWithSuperNode; //superNode的此值无效
    private boolean[] canReachBySuperNode ;
    private HashMap<Integer,Integer> normalChannelTemporary;

    private int[] nodeId2SuperNodeId;//normalNode的此值无效



    //构造函数计算出类里的属性
    public Topo(Scheduler scheduler){
        this.scheduler=scheduler;
        topoPropertyInitial();
    }

    private void topoPropertyInitial(){
        nodeCount=Main.config.mainConfig.nodeCount;
        nodeId=Main.config.index;
        finalSuperNodeId=7;
        groupSize=6;
        isSuperNode=false;
        hisSuperNodeId=1;

        isConnectedWithSuperNode=false;
        canReachBySuperNode=new boolean[nodeCount];
        nodeId2SuperNodeId=new int[nodeCount];
        for (int i=0;i<nodeCount;i++){
            canReachBySuperNode[i]=false;
            nodeId2SuperNodeId[i]=-1;
        }


    }

    public void topoInitialConnect(){
        if (!isSuperNode()){
            //节点向集群申请创建一条低速通道
            scheduler.sendChannelBuild(hisSuperNodeId,Const.STATE_REQUEST,Const.ERR_CODE_NONE,Const.CHANNEL_TYPE_NORMAL);
        }else {
            askConnectionWithSuperNodeAfterMe();
        }
    }

    private void askConnectionWithSuperNodeAfterMe(){
        //第n个superNode和后面的superNode申请建立连接，这样就可以实现superNode和其他superNode相连
        for (int nodeIdWillConnect=nodeId+1;nodeIdWillConnect<=finalSuperNodeId;nodeIdWillConnect++){
            scheduler.sendChannelBuild(nodeIdWillConnect,Const.STATE_REQUEST,Const.ERR_CODE_NONE,Const.CHANNEL_TYPE_FAST);
        }
    }

    public void onSys(Message message){

    }

    public boolean isSuperNode(){
        return isSuperNode;
    }

    public boolean canReachBySuperNode(int id){
        return canReachBySuperNode[id-1];
    }

    public int nodeId2SuperNodeId(int id){
        return nodeId2SuperNodeId[id-1];
    }

    public int getHisSuperNodeId(){
        return hisSuperNodeId;
    }

    public boolean hasDirectNormalChannel(int id){
        boolean reachable=false;
            if (normalChannelTemporary.containsKey(id)){
                reachable=true;
            }
        return reachable;
    }
}
