import RMI.RegisterInterface;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ClientMain {
    private static final String MULTICAST_GROUP_IP = "230.0.0.1";
    private static final int MULTICAST_PRICE_PORT = 5000;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        TCPClient client = new TCPClient();
        boolean isLogged = false;
        String actualUsername = "";

        ExecutorService udpUnicastExecutor = null;
        Future<?> udpUnicastListenerFuture = null;
        UDPClientListener udpUnicastListener = null;

        ExecutorService udpMulticastExecutor = null;
        Future<?> udpMulticastListenerFuture = null;
        UDPMulticastPriceListener udpMulticastPriceListener = null;

        String localClientIp = null;
        int clientUdpUnicastPort = -1;

        try {
            try {
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (networkInterfaces.hasMoreElements()) {
                    NetworkInterface ni = networkInterfaces.nextElement();
                    if (!ni.isLoopback() && ni.isUp() && !ni.isVirtual()) {
                        Enumeration<InetAddress> addresses = ni.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress addr = addresses.nextElement();
                            if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                                localClientIp = addr.getHostAddress();
                                System.out.println("Client: Found valid local IP for multicast: " + localClientIp + " on interface: " + ni.getDisplayName());
                                break;
                            }
                        }
                    }
                    if (localClientIp != null) {
                        break;
                    }
                }

                if (localClientIp == null) {
                    localClientIp = InetAddress.getLocalHost().getHostAddress();
                    System.out.println("Client: Fallback - Using InetAddress.getLocalHost().getHostAddress(): " + localClientIp);
                }
                System.out.println("Client IP address for UDP Unicast and Multicast: " + localClientIp);

            } catch (SocketException | UnknownHostException e) {
                System.err.println("Client: Error determining local IP: " + e.getMessage());
            }

            client.connect();
            System.out.println("Choose operation:");

            try (DatagramSocket tempSocket = new DatagramSocket()) {
                clientUdpUnicastPort = tempSocket.getLocalPort();
            } catch (SocketException e) {
                System.err.println("Could not obtain an ephemeral UDP port: " + e.getMessage());
                return;
            }

            udpUnicastExecutor = Executors.newSingleThreadExecutor();
            udpUnicastListener = new UDPClientListener(clientUdpUnicastPort);
            udpUnicastListenerFuture = udpUnicastExecutor.submit(udpUnicastListener);

            udpMulticastExecutor = Executors.newSingleThreadExecutor();
            udpMulticastPriceListener = new UDPMulticastPriceListener(MULTICAST_GROUP_IP, MULTICAST_PRICE_PORT, localClientIp);
            udpMulticastListenerFuture = udpMulticastExecutor.submit(udpMulticastPriceListener);

            while (true) {
                if (!isLogged) {
                    System.out.println("1 - Register");
                    System.out.println("2 - Sign In");
                    System.out.println("3 - Update password");
                    System.out.println("4 - Exit \n> ");
                    String choiceStr = scanner.nextLine().trim();
                    int choice = -1;
                    try {
                        choice = Integer.parseInt(choiceStr);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a number.");
                        continue;
                    }

                    switch (choice) {
                        case 1:
                            System.out.println("Enter username: ");
                            String username = scanner.nextLine();
                            System.out.println("Enter password: ");
                            String password = scanner.nextLine();

                            try {
                                RegisterInterface stub = (RegisterInterface) Naming.lookup("rmi://localhost:1099/RegisterService");
                                int result = stub.Register(username, password);

                                if (result == 100) {
                                    System.out.println("Register successful.");
                                } else if (result == 101) {
                                    System.out.println("Invalid password (min 2 characters).");
                                } else if (result == 102) {
                                    System.out.println("Username already in use.");
                                } else {
                                    System.out.println("Unknown error.");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case 2:
                            System.out.println("Enter username: ");
                            username = scanner.nextLine();
                            System.out.println("Enter password: ");
                            password = scanner.nextLine();

                            isLogged = client.sendLogin(username, password, localClientIp, clientUdpUnicastPort);
                            actualUsername = username;

                            if (isLogged) {
                                System.out.print("Do you want to receive BTC price notifications? (1-Yes/0-No): ");
                                String priceNotifChoiceStr = scanner.nextLine().trim();

                                try {
                                    int priceNotifChoice = Integer.parseInt(priceNotifChoiceStr);

                                    if (priceNotifChoice == 1) {
                                        client.sendRegisterPriceInterest(actualUsername, MULTICAST_PRICE_PORT);
                                        udpMulticastPriceListener.joinMulticastGroup();
                                        System.out.println("You are now subscribed to BTC price notifications (via Multicast).");
                                    } else if (priceNotifChoice == 0) {
                                        System.out.println("Skipping BTC price notifications setup.");
                                        udpMulticastPriceListener.setSubscriptionDisplayStatus(false);
                                    } else {
                                        System.out.println("Invalid input. Please enter 1 or 0. Skipping BTC price notifications setup.");
                                        udpMulticastPriceListener.setSubscriptionDisplayStatus(false);
                                    }
                                } catch (IOException e) {
                                    System.err.println("I/O Error during multicast subscription: " + e.getMessage());
                                    udpMulticastPriceListener.setSubscriptionDisplayStatus(false);
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid input. Please enter 1 or 0. Skipping BTC price notifications setup.");
                                    udpMulticastPriceListener.setSubscriptionDisplayStatus(false);
                                }
                            }
                            break;
                        case 3:
                            System.out.println("Enter username: ");
                            username = scanner.nextLine();
                            System.out.println("Enter password: ");
                            password = scanner.nextLine();
                            System.out.println("Enter new password: ");
                            String newPassword = scanner.nextLine();

                            client.sendUpdateCredentials(username, password, newPassword);
                            break;
                        case 4:
                            System.out.println("Exiting...");
                            break;
                        default:
                            System.out.println("Invalid choice. Please select a valid option (1-4).");
                            break;
                    }
                    if (choice == 4) {
                        break;
                    }
                } else {
                    System.out.println("1 - New Market Order");
                    System.out.println("2 - New Limit Order");
                    System.out.println("3 - New Stop Order");
                    System.out.println("4 - View your orders");
                    System.out.println("5 - Cancel a Order");
                    System.out.println("6 - Print Price History");
                    System.out.println("7 - Log out \n> ");
                    String choiceStr = scanner.nextLine().trim();
                    int choice = -1;
                    try {
                        choice = Integer.parseInt(choiceStr);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a number.");
                        continue;
                    }

                    switch (choice) {
                        case 1:
                            System.out.println("0 - ASK \n1 - BID: ");
                            int ask = Integer.parseInt(scanner.nextLine());
                            System.out.println("Quantity: ");
                            int quantity = Integer.parseInt(scanner.nextLine());

                            client.sendMarketOrder(actualUsername, ask, quantity);
                            break;
                        case 2:
                            System.out.println("0 - ASK \n1 - BID: ");
                            int askBid = Integer.parseInt(scanner.nextLine());
                            System.out.println("Quantity: ");
                            int size = Integer.parseInt(scanner.nextLine());
                            System.out.println("Price: ");
                            int price = Integer.parseInt(scanner.nextLine());
                            client.sendLimitOrder(actualUsername, askBid, size, price);
                            break;
                        case 3:
                            System.out.println("0 - ASK \n1 - BID: ");
                            int askBidStop = Integer.parseInt(scanner.nextLine());
                            System.out.println("Size: ");
                            int sizeStop = Integer.parseInt(scanner.nextLine());
                            System.out.println("Price: ");
                            int priceStop = Integer.parseInt(scanner.nextLine());
                            client.sendStopOrder(actualUsername, askBidStop, sizeStop, priceStop);
                            break;
                        case 4:
                            System.out.println("Your Active Orders: ");
                            client.sendPrintOrders(actualUsername);
                            break;
                        case 5:
                            System.out.println("Which order do you want to cancel: ");
                            client.sendPrintOrders(actualUsername);
                            int orderID = Integer.parseInt(scanner.nextLine());
                            client.sendCancelOrder(actualUsername, orderID);
                            break;
                        case 6:
                            System.out.println("Which month do you want the price history\n1 - January\n2 - February\n" +
                                    "3 - March\n 4 - April\n5 - May\n6 - June\n7 - July\n8 - August\n9 - September\n10 - October\n" +
                                    "11 - November\n12 - December\n>  ");
                            int month = Integer.parseInt(scanner.nextLine());
                            client.sendPriceHistory(month, actualUsername);
                            break;
                        case 7:
                            client.sendLogout(actualUsername);
                            isLogged = false;
                            actualUsername = "";
                            if (udpMulticastPriceListener != null) {
                                try {
                                    udpMulticastPriceListener.leaveMulticastGroup();
                                } catch (IOException e) {
                                    System.err.println("Error during leaveGroup on logout: " + e.getMessage());
                                } finally {
                                    udpMulticastPriceListener.stopListening();
                                }
                            }
                            System.out.println("Logged out successfully.");
                            break;
                        default:
                            System.out.println("Invalid choice. Please select a valid option (1-7).");
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            client.close();
            if (udpUnicastListener != null) {
                udpUnicastListener.stopListening();
            }
            if (udpUnicastExecutor != null) {
                udpUnicastExecutor.shutdownNow();
            }

            if (udpMulticastPriceListener != null) {
                udpMulticastPriceListener.stopListening();
            }
            if (udpMulticastExecutor != null) {
                udpMulticastExecutor.shutdownNow();
            }
            scanner.close();
            System.out.println("Client application terminated.");
        }
    }
}