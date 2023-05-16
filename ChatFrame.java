package dubatovka;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.net.UnknownHostException;

public class ChatFrame extends JFrame implements ActionListener, KeyListener {
    private JTextArea chatArea;
    private JTextField messageField;
    private JTextField ipField;
    private JTextField senderField;
    private DatagramSocket socket;
    private DatagramPacket receivePacket;

    public ChatFrame() {
        super("Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(400, 300));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(chatArea, BorderLayout.CENTER);

        messageField = new JTextField();
        messageField.addActionListener(this);
        messageField.addKeyListener(this);
        add(messageField, BorderLayout.SOUTH);

        ipField = new JTextField("127.0.0.1");
        senderField = new JTextField("Me");

        JPanel infoPanel = new JPanel();
        infoPanel.add(new JLabel("IP Address:"));
        infoPanel.add(ipField);
        infoPanel.add(new JLabel("Sender Name:"));
        infoPanel.add(senderField);
        add(infoPanel, BorderLayout.NORTH);

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(this);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendButton);
        add(buttonPanel, BorderLayout.EAST);

        pack();
        setVisible(true);

        // создание сокета на отправку и получение сообщений
        try {
            socket = new DatagramSocket();
            Thread thread = new Thread(new ReceiveThread());
            thread.start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        sendMessage();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()) {
            sendMessage();
        }
    }

    private void sendMessage() {
        String message = messageField.getText();
        String ipAddress = ipField.getText();
        String senderName = senderField.getText();
        String recipientIp = ipField.getText();
        if (!message.isEmpty()) {
            messageField.setText("");
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            String time = dateFormat.format(date);

            // проверка IP-адреса
            boolean isIpAddressValid = false;
            try {
                InetAddress address = InetAddress.getByName(ipAddress);
                isIpAddressValid = address.isReachable(3000); // ожидание ответа от сервера в течение 3 секунд
            } catch (UnknownHostException e1) {
                JOptionPane.showMessageDialog(this, "Error: invalid IP address");
                return;
            } catch (IOException e2) {
                JOptionPane.showMessageDialog(this, "Error: server is unavailable");
                return;
            }

            if (!isIpAddressValid) {
                JOptionPane.showMessageDialog(this, "Error: server is unavailable");
                return;
            }

            chatArea.append(time + " " + senderName + ": " + message + "\n");
            try {
                byte[] sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ipAddress), 1234);
                socket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        ChatFrame chatFrame = new ChatFrame();
    }

    // поток для приема сообщений
    private class ReceiveThread implements Runnable {
        byte[] receiveData = new byte[1024];

        @Override
        public void run() {
            try {
                while (true) {
                    receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    String ipAddress = receivePacket.getAddress().getHostAddress();
                    String senderName = "Anonymous";
                    if (receivePacket.getPort() == 1234) {
                        // если сообщение пришло на порт 1234, то определяем отправителя по IP-адресу
                        senderName = ipAddress;
                    }
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                    Date date = new Date();
                    String time = dateFormat.format(date);
                    chatArea.append(time + " " + senderName + " (" + ipAddress + "): " + message + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

