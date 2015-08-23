package com.github.dddpaul.netcat;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import static com.github.dddpaul.netcat.NetCat.BUFFER_LIMIT;

public class DatagramReceiver implements Callable<Long> {

    private static final String DISCONNECT_SEQUENCE = "~.";

    private BufferedOutputStream output;
    private DatagramChannel channel;
    private BlockingQueue<SocketAddress> queue;

    public DatagramReceiver(DatagramChannel channel, OutputStream output, BlockingQueue<SocketAddress> queue) {
        this.channel = channel;
        this.output = new BufferedOutputStream(output);
        this.queue = queue;
    }

    @Override
    public Long call() {
        long total = 0;
        ByteBuffer buf = ByteBuffer.allocate(BUFFER_LIMIT);
        try {
            InetSocketAddress remoteAddress = (InetSocketAddress) channel.receive(buf);
            System.err.println(String.format("Accepted from [%s:%d]", remoteAddress.getAddress(), remoteAddress.getPort()));
            total = buf.position() - 1;
            channel.connect(remoteAddress);
            queue.put(remoteAddress);
            while (channel.isConnected()) {
                output.write(buf.array(), 0, buf.position());
                output.flush();
                buf.clear();
                total += channel.read(buf);
                if (DISCONNECT_SEQUENCE.equals(new String(buf.array(), 0, buf.position()).trim())) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }
}
