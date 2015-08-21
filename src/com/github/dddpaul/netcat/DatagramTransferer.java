package com.github.dddpaul.netcat;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.Callable;

public class DatagramTransferer implements Callable<Long> {
    private static final String DISCONNECT_SEQUENCE = "~.";
    private static InetSocketAddress remoteAddress;

    private BufferedInputStream input;
    private BufferedOutputStream output;
    private DatagramChannel channel;

    public DatagramTransferer(InputStream input, DatagramChannel channel) {
        this.input = new BufferedInputStream(input);
        this.channel = channel;
    }

    public DatagramTransferer(DatagramChannel channel, OutputStream output) {
        this.channel = channel;
        this.output = new BufferedOutputStream(output);
    }

    @Override
    public Long call() {
        long total = 0;
        ByteBuffer buf = ByteBuffer.allocate(NetCat.BUFFER_LIMIT);
        try {
            if (input != null) {
                total = send(buf);
            } else {
                total = receive(buf);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }

    private int send(ByteBuffer buf) throws IOException {
        int bytesSent = 0;
        int bytes;
        while ((bytes = input.read(buf.array())) != -1) {
            bytesSent += channel.send(ByteBuffer.wrap(buf.array(), 0, bytes), remoteAddress);
            buf.clear();
        }
        return bytesSent;
    }

    private int receive(ByteBuffer buf) throws IOException, InterruptedException {
        remoteAddress = (InetSocketAddress) channel.receive(buf);
        System.err.println(String.format("Accepted from [%s:%d]", remoteAddress.getAddress(), remoteAddress.getPort()));
        int bytesReceived = buf.position() - 1;
        channel.connect(remoteAddress);
        while (true) {
            output.write(buf.array(), 0, buf.position());
            output.flush();
            buf.clear();
            bytesReceived += channel.read(buf);
            if (DISCONNECT_SEQUENCE.equals(new String(buf.array(), 0, buf.position()).trim())) {
                break;
            }
        }
        return bytesReceived;
    }
}
