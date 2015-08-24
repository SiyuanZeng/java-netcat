package com.github.dddpaul.net;

import com.github.dddpaul.netcat.NetCat;
import org.jooq.lambda.Unchecked;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.core.Is.is;

public class NetCatTest extends Assert {

    public static final String INPUT1 = "Input from listener, привет, €, 汉语\n";
    public static final String INPUT2 = "Input from connector, привет, €, 汉语\n";
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 9999;
    public static final int TIMEOUT = 500;

    private NetCat listener, connector;

    ByteArrayInputStream input1, input2;
    ByteArrayOutputStream output1, output2;

    @Before
    public void setUp() throws Exception {
        input1 = new ByteArrayInputStream(INPUT1.getBytes());
        output1 = new ByteArrayOutputStream();
        input2 = new ByteArrayInputStream(INPUT2.getBytes());
        output2 = new ByteArrayOutputStream();
    }

    @Test
    public void testUdp() throws Exception {
        listener = new NetCat(new NetCat.Options(true, true, HOST, PORT, input1, output1));
        new Thread(Unchecked.runnable(listener::start)).start();
        Thread.sleep(100);

        connector = new NetCat(new NetCat.Options(false, true, HOST, PORT, input2, output2));
        Future<Long> future = connector.start();

        // Wait till other side is terminated
        long bytesReceived = 0;
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ignored) {
        }
        assertThat("UDP receiver must be terminated by future.get()", bytesReceived, is(0L));
        assertEquals("Connector must receive input from listener", INPUT1, output2.toString());
        assertEquals("Listener must receive input from connector", INPUT2, output1.toString());
    }

    @Test
    public void testTcp() throws Exception {
        listener = new NetCat(new NetCat.Options(true, false, HOST, PORT, input1, output1));
        new Thread(Unchecked.runnable(listener::start)).start();
        Thread.sleep(100);

        connector = new NetCat(new NetCat.Options(false, false, HOST, PORT, input2, output2));
        Future<Long> future = connector.start();

        // Wait till other side is terminated
        long bytesReceived = 0;
        future.get();
        assertThat("TCP receiver must be terminated by future.get()", bytesReceived, is(0L));
        assertEquals("Connector must receive input from listener", INPUT1, output2.toString());
        assertEquals("Listener must receive input from connector", INPUT2, output1.toString());
    }

}
