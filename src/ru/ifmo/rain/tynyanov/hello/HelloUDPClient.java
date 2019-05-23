package ru.ifmo.rain.tynyanov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

public class HelloUDPClient implements HelloClient {
    private final int TIMEOUT = 500;
    @Override
    public void run(String host, int port, String prefix, int threadsNum, int requests) {
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.out.println("Can't reach host: " + host);
            return;
        }
        ExecutorService threads = Executors.newFixedThreadPool(threadsNum);
        Phaser phaser = new Phaser(1);
        for (int i = 0; i < threadsNum; i++) {
            int finalI = i;
            phaser.register();
            threads.submit(() -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(TIMEOUT);
                    int counter = 0;
                    DatagramPacket request = new DatagramPacket(new byte[0], 0, address, port);
                    int receiveSize = socket.getReceiveBufferSize();
                    DatagramPacket response = new DatagramPacket(new byte[receiveSize], receiveSize);
                    while (counter < requests) {
                        String message = String.format("%s%d_%d", prefix, finalI, counter);
                        request.setData(message.getBytes(StandardCharsets.UTF_8));
                        ++counter;
                        while (!socket.isClosed()) {
                            try {
                                socket.send(request);
                                System.out.println("\nRequest: \n" + message);
                                socket.receive(response);
                                String receivedMessage = new String(response.getData(),
                                        response.getOffset(), response.getLength(), StandardCharsets.UTF_8);
                                if (checkResponse(message, receivedMessage)) {
                                    System.out.println("\nResponse: \n" + receivedMessage);
                                    break;
                                }
                            } catch (IOException e) {
                                System.out.println("Error while completing request: " + e.getMessage());
                            }
                        }
                    }
                } catch (SocketException e) {
                    System.out.println("Error while creating socket: " + e.getMessage());
                } finally {
                    phaser.arrive();
                }
            });
        }
        threads.shutdown();
        phaser.arriveAndAwaitAdvance();
    }

    boolean checkResponse(String req, String resp) {
        return resp.contains(req);
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.out.println("You are expected to enter 5 arguments");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                System.out.println("All arguments must be not null");
                return;
            }
        }
        String host = args[0];
        String prefix = args[2];
        int port;
        int threads;
        int requests;
        try {
            port = Integer.parseInt(args[1]);
            threads = Integer.parseInt(args[3]);
            requests = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.out.println("One of arguments can't be converted to int: " + e.getMessage());
            return;
        }
        new HelloUDPClient().run(host, port, prefix, threads, requests);
    }
}