import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class UDPMulticastPriceListener implements Runnable {
    private final String multicastGroupIp;
    private final int multicastPort;
    private final String localClientIp;
    private MulticastSocket socket;
    private InetAddress group;
    private NetworkInterface localNetworkInterface;
    private volatile boolean running = true;
    private volatile boolean isSubscribedForDisplay = false;

    /**
     * Constructor for UDPMulticastPriceListener.
     *
     * @param multicastGroupIp The IP address of the multicast group.
     * @param multicastPort    The port for multicast communication.
     * @param localClientIp    The local IP address of the client to bind the socket to.
     */
    public UDPMulticastPriceListener(String multicastGroupIp, int multicastPort, String localClientIp) {
        this.multicastGroupIp = multicastGroupIp;
        this.multicastPort = multicastPort;
        this.localClientIp = localClientIp;
        System.out.println("DEBUG Multicast Listener: Inizializzazione con GroupIP=" + multicastGroupIp + ", Port=" + multicastPort + ", LocalIP=" + localClientIp);
    }

    /**
     * Sets the display status for price notifications.
     * If true, received multicast messages will be printed to console.
     *
     * @param status True to display notifications, false otherwise.
     */
    public void setSubscriptionDisplayStatus(boolean status) {
        this.isSubscribedForDisplay = status;
        System.out.println("DEBUG Multicast Listener: Stato visualizzazione sottoscrizione impostato a: " + status);
    }

    /**
     * Joins the multicast group. If the socket is closed or not initialized, it will be recreated.
     * The method attempts to join using the specified network interface if found, otherwise uses the default.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void joinMulticastGroup() throws IOException {
        if (socket == null || socket.isClosed()) {
            initializeSocketAndInterface();
        }

        if (localNetworkInterface != null) {
            socket.joinGroup(new InetSocketAddress(group, multicastPort), localNetworkInterface);
            System.out.println("Multicast Listener: Unito al gruppo " + multicastGroupIp + ":" + multicastPort + " su interfaccia: " + localNetworkInterface.getDisplayName());
            setSubscriptionDisplayStatus(true);
        } else {
            socket.joinGroup(group);
            System.out.println("Multicast Listener: Unito al gruppo " + multicastGroupIp + " su interfaccia predefinita.");
            setSubscriptionDisplayStatus(true);
        }
    }

    /**
     * Leaves the multicast group.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void leaveMulticastGroup() throws IOException {
        if (socket != null && !socket.isClosed() && group != null) {
            if (localNetworkInterface != null) {
                socket.leaveGroup(new InetSocketAddress(group, multicastPort), localNetworkInterface);
                System.out.println("Multicast Listener: Lasciato il gruppo " + multicastGroupIp + ":" + multicastPort + " su interfaccia: " + localNetworkInterface.getDisplayName());
            } else {
                socket.leaveGroup(group);
                System.out.println("Multicast Listener: Lasciato il gruppo " + multicastGroupIp + " su interfaccia predefinita.");
            }
            setSubscriptionDisplayStatus(false);
        }
    }

    /**
     * Helper method to initialize the MulticastSocket and identify the correct network interface
     * based on the local client IP.
     *
     * @throws IOException If an I/O error occurs during socket or interface initialization.
     */
    private void initializeSocketAndInterface() throws IOException {
        group = InetAddress.getByName(multicastGroupIp);
        socket = new MulticastSocket(multicastPort);

        System.out.println("DEBUG Multicast Listener: Socket legato all'indirizzo locale: " + socket.getLocalAddress() + ":" + socket.getLocalPort());

        if (localClientIp != null && !localClientIp.isEmpty()) {
            InetAddress clientAddr = InetAddress.getByName(localClientIp);
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.equals(clientAddr)) {
                        localNetworkInterface = ni;
                        break;
                    }
                }
                if (localNetworkInterface != null) {
                    break;
                }
            }

            if (localNetworkInterface != null) {
                socket.setNetworkInterface(localNetworkInterface);
                System.out.println("Multicast Listener: Interfaccia impostata su: " + localNetworkInterface.getDisplayName() + " (" + localClientIp + ")");
            } else {
                System.err.println("Multicast Listener: Attenzione: Interfaccia di rete non trovata per IP: " + localClientIp + ". Usando l'interfaccia predefinita.");
            }
        } else {
            System.out.println("Multicast Listener: Nessun IP locale specifico impostato, usando l'interfaccia predefinita.");
        }
    }

    @Override
    public void run() {
        try {
            initializeSocketAndInterface();

            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            System.out.println("Multicast Listener: In attesa di pacchetti sulla porta " + multicastPort + "...");

            while (running) {
                try {
                    socket.receive(packet);

                    System.out.println("DEBUG Multicast Listener: Pacchetto ricevuto da: " + packet.getAddress() + ":" + packet.getPort() + " Lunghezza: " + packet.getLength() + " byte.");

                    String received = new String(packet.getData(), 0, packet.getLength());

                    if (isSubscribedForDisplay) {
                        System.out.println("\n[*** NOTIFICA MULTICAST PREZZO RICEVUTA ***]: " + received);
                        System.out.println("DEBUG Multicast Listener: Contenuto del messaggio: \"" + received + "\"");
                    } else {
                        System.out.println("DEBUG Multicast Listener: Notifica multicast prezzo ricevuta ma non visualizzata (utente non iscritto alla visualizzazione).");
                    }

                    packet.setLength(buffer.length);

                } catch (SocketException e) {
                    if (running) {
                        System.err.println("ERRORE Multicast Listener Socket (inatteso, durante l'attesa): " + e.getMessage());
                    } else {
                        System.out.println("Multicast Listener: Socket chiuso come parte dello stop.");
                    }
                } catch (IOException e) {
                    System.err.println("ERRORE Multicast Listener I/O (durante la ricezione): " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("ERRORE CRITICO: Impossibile inizializzare MulticastSocket o interfaccia di rete: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Multicast Listener: Socket closed.");
            }
        }
    }

    /**
     * Stops the multicast listener thread. This will cause the socket to close and the run method to terminate.
     */
    public void stopListening() {
        running = false;
        System.out.println("Multicast Listener: Richiesta di stop ricevuta. Tentativo di chiudere il socket...");
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}