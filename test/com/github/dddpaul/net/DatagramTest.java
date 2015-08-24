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

public class DatagramTest extends Assert {

    public static final String INPUT1 = "Input from listener, привет, €, 汉语\n";
    public static final String INPUT2 = "Input from connector, привет, €, 汉语\n";
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 9999;
    public static final int TIMEOUT = 500;

    private NetCat listener, connector;


    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testUdp() throws Exception {
        ByteArrayInputStream input1 = new ByteArrayInputStream(INPUT1.getBytes());
        ByteArrayOutputStream output1 = new ByteArrayOutputStream();
        listener = new NetCat(new NetCat.Options(true, true, HOST, PORT, input1, output1));
        new Thread(Unchecked.runnable(listener::start)).start();
        Thread.sleep(100);

        ByteArrayInputStream input2 = new ByteArrayInputStream(INPUT2.getBytes());
        ByteArrayOutputStream output2 = new ByteArrayOutputStream();
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
}
