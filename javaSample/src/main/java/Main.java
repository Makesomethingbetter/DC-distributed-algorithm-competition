import com.fasterxml.jackson.databind.ObjectMapper;
import conn.Channel;
import conn.GeneralChannel;
import json.Message;
import json.config.Config;

import java.io.File;
import java.util.Date;
import java.util.List;

public class Main {

    public static Config config;

    public static double curTime() {
        return new Date().getTime() / 1000.0;
    }

    public static void main(String[] args) throws Exception {
        //读JSON配置
        ObjectMapper objectMapper = new ObjectMapper();
        config = objectMapper.readValue(new File("resources/client.json"), Config.class);
        //生成channel（这个是server的！） 配置
        Channel channel = new GeneralChannel();
        channel.initConfig(config);
        //
        mainloop(channel);
    }

    public static void mainloop(Channel channel){
        Scheduler scheduler = new Scheduler(channel);
        while (true) {
            try {
                List<Message> message = channel.recv();
                for (Message msg : message) {
                    scheduler.onRecv(msg);
                }
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
