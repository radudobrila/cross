import RMI.Register;
import RMI.RegisterInterface;
import manager.OrderManager;
import manager.TCPServer;
import manager.UdpSessionManager;
import orderBook.OrderBook;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerMain {
    private static final Set<String> loggedUsers = ConcurrentHashMap.newKeySet();

    private static final int SERVER_PORT = 1234;
    private static final LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private static final ExecutorService threadPool = new ThreadPoolExecutor(4, 10, 60L, TimeUnit.SECONDS, taskQueue);

    public static void main(String[] args) {
        System.out.println("Server starting...");

        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            RegisterInterface register = new Register();
            registry.rebind("RegisterService", register);
            System.out.println("Servizio RMI RegisterService avviato e legato.");
        } catch (Exception e) {
            System.err.println("Impossibile avviare il registro RMI o legare il servizio:");
            e.printStackTrace();
            System.exit(1);
        }

        String localServerIp = null;
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (!ni.isLoopback() && ni.isUp() && !ni.isVirtual()) {
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                            localServerIp = addr.getHostAddress();
                            System.out.println("Trovato IP valido per multicast: " + localServerIp + " su interfaccia: " + ni.getDisplayName());
                            break;
                        }
                    }
                }
                if (localServerIp != null) {
                    break;
                }
            }

            if (localServerIp == null) {
                localServerIp = InetAddress.getLocalHost().getHostAddress();
                System.out.println("Fallback: Usando InetAddress.getLocalHost().getHostAddress(): " + localServerIp);
            }

            System.out.println("Server avviato sull'IP locale: " + localServerIp);
        } catch (SocketException | UnknownHostException e) {
            System.err.println("Impossibile determinare l'IP locale del server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        if (localServerIp != null) {
            UdpSessionManager.setLocalServerIpForMulticast(localServerIp);
            System.out.println("UdpSessionManager configurato con IP locale: " + localServerIp);
        } else {
            System.err.println("Attenzione: IP locale del server non disponibile. Il multicast potrebbe non funzionare correttamente.");
        }

        OrderBook orderBook = new OrderBook();
        System.out.println("OrderBook inizializzato e ordini esistenti caricati.");

        OrderManager orderManager = new OrderManager(orderBook);

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server TCP avviato sulla porta " + SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso: " + clientSocket.getRemoteSocketAddress());

                threadPool.submit(new TCPServer(clientSocket, loggedUsers, orderManager));
            }
        } catch (IOException e) {
            System.err.println("Errore del Server TCP:");
            e.printStackTrace();
            return;
        }
    }
}