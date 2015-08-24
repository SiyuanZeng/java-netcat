package com.github.dddpaul.net;

import com.github.dddpaul.netcat.NetCat;
import org.jooq.lambda.Unchecked;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;

public class DatagramTest {

    public static final String LISTENER_INPUT = "Input from listener, привет, €, 汉语\n";
    public static final String CONNECTOR_INPUT = "Input from connector, привет, €, 汉语\n";
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 9999;
    public static final int TIMEOUT = 20000;

    private NetCat listener, connector;


    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testUdp() throws Exception {
        ByteArrayInputStream input1 = new ByteArrayInputStream(LISTENER_INPUT.getBytes());
        listener = new NetCat(new NetCat.Options(true, true, HOST, PORT, input1, System.out));
        new Thread(Unchecked.runnable(listener::listen)).start();
        Thread.sleep(100);

        ByteArrayInputStream input2 = new ByteArrayInputStream(CONNECTOR_INPUT.getBytes());
        connector = new NetCat(new NetCat.Options(false, true, HOST, PORT, input2, System.out, TIMEOUT));
        connector.connect();
    }
}
