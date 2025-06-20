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
    private final String localClientIp; // IP locale del client
    private MulticastSocket socket;
    private InetAddress group;
    private NetworkInterface localNetworkInterface; // Interfaccia di rete da usare
    private volatile boolean running = true;
    private volatile boolean isSubscribedForDisplay = false; // Controlla se mostrare le notifiche

    public UDPMulticastPriceListener(String multicastGroupIp, int multicastPort, String localClientIp) {
        this.multicastGroupIp = multicastGroupIp;
        this.multicastPort = multicastPort;
        this.localClientIp = localClientIp;
        System.out.println("DEBUG Multicast Listener: Inizializzazione con GroupIP=" + multicastGroupIp + ", Port=" + multicastPort + ", LocalIP=" + localClientIp);
    }

    // Metodo per impostare lo stato di visualizzazione delle notifiche
    public void setSubscriptionDisplayStatus(boolean status) {
        this.isSubscribedForDisplay = status;
        System.out.println("DEBUG Multicast Listener: Stato visualizzazione sottoscrizione impostato a: " + status);
    }

    // Metodo per unirsi al gruppo multicast a livello di rete
    public void joinMulticastGroup() throws IOException {
        if (socket == null || socket.isClosed()) {
            // Se il socket non è stato ancora creato o è stato chiuso, ricrealo
            // Questo potrebbe accadere se stopListening() è stato chiamato e poi si tenta di ri-sottoscrivere
            initializeSocketAndInterface();
        }

        if (localNetworkInterface != null) {
            socket.joinGroup(new InetSocketAddress(group, multicastPort), localNetworkInterface);
            System.out.println("Multicast Listener: Unito al gruppo " + multicastGroupIp + ":" + multicastPort + " su interfaccia: " + localNetworkInterface.getDisplayName());
            setSubscriptionDisplayStatus(true); // Una volta unito, abilita la visualizzazione
        } else {
            // Fallback se l'interfaccia specifica non è stata trovata
            socket.joinGroup(group); // Questo usa l'interfaccia predefinita del socket
            System.out.println("Multicast Listener: Unito al gruppo " + multicastGroupIp + " su interfaccia predefinita.");
            setSubscriptionDisplayStatus(true); // Una volta unito, abilita la visualizzazione
        }
    }

    // Metodo per lasciare il gruppo multicast a livello di rete
    public void leaveMulticastGroup() throws IOException {
        if (socket != null && !socket.isClosed() && group != null) {
            // Tenta di lasciare il gruppo solo se l'interfaccia è nota
            if (localNetworkInterface != null) {
                socket.leaveGroup(new InetSocketAddress(group, multicastPort), localNetworkInterface);
                System.out.println("Multicast Listener: Lasciato il gruppo " + multicastGroupIp + ":" + multicastPort + " su interfaccia: " + localNetworkInterface.getDisplayName());
            } else {
                socket.leaveGroup(group); // Se era unito con interfaccia predefinita
                System.out.println("Multicast Listener: Lasciato il gruppo " + multicastGroupIp + " su interfaccia predefinita.");
            }
            setSubscriptionDisplayStatus(false); // Disabilita la visualizzazione dopo aver lasciato il gruppo
        }
    }

    // Metodo helper per inizializzare il socket e l'interfaccia di rete
    private void initializeSocketAndInterface() throws IOException {
        group = InetAddress.getByName(multicastGroupIp);
        socket = new MulticastSocket(multicastPort); // Lega il socket alla porta multicast

        System.out.println("DEBUG Multicast Listener: Socket legato all'indirizzo locale: " + socket.getLocalAddress() + ":" + socket.getLocalPort());

        // Trova l'interfaccia di rete corretta
        if (localClientIp != null && !localClientIp.isEmpty()) {
            InetAddress clientAddr = InetAddress.getByName(localClientIp);
            // Cerca l'interfaccia che contiene l'indirizzo IP del client
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
            // Inizializza il socket e l'interfaccia all'avvio del thread
            // Ma NON si unisce ancora al gruppo multicast qui
            initializeSocketAndInterface();

            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // In attesa di pacchetti (bloccante), anche se non ancora unito al gruppo multicast
            // Questo listener è attivo, ma riceverà pacchetti multicast solo dopo joinMulticastGroup()
            System.out.println("Multicast Listener: In attesa di pacchetti sulla porta " + multicastPort + "...");

            while (running) {
                try {
                    socket.receive(packet); // Questa è la chiamata bloccante

                    System.out.println("DEBUG Multicast Listener: Pacchetto ricevuto da: " + packet.getAddress() + ":" + packet.getPort() + " Lunghezza: " + packet.getLength() + " byte.");

                    String received = new String(packet.getData(), 0, packet.getLength());

                    // *** FILTRA LA VISUALIZZAZIONE QUI ***
                    if (isSubscribedForDisplay) {
                        System.out.println("\n[*** NOTIFICA MULTICAST PREZZO RICEVUTA ***]: " + received);
                        System.out.println("DEBUG Multicast Listener: Contenuto del messaggio: \"" + received + "\"");
                    } else {
                        System.out.println("DEBUG Multicast Listener: Notifica multicast prezzo ricevuta ma non visualizzata (utente non iscritto alla visualizzazione).");
                    }
                    // **********************************

                    packet.setLength(buffer.length); // Reimposta la lunghezza per il prossimo pacchetto

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
            // Lascia il gruppo e chiudi il socket solo quando il listener viene effettivamente fermato
            if (socket != null && !socket.isClosed()) {
                try {
                    // Tenta di lasciare il gruppo prima di chiudere il socket
                    if (group != null) {
                        // Verifica se eravamo uniti al gruppo prima di tentare di lasciarlo
                        if (localNetworkInterface != null) {
                            socket.leaveGroup(new InetSocketAddress(group, multicastPort), localNetworkInterface);
                            System.out.println("Multicast Listener: Lasciato il gruppo " + multicastGroupIp + " su interfaccia: " + localNetworkInterface.getDisplayName());
                        } else {
                            socket.leaveGroup(group); // Se era unito con interfaccia predefinita
                            System.out.println("Multicast Listener: Lasciato il gruppo " + multicastGroupIp + " su interfaccia predefinita.");
                        }
                    }
                    socket.close();
                    System.out.println("Multicast Listener: Socket chiuso.");
                } catch (IOException e) {
                    System.err.println("Errore chiusura socket multicast: " + e.getMessage());
                }
            }
        }
    }

    public void stopListening() {
        running = false;
        System.out.println("Multicast Listener: Richiesta di stop ricevuta. Tentativo di chiudere il socket...");
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Questo sbloccherà il socket.receive()
        }
    }
}