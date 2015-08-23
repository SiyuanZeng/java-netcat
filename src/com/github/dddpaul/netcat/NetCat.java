package com.github.dddpaul.netcat;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;

public class NetCat {

    // Ready to handle full-size UDP datagram or TCP segment in one step
    public static int BUFFER_LIMIT = 2 << 16 - 1;

    private static ExecutorService executor = Executors.newFixedThreadPool(2);

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
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(host, port));
        transferStreams(channel);
    }

    private static void listen(boolean udp, int port) throws Exception {
        System.err.println(String.format("Listening at %s:%d", udp ? "UDP" : "TCP", port));
        if (udp) {
            listenUdp(port);
        } else {
            listenTcp(port);
        }
    }

    private static void listenUdp(int port) throws Exception {
        DatagramChannel channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(port));
        channel.configureBlocking(true);
        transferDatagrams(channel);
    }

    private static void listenTcp(int port) throws Exception {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(true);
        SocketChannel channel = serverChannel.accept();
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.getRemoteAddress();
        System.err.println(String.format("Accepted from [%s:%d]", remoteAddress.getAddress().getHostAddress(), remoteAddress.getPort()));
        transferStreams(channel);
    }


    private static void transferStreams(final SocketChannel channel) throws IOException, ExecutionException, InterruptedException {
        // Shutdown socket when this program is terminated
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    channel.shutdownOutput();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        executor.submit(new StreamTransferer(System.in, channel.socket().getOutputStream()));
        Future<Long> future = executor.submit(new StreamTransferer(channel.socket().getInputStream(), System.out));

        // Wait till other side is terminated
        long bytesReceived = future.get();
        System.err.println("bytesReceived = " + bytesReceived);
        System.exit(0);
    }

    private static void transferDatagrams(final DatagramChannel channel) throws InterruptedException, ExecutionException {
        BlockingQueue<SocketAddress> queue = new LinkedBlockingQueue<>();
        Future<Long> future = executor.submit(new DatagramReceiver(channel, System.out, queue));

        // Start sender after remote address will be determined
        SocketAddress remoteAddress = queue.take();
        executor.submit(new DatagramSender(System.in, channel, remoteAddress));

        // Wait till other side is terminated
        long bytesReceived = future.get();
        System.err.println("bytesReceived = " + bytesReceived);
        System.exit(0);
    }
}
