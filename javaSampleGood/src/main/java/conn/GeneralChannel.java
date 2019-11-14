package conn;


import com.fasterxml.jackson.databind.ObjectMapper;
import json.config.Config;
import json.Message;
import json.config.NodeConfig;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class GeneralChannel implements Channel {

    private final static ObjectMapper mapper = new ObjectMapper();

    private SocketClient client;
    private Config config;

    public GeneralChannel() throws Exception { }

    private void initMainConfig() throws Exception {
        //socket连接到master（master应该是比赛方的服务器
        client = new SocketClient(InetAddress.getByName(config.mainConfig.masterIp), config.mainConfig.masterPort);
        while (true) {
            String line = client.readLine();
            if (line.length() > 0) {
                System.out.println("id recved :" + line);
                NodeConfig nodeConfig = GeneralChannel.mapper.readValue(line, NodeConfig.class);
                config.index = nodeConfig.index;
                config.maxChannelConn = nodeConfig.maxChannelConn;
                break;
            }
            Thread.sleep(50);
        }
    }

    @Override
    public void initConfig(Config config) throws Exception {
        this.config = config;
        initMainConfig();
    }

    @Override
    public void send(Message message, int targetId) throws Exception {
        message.targetId = targetId;
        String buf = GeneralChannel.mapper.writeValueAsString(message) + '\n';
        System.out.println("msg send   :" + buf);
        client.println(buf);
    }


    /**
     *
     * 有一个问题：
     * 当通道一直在向A传信息，A一直得不到result...
     * 当然总会得到result，但是这样也会增加delay
     *
     * */
    @Override
    public List<Message> recv() throws Exception{
        List<Message> result = new ArrayList<>();
        while (true) {
            String line = client.readLine();
            if (line.length() > 0) {
                result.add(GeneralChannel.mapper.readValue(line, Message.class));
            } else {
                break;
            }
        }
        return result;
    }

    @Override
    public int getId() {
        return config.index;
    }
}
