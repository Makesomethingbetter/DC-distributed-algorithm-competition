import com.fasterxml.jackson.databind.ObjectMapper;
import conn.Channel;
import conn.GeneralChannel;
import json.Message;
import json.config.Config;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static Config config;

    public static void main(String[] args) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        config = objectMapper.readValue(new File("/home/config/client.json"), Config.class);
        Channel channel = new GeneralChannel();
        channel.initConfig(config);
        //topo初始化
        Topo topo=new Topo();
        mainloop(channel,topo);
    }


    public static void mainloop(Channel channel,Topo topo){
        Scheduler scheduler = new Scheduler(channel,topo);
        int increaseInt=0;
        int interval=3;
        while (true) {
            try {
                List<Message> message = channel.recv();
                for (Message msg : message) {
                    scheduler.onRecv(msg);
                }
                // while（true）每执行3次遍历TargetLimitNodesList
                // 如果我可以建立通道则向TargetLimitNodesList里的点申请建立通道并将申请建立通道的点移除TargetLimitNodesList
                // 如果不能连了，停止遍历TargetLimitNodesList
                increaseInt++;
                if (increaseInt%interval==0){
                    if (topo.getTargetLimitNodesListSize()>0){
                        ArrayList<Integer> list=(ArrayList<Integer>) topo.getTargetLimitNodesList();
                        for (int i=0;i<list.size();i++){
                            if (topo.canLinkAnyMore()){
                                scheduler.sendChannelBuild(list.get(i),Const.STATE_REQUEST,Const.ERR_CODE_NONE,Const.CHANNEL_TYPE_FAST);
                                topo.removeFromTargetLimitNodesList(list.get(i));
                            }else{
                                return;
                            }
                        }
                    }
                }
                //message缓冲
                Thread.sleep(20);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }
}
