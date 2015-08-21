package com.github.dddpaul.netcat;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

public class StreamTransferer implements Callable<Long> {
    private BufferedInputStream input;
    private BufferedOutputStream output;

    public StreamTransferer(InputStream input, OutputStream output) {
        this.input = new BufferedInputStream(input);
        this.output = new BufferedOutputStream(output);
    }

    @Override
    public Long call() {
        long total = 0;
        ByteBuffer buf = ByteBuffer.allocate(NetCat.BUFFER_LIMIT);
        try {
            int bytes;
            while ((bytes = input.read(buf.array())) != -1) {
                output.write(buf.array(), 0, bytes);
                output.flush();
                total += bytes;
                buf.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return total;
    }
}
