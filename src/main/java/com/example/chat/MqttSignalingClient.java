package com.example.chat;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;

public class MqttSignalingClient {

    private static final String BROKER_URL = "tcp://broker.hivemq.com:1883";
    private MqttClient client;
    private String currentTopic;
    private String clientId;

    public interface MessageListener {
        void onMessageReceived(String senderId, String message);
    }

    public void connectAndSubscribe(String roomId, MessageListener listener) throws Exception {
        this.clientId = "JavaPeer_" + System.currentTimeMillis();
        client = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(10);

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {}

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                // フォーマット: SenderID:Content
                int colonIdx = payload.indexOf(":");
                if (colonIdx > 0) {
                    String sender = payload.substring(0, colonIdx);
                    String content = payload.substring(colonIdx + 1);
                    // 自分の送信したメッセージは二重表示を防ぐため除外
                    if (!sender.equals(clientId)) {
                        listener.onMessageReceived(sender, content);
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

        client.connect(options);
        this.currentTopic = "p2p-chat-app/rooms/" + roomId;
        client.subscribe(this.currentTopic, 1);
    }

    public void sendMessage(String content) throws Exception {
        if (client != null && client.isConnected()) {
            String payload = clientId + ":" + content;
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish(this.currentTopic, message);
        }
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception ignored) {}
    }

    public String getClientId() {
        return clientId;
    }
}
