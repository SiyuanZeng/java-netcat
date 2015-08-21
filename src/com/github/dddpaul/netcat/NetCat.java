package com.github.dddpaul.netcat;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.DatagramChannel;

public class NetCat {

    static class Options {
        @Option(name = "-l", usage = "Listen mode, default false")
        public boolean listen = false;

        @Option(name = "-u", usage = "UDP instead TCP, default false")
        public boolean udp = false;

        @Option(name = "-p", usage = "Port number, default 9999")
        public int port = 9999;

        @Argument(usage = "Host, default 127.0.0.1", metaVar = "host")
        public String host = "127.0.0.1";
    }

    public static void main(String[] args) throws Exception {
        Options opt = new Options();
        CmdLineParser parser = new CmdLineParser(opt);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.exit(1);
        }

        if (opt.listen) {
            listen(opt.udp, opt.port);
        } else {
            connect(opt.host, opt.port);
        }
    }

    private static void connect(String host, int port) throws Exception {
        System.err.println("Connecting to " + host + " port " + port);
        final Socket socket = new Socket(host, port);
        transferStreams(socket);
    }

    private static void listen(boolean udp, int port) throws Exception {
        System.err.println(String.format("Listening at %s:%d", udp ? "UDP" : "TCP", port));
        if (udp) {
            listenUdp(port);
        } else {
            listenTcp(port);
        }
    }

    private static void listenTcp(int port) throws Exception {
        DatagramChannel channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(port));
        channel.configureBlocking(true);
        transferDatagrams(channel);
    }

    private static void listenUdp(int port) throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);
        Socket socket = serverSocket.accept();
        System.err.println("Accepted");
        transferStreams(socket);
    }


    private static void transferStreams(final Socket socket) throws IOException, InterruptedException {
        // Shutdown socket when this program is terminated
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    socket.shutdownOutput();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        InputStream input1 = System.in;
        OutputStream output1 = socket.getOutputStream();
        InputStream input2 = socket.getInputStream();
        PrintStream output2 = System.out;

        Thread thread1 = new Thread(new StreamTransferer(input1, output1));
        thread1.setName("Thread1: Local-Remote");

        Thread thread2 = new Thread(new StreamTransferer(input2, output2));
        thread2.setName("Thread2: Remote-Local");

        thread1.start();
        thread2.start();

        // Exit when other side is terminated
        thread2.join();
        System.exit(0);
    }

    private static void transferDatagrams(final DatagramChannel channel) throws IOException, InterruptedException {
        InputStream input = System.in;
        PrintStream output = System.out;

        Thread thread1 = new Thread(new DatagramTransferer(input, channel));
        thread1.setName("Thread1: Local-Remote");

        Thread thread2 = new Thread(new DatagramTransferer(channel, output));
        thread2.setName("Thread2: Remote-Local");

        thread1.start();
        thread2.start();

        // Exit when other side is terminated
        thread2.join();
        System.exit(0);
    }
}
