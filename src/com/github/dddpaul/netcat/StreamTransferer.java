package com.github.dddpaul.netcat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

public class StreamTransferer implements Callable<Long> {
    private InputStream input;
    private OutputStream output;

    public StreamTransferer(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public Long call() {
        long total = 0;
        try {
            PrintWriter writer = new PrintWriter(output);
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = reader.readLine()) != null) {
                total += line.length();
                writer.println(line);
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return total;
    }
}
