package com.github.dddpaul.netcat;

import java.io.*;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.Callable;

import static com.github.dddpaul.netcat.NetCat.*;

public class DatagramSender implements Callable<Long> {

    private BufferedInputStream input;
    private DatagramChannel channel;
    private SocketAddress remoteAddress;

    public DatagramSender(InputStream input, DatagramChannel channel, SocketAddress remoteAddress) {
        this.input = new BufferedInputStream(input, BUFFER_LIMIT);
        this.channel = channel;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public Long call() {
        long total = 0;
        ByteBuffer buf = ByteBuffer.allocate(BUFFER_LIMIT);
        try {
            int bytes;
            while ((bytes = input.read(buf.array())) != -1) {
                total += channel.send(ByteBuffer.wrap(buf.array(), 0, bytes), remoteAddress);
                buf.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }
}
