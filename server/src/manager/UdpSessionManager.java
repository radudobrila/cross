package manager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public class UdpSessionManager {

    private static final Map<String, InetSocketAddress> loggedUserUdpInfo = new ConcurrentHashMap<>();
    private static final Set<String> multicastSubscribers = ConcurrentHashMap.newKeySet();

    private static String localServerIpForMulticast;

    private static final String MULTICAST_GROUP_IP = "230.0.0.1";
    private static final int MULTICAST_BTC_PRICE_PORT = 5000;

    public static void setLocalServerIpForMulticast(String ipAddress) {
        localServerIpForMulticast = ipAddress;
        System.out.println("UdpSessionManager configurato con IP locale: " + localServerIpForMulticast);
    }

    public static void registerClientUdpInfo(String username, String clientIp, int udpPort) {
        try {
            InetAddress clientAddress = InetAddress.getByName(clientIp);
            InetSocketAddress socketAddress = new InetSocketAddress(clientAddress, udpPort);
            loggedUserUdpInfo.put(username, socketAddress);
            System.out.println("UdpSessionManager: Registered user " + username + " for unicast on " + clientIp + ":" + udpPort);
        } catch (Exception e) {
            System.err.println("UdpSessionManager: Error registering UDP info for " + username + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void removeClientUdpInfo(String username) {
        loggedUserUdpInfo.remove(username);
        multicastSubscribers.remove(username);
        System.out.println("UdpSessionManager: Removed UDP info for user " + username);
    }

    public static int handleMulticastInterest(String username, int clientMulticastPort) {
        if (!loggedUserUdpInfo.containsKey(username)) {
            System.out.println("UdpSessionManager: User " + username + " not found in loggedUserUdpInfo for multicast notification. Code 101.");
            return 101;
        }
        if (multicastSubscribers.contains(username)) {
            System.out.println("UdpSessionManager: User " + username + " already subscribed to multicast notifications. Code 102.");
            return 102;
        }

        multicastSubscribers.add(username);
        System.out.println("UdpSessionManager: User " + username + " subscribed to multicast notifications on port " + clientMulticastPort + ". Code 100.");
        return 100;
    }

    public static void notifyTradeExecution(String username, String message) {
        InetSocketAddress userAddress = loggedUserUdpInfo.get(username);
        if (userAddress != null) {
            try (DatagramSocket socket = new DatagramSocket()) {
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, userAddress.getAddress(), userAddress.getPort());
                socket.send(packet);
                System.out.println("UdpSessionManager: Sent unicast notification to " + username + " at " + userAddress.getAddress().getHostAddress() + ":" + userAddress.getPort() + " - Message: \"" + message + "\"");
            } catch (Exception e) {
                System.err.println("UdpSessionManager: Error sending unicast notification to " + username + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("UdpSessionManager: User " + username + " not found for unicast notification (not logged in or UDP info not registered).");
        }
    }

    public static void sendBtcPriceMulticast(int currentPrice) {
        String message = String.format("BTC Price Update: %d", currentPrice);
        try (MulticastSocket socket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP_IP);
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, group, MULTICAST_BTC_PRICE_PORT);

            socket.send(packet);
            System.out.println("UdpSessionManager: Sent multicast BTC price update: \"" + message + "\" to " + MULTICAST_GROUP_IP + ":" + MULTICAST_BTC_PRICE_PORT);
        } catch (Exception e) {
            System.err.println("UdpSessionManager: Error sending multicast BTC price update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static InetSocketAddress getClientUdpInfo(String username) {
        return loggedUserUdpInfo.get(username);
    }
}