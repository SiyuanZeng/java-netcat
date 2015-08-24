package com.github.dddpaul.netcat;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Callable;

import static com.github.dddpaul.netcat.NetCat.BUFFER_LIMIT;

public class StreamReceiver implements Callable<Long> {
    private SocketChannel input;
    private WritableByteChannel output;

    public StreamReceiver(SocketChannel input, OutputStream output) {
        this.input = input;
        this.output = Channels.newChannel(output);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return total;
    }
}
