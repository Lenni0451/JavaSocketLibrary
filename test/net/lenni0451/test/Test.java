package net.lenni0451.test;

import net.Lenni0451.JavaSocketLib.client.ClientEventListener;
import net.Lenni0451.JavaSocketLib.client.SocketClient;
import net.Lenni0451.JavaSocketLib.packets.IPacket;
import net.Lenni0451.JavaSocketLib.server.ClientConnection;
import net.Lenni0451.JavaSocketLib.server.ServerEventListener;
import net.Lenni0451.JavaSocketLib.server.SocketServer;

public class Test {

    public static void main(String[] args) throws Throwable {
        SocketServer testServer = new SocketServer(2154);
        SocketClient testClient = new SocketClient("127.0.0.1", 2154, true);

        testClient.getPacketRegister().addPacket("MessagePacket", MessagePacket.class);
        testClient.getPacketRegister().addPacketExecutor(new Object() {
            public void handleMessage(MessagePacket packet) {
                System.out.println("From server: " + packet.message);
            }
        });
        testClient.addEventListener(new ClientEventListener() {
            @Override
            public void onPacketReceive(IPacket packet) {
                testClient.getPacketRegister().callPacketExecutor(packet);
            }
        });

        testServer.getPacketRegister().addPacket("MessagePacket", MessagePacket.class);
        testServer.getPacketRegister().addPacketExecutor(new Object() {
            public void handleMessage(ClientConnection clientConnection, MessagePacket packet) {
                System.out.println("From client " + clientConnection.getAddress().toString().substring(1) + ": " + packet.message);
            }
        });
        testServer.addEventListener(new ServerEventListener() {
            @Override
            public void onPacketReceive(ClientConnection client, IPacket packet) {
                testServer.getPacketRegister().callPacketExecutor(client, packet);
            }
        });

        testServer.bind();
        testClient.connect();

        Thread.sleep(1000);

        System.out.println("sdaf");

        while (true) {
            Thread.sleep(1000);

            testClient.sendPacket(new MessagePacket("Test1"));
            testServer.broadcastPacket(new MessagePacket("Test1"));
        }

//        Thread.sleep(1000);
//
//        testClient.disconnect();
//        testServer.stop();
//
//        System.exit(0);
    }

}
