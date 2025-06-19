import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPClientListener implements Runnable {
    private final int udpPort;
    private DatagramSocket socket;
    private volatile boolean running = true;

    public UDPClientListener(int udpPort) {
        this.udpPort = udpPort;
    }

    @Override
    public void run() {
        try {
            this.socket = new DatagramSocket(udpPort);
            //System.out.println("UDP Listener started on port " + udpPort + "...");

            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (running) {
                try {
                    socket.receive(packet);

                    String received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("\n[UDP Notification]: " + received);

                    packet.setLength(buffer.length);

                } catch (SocketException e) {
                    if (running) {
                        System.err.println("UDP Listener Socket error (unexpected): " + e.getMessage());
                    }
                } catch (IOException e) {
                    System.err.println("UDP Listener I/O error: " + e.getMessage());
                }
            }

        } catch (SocketException e) {
            System.err.println("Could not bind UDP socket to port " + udpPort + ": " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("UDP Listener on port " + udpPort + " stopped.");
            }
        }
    }

    public void stopListening() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}