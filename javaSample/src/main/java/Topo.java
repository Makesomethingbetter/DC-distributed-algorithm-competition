import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import static java.lang.Math.ceil;
import static java.lang.Math.toIntExact;


/**
 *
 * 初始化
 * channel建好以后改变自己的路由（通过先见知识）并向邻居发路由
 * 处理SYS：改变自己的路由，向邻居发路由
 *
 *
 * */
public class Topo {

    private Scheduler scheduler;

    private int nodeId;
    private int nodeCount;
    private int maxHighChannelCount;
    private float lagOfHighChannel;
    private float lagOfNormalChannel;

    public int getNodeCount() {
        return nodeCount;
    }

    private float timeOut;
    //记录初始化已经被用的点



    private int tinyGroupSize;
    private int halfGroupNumber;
    private int tinyGroupNumber;
    private int farthestNumber;

    private int enter;
    private int highEnter;
    private int highCircle;
    private boolean highSmall;


    //用来保存到其他点的需要的时间
    private float[] indexToTimeNeedArray;
    //用来路由 当indexToTimeNeedArray改变，indexToChangedByWhoArray也要改
    private int[] indexToChangedByWhoArray;
    //自己直接连接的点
    private HashMap<Integer,Integer> nodesDirectlyLinked;
    //存储收到的随机字符串
    private ArrayList<String> randomStringArrayList;


    public Topo(Scheduler scheduler){
        this.scheduler=scheduler;
        initalArray();
        initalDataFromConfig();
        initalTopoStructure();
        changeMyRouteAfterinitial();
    }


    /**
     *
     * channel建立以后用的函数
     *
     * */
    public void addNodeAfterBuildSuc(int targetId,int channelType){
        nodesDirectlyLinked.put(targetId,channelType);
    }
    /**
     *
     * 初始化数组
     *
     * */
    public void initalArray(){
        randomStringArrayList=new ArrayList<>();
        nodesDirectlyLinked=new HashMap<>();
        indexToTimeNeedArray=new float[nodeCount];
        indexToChangedByWhoArray=new int[nodeCount];
        for (int i=0;i<nodeCount;i++){
            indexToTimeNeedArray[i]=(float) 9999.9;
            indexToChangedByWhoArray[i]=nodeId;
        }
    }


    /**
     *
     * 从config读配置到类
     *
     * */
    public void initalDataFromConfig(){
        nodeId=Main.config.index;
        nodeCount=Main.config.mainConfig.nodeCount;
        timeOut=Main.config.mainConfig.timeOut;
        lagOfHighChannel= Main.config.channelConfig.highSpeed.lag;
        lagOfNormalChannel=Main.config.channelConfig.normalSpeed.lag;
        maxHighChannelCount=Main.config.channelConfig.highSpeed.maxCount;
    }

    /**
     *
     *  通过config读的数据初始化拓扑结构
     *  这里需要多次修改达到最好的效果
     *
     * */
    public void initalTopoStructure(){
        enter=1;
        tinyGroupSize=15;
        halfGroupNumber=(int)(   ((float)(nodeCount/2))   / tinyGroupSize );
        tinyGroupNumber=halfGroupNumber*2;
        farthestNumber=halfGroupNumber/2;
        //根据这个判断性能
        while (farthestNumber>=4){
            farthestNumber/=2;
            enter++;
        }
        //计算出highEnter\highCircle\highSmall
        highEnter=1;
        highCircle=2;
        highSmall=false;
        if (checkStrategy1_1()>=maxHighChannelCount){
            highEnter=enter;
            highCircle=tinyGroupNumber;
            highSmall=true;
        }
        else if (checkStrategy1_2()>=maxHighChannelCount){
            highEnter=enter;
            highCircle=tinyGroupNumber;
        }
        else if (checkStrategy2_1(highEnter)>=maxHighChannelCount){
            for (int i=enter;i<highEnter;i--){
                if (checkStrategy2_1(i)>=maxHighChannelCount){
                    highEnter=i;
                    highCircle=tinyGroupNumber;
                    highSmall=true;
                }
            }
        }
        else if (checkStrategy2_2(highEnter)>=maxHighChannelCount){
            for (int i=enter;i<highEnter;i--){
                if (checkStrategy2_2(i)>=maxHighChannelCount){
                    highEnter=i;
                    highCircle=tinyGroupNumber;
                }
            }
        }
        else if (checkStrategy3_1(highCircle)>=maxHighChannelCount){
            for (int i=halfGroupNumber;i<highCircle;i--){
                if (checkStrategy3_1(i)>=maxHighChannelCount){
                    highEnter=enter;
                    highCircle=i;
                    highSmall=true;
                }
            }

        }
        else if (checkStrategy3_2(highCircle)>=maxHighChannelCount){
            for (int i=halfGroupNumber;i<highCircle;i--){
                if (checkStrategy3_2(i)>=maxHighChannelCount){
                    highEnter=enter;
                    highCircle=i;
                }
            }

        }
        else if(checkStrategy4_1(highEnter,highCircle)>=maxHighChannelCount){
            for (int i=halfGroupNumber;i<highCircle;i++){
                for (int j=enter;j<highEnter;j--){
                    if (checkStrategy4_1(j,i)>=maxHighChannelCount){
                        highEnter=j;
                        highCircle=i;
                        highSmall=true;
                    }
                }
            }
        }
        else if(checkStrategy4_2(highEnter,highCircle)>=maxHighChannelCount){
            for (int i=halfGroupNumber;i<highCircle;i++){
                for (int j=enter;j<highEnter;j--){
                    if (checkStrategy4_2(j,i)>=maxHighChannelCount){
                        highEnter=j;
                        highCircle=i;
                    }
                }
            }
        }
        //连接
        int leftCount=tinyGroupSize-1-enter*2;
        for (int i=0;i<tinyGroupNumber;i++){
            if (nodeId==1+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeCount/2,Const.STATE_REQUEST,Const.ERR_CODE_NONE,Const.CHANNEL_TYPE_FAST);
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,Const.CHANNEL_TYPE_FAST);
                break;
            }else if (nodeId==2+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,Const.CHANNEL_TYPE_FAST);
                scheduler.sendChannelBuild(nodeId+tinyGroupSize-1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,Const.CHANNEL_TYPE_FAST);
                break;
            }else if (nodeId==3+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highCircle>=2?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                ////////////////////
                if (enter>=2){
                    scheduler.sendChannelBuild(nodeId+tinyGroupSize-1+farthestNumber*tinyGroupSize,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highEnter>=2?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                }else if (enter==1){
                    scheduler.sendChannelBuild(tinyGroupSize*i+1+(tinyGroupSize-1)/2,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highSmall?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                }
                break;
            }else if (nodeId==4+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highCircle>=3?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                /////////////////////
                if (enter>=3){
                    scheduler.sendChannelBuild(nodeId+tinyGroupSize-1+farthestNumber/2*tinyGroupSize,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highEnter>=3?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                }else if (enter==2){
                    scheduler.sendChannelBuild(tinyGroupSize*i+1+(tinyGroupSize-1)/2,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highSmall?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                }
                break;
            }else if (nodeId==5+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highCircle>=4?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                ///////////////////////
                if (enter>=4){
                    scheduler.sendChannelBuild(nodeId+tinyGroupSize-1+farthestNumber/4*tinyGroupSize,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highEnter>=4?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                }else if (enter==3){
                    scheduler.sendChannelBuild(tinyGroupSize*i+1+(tinyGroupSize-1)/2,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highSmall?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                }
                break;
            }else if (nodeId==6+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highCircle>=5?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                if (enter>=5){
                    scheduler.sendChannelBuild(nodeId+tinyGroupSize-1+farthestNumber/8*tinyGroupSize,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highEnter>=5?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                }else if (enter==4){
                    scheduler.sendChannelBuild(tinyGroupSize*i+1+(tinyGroupSize-1)/2,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highSmall?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                }
                break;
            }else if (nodeId==7+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highCircle>=6?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                break;
            }else if (nodeId==8+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highCircle>=7?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                scheduler.sendChannelBuild(i*tinyGroupSize+tinyGroupSize-1-enter,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highSmall?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                break;
            }else if (nodeId==9+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highCircle>=8?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                break;
            }else if (nodeId==10+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highCircle>=9?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                break;
            }else if (nodeId==11+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highCircle>=10?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                break;
            }else if (nodeId==12+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,highCircle>=11?Const.CHANNEL_TYPE_FAST:Const.CHANNEL_TYPE_NORMAL);
                break;
            }else if (nodeId==13+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,Const.CHANNEL_TYPE_FAST);
                break;
            }else if (nodeId==14+i*tinyGroupSize){
                scheduler.sendChannelBuild(nodeId+1,Const.STATE_REQUEST,Const.ERR_CODE_NONE,Const.CHANNEL_TYPE_FAST);
                break;
            }
        }

    }

    private int checkStrategy1_1(){
        int all=0;
        //圆用高速连接
        all+=nodeCount;
        //半圆
        all+=halfGroupNumber;
        all+=tinyGroupNumber*enter;
        all+=tinyGroupNumber*2;
        return all;
    }
    private int checkStrategy1_2(){
        int all=0;
        //圆用高速连接
        all+=nodeCount;
        //半圆
        all+=halfGroupNumber;
        all+=tinyGroupNumber*enter;
        return all;
    }

    private int checkStrategy2_1(int i){
        int all=0;
        all+=nodeCount;
        all+=halfGroupNumber;
        all+=tinyGroupNumber*i;
        all+=tinyGroupNumber*2;
        return all;
    }
    private int checkStrategy2_2(int i){
        int all=0;
        all+=nodeCount;
        all+=halfGroupNumber;
        all+=tinyGroupNumber*i;
        return all;
    }

    private int checkStrategy3_1(int i){
        int all=0;
        all+=halfGroupNumber;
        all+=tinyGroupNumber*enter;
        all+=tinyGroupNumber*i*2;
        all+=tinyGroupNumber*2;
        return all;
    }
    private int checkStrategy3_2(int i){
        int all=0;
        all+=halfGroupNumber;
        all+=tinyGroupNumber*enter;
        all+=tinyGroupNumber*i*2;
        return all;
    }

    //第一个是enter 第二个是highCircle
    private int checkStrategy4_1(int i,int j){
        int all=0;
        all+=halfGroupNumber;
        all+=tinyGroupNumber*i;
        all+=tinyGroupNumber*j*2;
        all+=tinyGroupNumber*2;
        return all;
    }

    private int checkStrategy4_2(int i,int j){
        int all=0;
        all+=halfGroupNumber;
        all+=tinyGroupNumber*i;
        all+=tinyGroupNumber*j*2;
        return all;
    }

    /**
     *
     * 通过参数中的数组改变自己的数组，如果参数中下标为1的值比自己下标为i的值小
     * 就改变自己下标为i的值，并改变indexToChangedByWhoArray[i]为targertId
     *
     * */
    public void changeMyRouteWhenReceiveSYS(String str,int targetId){
        String[] strArray=str.split(",");
        for (int i=0;i<nodeCount;i++){
            float timeNeed=Float.valueOf(strArray[i]);
            timeNeed+=nodesDirectlyLinked.get(targetId);
            if (timeNeed<indexToTimeNeedArray[i]){
                indexToTimeNeedArray[i]=timeNeed;
                indexToChangedByWhoArray[i]=targetId;
            }
        }
    }

    /**
     *
     *根据圆的先见知识
     *有问题没写好：因为不能整除就炸了
     *
     *
     * */
    public void changeMyRouteAfterinitial(){
        float[] timeBettwenNodes=new float[nodeCount];
        for (int i=0;i<tinyGroupNumber/2;i++){
            for (int j=0;j<tinyGroupSize;i++){
                if (j<highCircle){
                    timeBettwenNodes[i*tinyGroupNumber+tinyGroupSize]=lagOfHighChannel;
                }
                else
                    timeBettwenNodes[i*tinyGroupNumber+tinyGroupSize]=lagOfNormalChannel;
            }
        }

        for (int i=2;i<2/nodeCount;i++){
            indexToTimeNeedArray[nodeId-1+i<nodeCount?nodeId-1+i:(nodeId-1+i)-nodeCount]=
                    indexToTimeNeedArray[nodeId-1-1+i<=nodeCount?nodeId-1-1+i:nodeCount-(nodeId-1-1+i)]
                            + timeBettwenNodes[nodeId-1+i<=nodeCount?nodeId-1+i:nodeCount-(nodeId-1+i)];
            indexToChangedByWhoArray[nodeId-1+i<nodeCount?nodeId-1+i:(nodeId-1+i)-nodeCount]=nodeId+1;
            indexToTimeNeedArray[nodeId-1-i>0?nodeId-1-i:nodeCount+(nodeId-1-i)]=
                    indexToTimeNeedArray[nodeId-1-1-i>0?nodeId-1-1-i:nodeCount+nodeId-1-1-i]
                            + timeBettwenNodes[nodeId-1-i>0?nodeId-1-i:nodeCount+(nodeId-1-i)];
            indexToChangedByWhoArray[nodeId-1-i>0?nodeId-1-i:nodeCount+(nodeId-1-i)]=nodeId-1;

        }
        indexToTimeNeedArray[nodeId-1]=0;
        indexToChangedByWhoArray[nodeId-1]=nodeId;
    }


    /**
     *
     * 在doSendSys中使用
     * sysMessage中的data为String
     * 把indexToTimeNeedArray转换成字符串，用于message的data发送
     *
     * */
    public String transferIndexToTimeNeedArrayToString(){
        StringBuilder sb=new StringBuilder();
        for (int i=0;i<indexToTimeNeedArray.length;i++){
            sb.append(indexToTimeNeedArray[i]);
            sb.append(",");
        }
        String str=sb.toString();
        return str;
    }


    /**
     *
     * 处理邻居发来的
     *
     * 参数：message里的nodesHasRead（记录这个信息已经到达的点，
     * 若这个里面有自己就扔掉这个信息，
     * 否则在nodesHasRead中加上自己并调用函数sendSysRouteToNodesDirectlyLinked）
     *
     *
     * */
    public void dealWithDataInSYSMessage(String dataString,int targetId){

        String str=dataString.split("|")[0];
        String randomString=dataString.split("|")[1];
        saveRandomString(randomString);
        //改变自己的路由表
        changeMyRouteWhenReceiveSYS(str,targetId);
        if (!checkRandomStringExits(randomString)){
            //发给所有邻居
            sendSysRouteToNodesDirectlyLinked();
        }
    }


    public String generateRandomString(){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int length=10;
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(62);
            sb.append(str.charAt(number));
        }
        String randomString=sb.toString();
        return randomString;
    }

    public boolean checkRandomStringExits(String str){
        boolean exit=false;
        exit= randomStringArrayList.contains(str);
        return exit;
    }

    public void saveRandomString(String randomString){
        randomStringArrayList.add(randomString);
    }

    /**
     *
     * 使用场景：
     * 1自己建好了channel后
     * 2在dealWithIndexToTimeNeedArrayInSYSMessage中
     *
     * 向自己直接相连的点发路由信息
     * 参数：自己的indexToTimeNeedArray，
     * 自己的nodesDirectlyLinked，
     * message里的nodesHasRead（记录这个信息已经到达的点，若这个里面有自己就扔掉这个信息，否则在nodesHasRead中加上自己并调用这个函数）
     *
     * */
    public void sendSysRouteToNodesDirectlyLinked(){
        String str=transferIndexToTimeNeedArrayToString();
        String randomString=generateRandomString();
        String dataString =  str+"|"+randomString;
        Integer keyArray[];
        keyArray=(Integer[])nodesDirectlyLinked.keySet().toArray();
        for (int i=0;i<keyArray.length;i++){
            scheduler.sendSys(keyArray[i],Const.STATE_NOTICE,Const.ERR_CODE_NONE,dataString);
        }
    }

    /**
     *
     * 参数：nodeId
     * 返回值：nextHop
     *
     * */
    public int nodeId2NextHop(int nodeId){
        return indexToChangedByWhoArray[nodeId-1];
    }

    /**
     *
     * 通过nodeId判断我是不是圆上
     *
     * */

    public boolean isNormalNodeByNodeId(int nodeId){
        boolean is=false;
        return is;
    }

    /**
     *
     * 我还能连吗
     *
     * */
    public boolean canLinkAnyMore(){
        boolean can=false;
        if (nodesDirectlyLinked.size()<Main.config.maxChannelConn){
            can=true;
        }
        return can;
    }

    /**
     * 参数：nodeId
     * 返回值：到该nodeId所需要的时间
     *
     * */
    public float indexToTimeNeed(int nodeId){
        return indexToTimeNeedArray[nodeId-1];
    }

    /**
     *
     * 如果ignoreWho<0则都不忽略
     *
     * */
    public int getNodeLongestTimeIgnoreOneNode(int ignoreWho){
        int longestTimeNodeId=1;
        float temp=1;
        for (int i=0;i<nodeCount;i++){
            if (i+1==ignoreWho)
                continue;
            if (indexToTimeNeedArray[i]>temp){
                temp=longestTimeNodeId;
                longestTimeNodeId=i+1;
            }
        }
        return longestTimeNodeId;
    }

    /**
     *
     *
     *
     * */
    public boolean isAfterInitial(){
        boolean afterInitial=false;
        double nowTime=new Date().getTime();
        if (nowTime-Main.beginTime>3){
            afterInitial=true;
        }
        return afterInitial;
    }

    /**
     *
     * 向最远的建立连接
     *
     * */
    public void linkToLongest(){
        int longestNodeId=getNodeLongestTimeIgnoreOneNode(-1);
        scheduler.sendChannelBuild(longestNodeId,Const.STATE_REQUEST,Const.ERR_CODE_NONE,Const.CHANNEL_TYPE_NORMAL);
    }

}
