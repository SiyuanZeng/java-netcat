package com.github.dddpaul.netcat;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;

public class NetCat {

    // Ready to handle full-size UDP datagram or TCP segment in one step
    public static int BUFFER_LIMIT = 2 << 16 - 1;

    private ExecutorService executor;

    private Options opt;

    public NetCat(Options opt) {
        this.opt = opt;
        executor = Executors.newFixedThreadPool(2);
    }

    public static class Options {
        @Option(name = "-l", usage = "Listen mode, default false")
        public boolean listen = false;

        @Option(name = "-u", usage = "UDP instead TCP, default false")
        public boolean udp = false;

        @Option(name = "-p", usage = "Port number, default 9999")
        public int port = 9999;

        @Argument(usage = "Host, default 127.0.0.1", metaVar = "host")
        public String host = "127.0.0.1";

        public InputStream input = System.in;
        public OutputStream output = System.out;
        public int timeout;

        public Options() {
        }

        public Options(boolean listen, boolean udp, String host, int port, InputStream input, OutputStream output, int timeout) {
            this.listen = listen;
            this.udp = udp;
            this.port = port;
            this.host = host;
            this.input = input;
            this.output = output;
            this.timeout = timeout;
        }

        public Options(boolean listen, boolean udp, String host, int port, InputStream input, OutputStream output) {
            this(listen, udp, host, port, input, output, 0);
        }
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

        new NetCat(opt).run();
    }

    private NetCat run() throws Exception {
        if (opt.listen) {
            listen();
        } else {
            connect();
        }
        return this;
    }

    public void connect() throws Exception {
        System.err.println(String.format("Connecting to [%s:%d]", opt.host, opt.port));
        if (opt.udp) {
            connectUdp();
        } else {
            connectTcp();
        }
    }

    public void listen() throws Exception {
        System.err.println(String.format("Listening at %s:%d", opt.udp ? "UDP" : "TCP", opt.port));
        if (opt.udp) {
            listenUdp();
        } else {
            listenTcp();
        }
    }

    private void connectUdp() throws Exception {
        DatagramChannel channel = DatagramChannel.open();
        SocketAddress remoteAddress = new InetSocketAddress(opt.host, opt.port);
        channel.connect(remoteAddress);

        executor.submit(new DatagramSender(opt.input, channel, remoteAddress, opt.timeout));
        Future<Long> future = executor.submit(new DatagramReceiver(channel, opt.output, null));

        // Wait till other side is terminated
        long bytesReceived = future.get();
        System.err.println("bytesReceived = " + bytesReceived);
        System.exit(0);
    }

    private void connectTcp() throws Exception {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(opt.host, opt.port));
        transferStreams(channel);
    }

    private void listenUdp() throws Exception {
        DatagramChannel channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(opt.port));
        channel.configureBlocking(true);

        BlockingQueue<SocketAddress> queue = new ArrayBlockingQueue<>(1);
        Future<Long> future = executor.submit(new DatagramReceiver(channel, opt.output, queue));

        // Start sender after remote address will be determined
        SocketAddress remoteAddress = queue.take();
        executor.submit(new DatagramSender(opt.input, channel, remoteAddress, opt.timeout));

        // Wait till other side is terminated
        long bytesReceived = future.get();
        System.err.println("bytesReceived = " + bytesReceived);
        System.exit(0);
    }

    private void listenTcp() throws Exception {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(opt.port));
        serverChannel.configureBlocking(true);
        SocketChannel channel = serverChannel.accept();
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.getRemoteAddress();
        System.err.println(String.format("Accepted from [%s:%d]", remoteAddress.getAddress().getHostAddress(), remoteAddress.getPort()));
        transferStreams(channel);
    }

    private void transferStreams(final SocketChannel channel) throws IOException, ExecutionException, InterruptedException {
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

        executor.submit(new StreamTransferer(opt.input, channel.socket().getOutputStream()));
        Future<Long> future = executor.submit(new StreamTransferer(channel.socket().getInputStream(), opt.output));

        // Wait till other side is terminated
        long bytesReceived = future.get();
        System.err.println("bytesReceived = " + bytesReceived);
        System.exit(0);
    }
}
