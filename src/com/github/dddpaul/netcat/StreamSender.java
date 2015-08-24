package com.github.dddpaul.netcat;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;

import static com.github.dddpaul.netcat.NetCat.BUFFER_LIMIT;

public class StreamSender implements Callable<Long> {
    private ReadableByteChannel input;
    private SocketChannel output;

    public StreamSender(InputStream input, SocketChannel output) {
        this.input = Channels.newChannel(input);
        this.output = output;
    }

    @Override
    public Long call() {
        long total = 0;
        ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER_LIMIT);
        try {
            while (input.read(buf) != -1) {
                buf.flip();
                total += output.write(buf);
                if (buf.hasRemaining()) {
                    buf.compact();
                } else {
                    buf.clear();
                }
            }
            output.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return total;
    }
}
