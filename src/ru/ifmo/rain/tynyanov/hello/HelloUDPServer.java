package ru.ifmo.rain.tynyanov.hello;

import info.kgeorgiy.java.advanced.hello.*;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class HelloUDPServer implements HelloServer {
    private ExecutorService threads;
    private ExecutorService requestsReceiver;
    private DatagramSocket socket;

    @Override
    public void start(int port, int threadsNum) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.out.println("Error while creating socket on port: " + port);
            return;
        }
        threads = Executors.newFixedThreadPool(threadsNum);
        requestsReceiver = Executors.newSingleThreadExecutor();
        requestsReceiver.submit(() -> {
            try {
                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    DatagramPacket receivedRequest = new DatagramPacket(new byte[socket.getReceiveBufferSize()],
                            socket.getReceiveBufferSize());
                    socket.receive(receivedRequest);
                    threads.submit(() -> {
                        String receivedMessage = new String(receivedRequest.getData(),
                                receivedRequest.getOffset(), receivedRequest.getLength(), StandardCharsets.UTF_8);
                        String responseText = "Hello, " + receivedMessage;
                        DatagramPacket responsePacket = new DatagramPacket(new byte[0], 0, receivedRequest.getSocketAddress());
                        responsePacket.setData(responseText.getBytes(StandardCharsets.UTF_8));
                        try {
                            socket.send(responsePacket);
                        } catch (IOException e) {
                            System.out.println("Error while sending response: " + e.getMessage());
                        }
                    });
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.out.println("Error while working with packets: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void close() {
        requestsReceiver.shutdownNow();
        threads.shutdownNow();
        threads.shutdown();
        socket.close();
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("You are expected to enter 2 arguments");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                System.out.println("All arguments must be not null");
                return;
            }
        }
        int port;
        int threads;
        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("One of arguments can't be converted to int: " + e.getMessage());
            return;
        }
        new HelloUDPServer().start(port, threads);
    }
}
