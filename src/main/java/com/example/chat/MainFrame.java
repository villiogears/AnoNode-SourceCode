package com.example.chat;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.security.SecureRandom;

public class MainFrame extends JFrame {

    private String myRoomCode = "";
    private final JTextField roomCodeField = createRetroTextField(16);
    private final JTextArea chatArea = new JTextArea();
    private final JTextField messageField = createRetroTextField(20);
    private final JLabel statusLabel = new JLabel("STATUS: DISCONNECTED");

    private MqttSignalingClient mqttClient;

    private static final Font RETRO_FONT = new Font("Monospaced", Font.BOLD, 14);
    private static final Color COLOR_BG = new Color(15, 15, 20);
    private static final Color COLOR_PANEL = new Color(30, 30, 40);
    private static final Color COLOR_TEXT = new Color(255, 255, 255);
    private static final Color COLOR_GREEN = new Color(85, 255, 85);
    private static final Color COLOR_BORDER = new Color(85, 85, 85);

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public MainFrame() {
        setTitle("==== AnoNode ====");
        setSize(720, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout(10, 10));

        // --- 上部パネル ---
        JPanel topContainer = new JPanel(new BorderLayout(5, 5));
        topContainer.setBackground(COLOR_BG);

        statusLabel.setFont(RETRO_FONT);
        statusLabel.setForeground(COLOR_GREEN);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        topContainer.add(statusLabel, BorderLayout.NORTH);

        JPanel targetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        targetPanel.setBackground(COLOR_PANEL);
        targetPanel.setBorder(new LineBorder(COLOR_BORDER, 2));

        JLabel codeLabel = new JLabel("ROOM CODE:");
        codeLabel.setFont(RETRO_FONT);
        codeLabel.setForeground(COLOR_TEXT);
        targetPanel.add(codeLabel);
        targetPanel.add(roomCodeField);

        JButton connectBtn = createRetroButton("JOIN ROOM");
        JButton copyBtn = createRetroButton("COPY CODE");
        JButton genBtn = createRetroButton("NEW ROOM");

        targetPanel.add(connectBtn);
        targetPanel.add(copyBtn);
        targetPanel.add(genBtn);

        topContainer.add(targetPanel, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        // --- 中央: チャット表示エリア ---
        chatArea.setEditable(false);
        chatArea.setBackground(COLOR_BG);
        chatArea.setForeground(COLOR_TEXT);
        chatArea.setFont(RETRO_FONT);
        chatArea.setLineWrap(true);
        chatArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(new LineBorder(COLOR_BORDER, 2));
        scrollPane.getViewport().setBackground(COLOR_BG);
        add(scrollPane, BorderLayout.CENTER);

        // --- 下部: メッセージ入力エリア ---
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setBackground(COLOR_BG);

        JButton sendBtn = createRetroButton("SEND");
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- イベント処理 ---
        connectBtn.addActionListener(e -> {
            String code = roomCodeField.getText().trim();
            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(this, "ROOM CODEを入力してください", "ERROR", JOptionPane.ERROR_MESSAGE);
                return;
            }
            joinRoom(code);
        });

        copyBtn.addActionListener(e -> {
            String code = roomCodeField.getText().trim();
            if (!code.isEmpty()) {
                StringSelection selection = new StringSelection(code);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                appendLog("[SYSTEM] ROOM CODE をクリップボードにコピーしました！");
            } else {
                JOptionPane.showMessageDialog(this, "コピーするコードがありません", "ERROR", JOptionPane.WARNING_MESSAGE);
            }
        });

        genBtn.addActionListener(e -> {
            String randomCode = generateUniqueRoomCode();
            roomCodeField.setText(randomCode);
            joinRoom(randomCode);
        });

        sendBtn.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 初期生成
        myRoomCode = generateUniqueRoomCode();
        roomCodeField.setText(myRoomCode);
    }

    private static String generateUniqueRoomCode() {
        StringBuilder sb = new StringBuilder("ROOM-");
        for (int i = 0; i < 8; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private void joinRoom(String roomId) {
        if (mqttClient != null) {
            mqttClient.disconnect();
        }

        statusLabel.setText("STATUS: CONNECTING TO MQTT BROKER...");
        mqttClient = new MqttSignalingClient();

        new Thread(() -> {
            try {
                mqttClient.connectAndSubscribe(roomId, (sender, msg) -> {
                    SwingUtilities.invokeLater(() -> appendLog("< " + msg));
                });

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("STATUS: ONLINE (ROOM: " + roomId + ")");
                    appendLog("* AnoNode ネットワーク経由で部屋 [" + roomId + "] に接続しました。");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("STATUS: ERROR");
                    statusLabel.setForeground(Color.RED);
                    appendLog("! 接続エラー: " + e.getMessage());
                });
            }
        }).start();
    }

    private static JTextField createRetroTextField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(RETRO_FONT);
        tf.setBackground(COLOR_BG);
        tf.setForeground(COLOR_GREEN);
        tf.setCaretColor(COLOR_GREEN);
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 2),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        return tf;
    }

    private static JButton createRetroButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(RETRO_FONT);
        btn.setBackground(COLOR_PANEL);
        btn.setForeground(COLOR_TEXT);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_TEXT, 2),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        return btn;
    }

    private void sendMessage() {
        String msg = messageField.getText().trim();
        if (!msg.isEmpty() && mqttClient != null) {
            try {
                mqttClient.sendMessage(msg);
                appendLog("> " + msg);
                messageField.setText("");
            } catch (Exception e) {
                appendLog("! 送信エラー: " + e.getMessage());
            }
        }
    }

    private void appendLog(String text) {
        chatArea.append(text + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
