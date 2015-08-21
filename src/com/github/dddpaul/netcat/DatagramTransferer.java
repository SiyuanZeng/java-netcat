package com.github.dddpaul.netcat;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class DatagramTransferer implements Runnable {

    // Ready to get full-size UDP datagram or TCP segment in one step
    public static int RECEIVE_BUFFER_LIMIT = 2 << 16 - 1;

    // Ethernet-safe buffer limit
    public static int SEND_BUFFER_LIMIT = 1400;

    private static InetSocketAddress remoteAddress;

    private InputStream input;
    private OutputStream output;
    private DatagramChannel channel;

    public DatagramTransferer(InputStream input, DatagramChannel channel) {
        this.input = input;
        this.channel = channel;
    }

    public DatagramTransferer(DatagramChannel channel, OutputStream output) {
        this.channel = channel;
        this.output = output;
    }

    @Override
    public void run() {
        ByteBuffer buf = ByteBuffer.allocate(input != null ? SEND_BUFFER_LIMIT : RECEIVE_BUFFER_LIMIT);
        try {
            while (true) {
                if (input != null) {
                    send(buf);
                } else {
                    receive(buf);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int send(ByteBuffer buf) throws IOException {
        int bytesSent = 0;
        buf.clear();
        int bytesRead = input.read(buf.array());
        if (bytesRead > 0) {
            bytesSent = channel.send(ByteBuffer.wrap(buf.array(), 0, bytesRead), remoteAddress);
        }
        return bytesSent;
    }

    private int receive(ByteBuffer buf) throws IOException {
        int bytesReceived;
        buf.clear();
        if (!channel.isConnected()) {
            InetSocketAddress oldRemoteAddress = remoteAddress;
            remoteAddress = (InetSocketAddress) channel.receive(buf);
            if (remoteAddress != null && !remoteAddress.equals(oldRemoteAddress)) {
                System.err.println(String.format("Accepted from [%s:%d]", remoteAddress.getAddress(), remoteAddress.getPort()));
            }
            bytesReceived = buf.position() - 1;
        } else {
            bytesReceived = channel.read(buf);
        }
        if (bytesReceived > 0) {
            output.write(buf.array(), 0, buf.position());
        }
        return bytesReceived;
    }
}
