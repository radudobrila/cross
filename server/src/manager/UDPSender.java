package manager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSender {

    /**
     * Sends an unicast UDP message to a specified IP address and port.
     * This method creates and closes a DatagramSocket for each send operation.
     *
     * @param ipAddress The IP address of the recipient.
     * @param port      The UDP port of the recipient.
     * @param message   The message string to send.
     * @throws IOException If an I/O error occurs during the send operation.
     */
    public static void sendUnicast(String ipAddress, int port, String message) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress clientAddress = InetAddress.getByName(ipAddress);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, clientAddress, port);
            socket.send(packet);
        }
    }
}