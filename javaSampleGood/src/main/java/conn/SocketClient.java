package conn;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketClient {
    private SocketChannel channel;
    private ByteBuffer readBuffer = ByteBuffer.allocateDirect(1024 * 10);
    private StringBuffer stringBuffer = new StringBuffer();
    private ByteBuffer writeBuffer = ByteBuffer.allocateDirect(1024 * 10);
    private int left = 0;
    //private Socket socket;

    public SocketClient(InetAddress ip, int port) throws IOException, InterruptedException {
        channel = SocketChannel.open(new InetSocketAddress(ip, port));
        channel.configureBlocking(false);
        while (!channel.isConnected()){
            Thread.sleep(10);
        }
    }

    public void println(String message) throws IOException {
        writeBuffer.clear();
        writeBuffer.put(message.getBytes());

        /**
         * //flip反转buffer clear清理buffer mark标记buffer
         * flip翻转主要是在Buffer的读写之间进行切换，flip后，将limit设置为position的大小，position归零，从0位开始读，读到limit为止。
         *
         * */
        writeBuffer.flip();
        while (writeBuffer.hasRemaining()) {
            channel.write(writeBuffer);
        }
    }


    /**
     *
     * 如果readBuffer空->从channel读数据到readBuffer
     * 如果readBuffer不空->读readBuffer，读到\n返回读到的字符串.(如果把readBuffer读完也没有\n，那么把read到的东西存到stringBuffer供下次使用，并返回“”）
     * left用来监控readBuffer是否为空
     *
     * 结果：当readLine一直在用的时候，就一行一行的返回，不在乎channel
     *
     * */
    public String readLine() throws IOException{
        if (left <= 0) {
            readBuffer.clear();
            left = channel.read(readBuffer);
            readBuffer.flip();
        }
        if (left <= 0) {
            //System.out.println("read nothing");
            return "";
        }
        while (left > 0) {
            char cur = (char) readBuffer.get();
            --left;
            if (cur == '\n') {
                String result = stringBuffer.toString();
                stringBuffer.setLength(0);
                return result;
            } else {
                stringBuffer.append(cur);
            }
        }
        System.out.println("no \\n, cur:" + stringBuffer.toString());
        return "";
    }

    public void close() throws IOException {
        if (channel != null && channel.isOpen())
            channel.close();
    }
}