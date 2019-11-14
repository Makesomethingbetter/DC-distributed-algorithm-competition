package json.config;

public class Config extends NodeConfig{
    //节点数 timeout时长 比赛方的ip和端口
    public MainConfig mainConfig;
    //高速 普通两种 lag 最大数量 建立时间 最大消息数
    public ChannelConfig channelConfig;

    public MessageConfig messageConfig;
}
