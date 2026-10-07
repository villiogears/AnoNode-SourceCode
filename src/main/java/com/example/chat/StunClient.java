package com.example.chat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class StunClient {

    public static class StunResult {
        public final String publicIp;
        public final int publicPort;

        public StunResult(String publicIp, int publicPort) {
            this.publicIp = publicIp;
            this.publicPort = publicPort;
        }
    }

    public static StunResult getPublicAddress(DatagramSocket socket) throws IOException {
        String stunServer = "stun.cloudflare.com";
        int stunPort = 3478;

        InetAddress address = InetAddress.getByName(stunServer);

        // Binding Request (20バイト)
        byte[] request = new byte[20];
        request[0] = 0x00; // Message Type: Binding Request (0x0001)
        request[1] = 0x01;
        request[2] = 0x00; // Message Length: 0
        request[3] = 0x00;
        // Magic Cookie: 0x2112A442
        request[4] = 0x21;
        request[5] = 0x12;
        request[6] = (byte) 0xA4;
        request[7] = 0x42;
        // Transaction ID (12バイトのダミーデータ)
        for (int i = 8; i < 20; i++) {
            request[i] = (byte) i;
        }

        DatagramPacket sendPacket = new DatagramPacket(request, request.length, address, stunPort);
        socket.setSoTimeout(3000); // 3秒タイムアウト
        socket.send(sendPacket);

        byte[] response = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(response, response.length);
        socket.receive(receivePacket);

        // STUNレスポンスの解析 (XOR-MAPPED-ADDRESS: 0x0020 または MAPPED-ADDRESS: 0x0001)
        int offset = 20; // ヘッダーをスキップ
        int length = receivePacket.getLength();

        while (offset + 4 <= length) {
            int attrType = ((response[offset] & 0xFF) << 8) | (response[offset + 1] & 0xFF);
            int attrLen = ((response[offset + 2] & 0xFF) << 8) | (response[offset + 3] & 0xFF);

            if (attrType == 0x0020) { // XOR-MAPPED-ADDRESS
                int port = (((response[offset + 6] & 0xFF) << 8) | (response[offset + 7] & 0xFF)) ^ 0x2112;
                String ip = String.format("%d.%d.%d.%d",
                        (response[offset + 8] ^ 0x21) & 0xFF,
                        (response[offset + 9] ^ 0x12) & 0xFF,
                        (response[offset + 10] ^ 0xA4) & 0xFF,
                        (response[offset + 11] ^ 0x42) & 0xFF);
                return new StunResult(ip, port);
            } else if (attrType == 0x0001) { // MAPPED-ADDRESS
                int port = ((response[offset + 6] & 0xFF) << 8) | (response[offset + 7] & 0xFF);
                String ip = String.format("%d.%d.%d.%d",
                        response[offset + 8] & 0xFF,
                        response[offset + 9] & 0xFF,
                        response[offset + 10] & 0xFF,
                        response[offset + 11] & 0xFF);
                return new StunResult(ip, port);
            }

            offset += 4 + attrLen;
            if (attrLen % 4 != 0) {
                offset += (4 - (attrLen % 4)); // パディング調整
            }
        }

        throw new IOException("STUNレスポンスの解析に失敗しました。");
    }
}
