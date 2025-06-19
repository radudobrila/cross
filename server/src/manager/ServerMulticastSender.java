package manager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ServerMulticastSender implements Runnable {
    private final String multicastGroupIp;
    private final int multicastPort;
    private InetAddress group;
    private MulticastSocket socket;
    private final String localIpAddress;

    public ServerMulticastSender(String multicastGroupIp, int multicastPort, String localIpAddress) {
        this.multicastGroupIp = multicastGroupIp;
        this.multicastPort = multicastPort;
        this.localIpAddress = localIpAddress;
        try {
            group = InetAddress.getByName(multicastGroupIp);
            socket = new MulticastSocket();

            if (localIpAddress != null && !localIpAddress.isEmpty()) {
                socket.setInterface(InetAddress.getByName(localIpAddress));
            }

            socket.setTimeToLive(1);
            System.out.println("ServerMulticastSender: Inizializzato per gruppo " + multicastGroupIp + ":" + multicastPort);
        } catch (IOException e) {
            System.err.println("Error initializing ServerMulticastSender: " + e.getMessage());
            throw new RuntimeException("Impossibile configurare socket Multicast: " + e.getMessage(), e);
        }
    }

    @Override
    public void run() {
        System.out.println("ServerMulticastSender: Thread (se avviato) in attesa di chiamate per invio.");
    }

    public void sendPriceUpdate(double price) {
        if (socket == null || socket.isClosed()) {
            System.err.println("ServerMulticastSender: Socket is not initialized or is closed. Cannot send price update.");
            return;
        }

        try {
            String message = "BTC Price Update: " + String.format("%.2f", price);
            byte[] buffer = message.getBytes();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, multicastPort);
            socket.send(packet);
            System.out.println("SERVER: Sent multicast BTC price update: \"" + message + "\" to " + multicastGroupIp + ":" + multicastPort);

        } catch (IOException e) {
            System.err.println("Error sending multicast price update: " + e.getMessage());
        }
    }

    public void stopSending() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                System.out.println("ServerMulticastSender: Socket closed.");
            } catch (Exception e) {
                System.err.println("Error closing multicast socket: " + e.getMessage());
            }
        }
    }
}