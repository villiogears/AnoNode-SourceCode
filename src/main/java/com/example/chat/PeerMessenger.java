package com.example.chat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class PeerMessenger {

    public interface MessageListener {
        void onMessageReceived(String sender, String message);
    }

    private final DatagramSocket socket;
    private InetSocketAddress targetAddress;

    public PeerMessenger(DatagramSocket socket) {
        this.socket = socket;
    }

    public void setTarget(String ip, int port) {
        this.targetAddress = new InetSocketAddress(ip, port);
    }

    public void sendHolePunch() throws IOException {
        if (targetAddress == null) return;
        byte[] data = "HOLE_PUNCH".getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress);
        socket.send(packet);
    }

    public void sendMessage(String message) throws IOException {
        if (targetAddress == null) {
            throw new IOException("接続相手のIPとポートが設定されていません。");
        }
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress);
        socket.send(packet);
    }

    public void startListening(MessageListener listener) {
        byte[] buffer = new byte[2048];
        while (!socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());
                String sender = packet.getAddress().getHostAddress() + ":" + packet.getPort();

                // ホールパンチ用の特殊メッセージは無視
                if ("HOLE_PUNCH".equals(msg)) {
                    continue;
                }

                listener.onMessageReceived(sender, msg);
            } catch (IOException e) {
                if (socket.isClosed()) break;
                e.printStackTrace();
            }
        }
    }
}
