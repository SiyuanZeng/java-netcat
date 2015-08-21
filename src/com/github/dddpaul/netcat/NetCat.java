package com.github.dddpaul.netcat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.DatagramChannel;

import org.apache.commons.cli.*;

public class NetCat {
    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("l", "listen", false, "listen mode");
        options.addOption("p", "port", true, "port number");
        options.addOption("u", "udp", false, "UDP instead TCP");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('l')) {
            if (line.hasOption('p')) {
                int port = Integer.parseInt(line.getOptionValue('p'));
                if (line.hasOption('u')) {
                    listenUdp(port);
                } else {
                    listen(port);
                }
            }
        } else {
            if (line.hasOption('p')) {
                int port = Integer.parseInt(line.getOptionValue('p'));
                connect(line.getArgs()[0], port);
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("netcat [OPTIONS] <HOST>", options);
            }
        }
    }

    private static void connect(String host, int port) throws Exception {
        System.err.println("Connecting to " + host + " port " + port);
        final Socket socket = new Socket(host, port);
        transferStreams(socket);
    }

    private static void listen(int port) throws Exception {
        System.err.println("Listening at TCP:" + port);
        ServerSocket serverSocket = new ServerSocket(port);
        Socket socket = serverSocket.accept();
        System.err.println("Accepted");
        transferStreams(socket);
    }

    private static void listenUdp(int port) throws Exception {
        System.err.println("Listening at UDP:" + port);
        DatagramChannel channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(port));
        channel.configureBlocking(true);
        transferDatagrams(channel);
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
