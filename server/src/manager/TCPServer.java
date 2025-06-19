package manager;

import java.io.*;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class TCPServer implements Runnable {
    private final Socket clientSocket;
    private Set<String> loggedUsers;
    private final OrderManager orderManager;

    /**
     * Constructor method for TCPServer.
     *
     * @param clientSocket The client socket connection.
     * @param loggedUsers  A set which contains all the users logged in (must be thread-safe).
     * @param orderManager The order manager, which must be thread-safe or have synchronized methods.
     */
    public TCPServer(Socket clientSocket, Set<String> loggedUsers, OrderManager orderManager) {
        this.clientSocket = clientSocket;
        this.loggedUsers = loggedUsers;
        this.orderManager = orderManager;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
            String command;
            while ((command = in.readLine()) != null) {
                System.out.println("Received command: " + command);
                if (command.equals("LOGIN")) {
                    handleLogin(in, out);
                } else if (command.equals("UPDATE_CREDENTIALS")) {
                    handleUpdateCredentials(in, out);
                } else if (command.equals("MARKET_ORDER")) {
                    handleMarketOrder(in, out);
                } else if (command.equals("LOGOUT")) {
                    handleLogout(in, out);
                } else if (command.equals("LIMIT_ORDER")) {
                    handleLimitOrder(in, out);
                } else if (command.equals("PRINT")) {
                    handlePrint(in, out);
                } else if (command.equals("STOP_ORDER")) {
                    handleStopOrder(in, out);
                } else if (command.equals("CANCEL")) {
                    handleCancelOrder(in, out);
                } else if (command.equals("REGISTER_PRICE_INTEREST")) {
                    handleRegisterPriceInterest(in, out);
                } else if (command.equals("PRICE_HISTORY")) {
                    handlePriceHistory(in, out);
                } else {
                    out.write("ERROR: Unknown command\n");
                    out.flush();
                }
            }
        } catch (IOException e) {
            if (!clientSocket.isClosed()) {
                System.err.println("Errore I/O nel client handler per " + clientSocket.getInetAddress() + ": " + e.getMessage());
            }
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
                System.out.println("ClientHandler per " + clientSocket.getInetAddress() + " terminato e socket chiuso.");
            } catch (IOException e) {
                System.err.println("Errore durante la chiusura del socket del client: " + e.getMessage());
            }
        }
    }

    /**
     * Handles the LOGIN command.
     * Reads login data from the input buffer, sends it to the ServerManager,
     * and writes the result back to the output buffer.
     *
     * @param in  The input buffer from the client.
     * @param out The output buffer to the client.
     * @throws IOException If an I/O error occurs during communication.
     */
    private void handleLogin(BufferedReader in, BufferedWriter out) throws IOException {
        String username = in.readLine();
        String password = in.readLine();
        String clientIp = in.readLine();
        int udpPort = Integer.parseInt(in.readLine());

        int result = ServerManager.handleLogin(username, password, clientIp, udpPort);

        out.write(result + "\n");
        out.flush();
    }

    /**
     * Handles the UPDATE_CREDENTIALS command.
     * Reads update data from the input buffer, sends it to the ServerManager,
     * and writes the result back to the output buffer.
     *
     * @param in  The input buffer from the client.
     * @param out The output buffer to the client.
     * @throws IOException If an I/O error occurs during communication.
     */
    private void handleUpdateCredentials(BufferedReader in, BufferedWriter out) throws IOException {
        String username = in.readLine();
        String password = in.readLine();
        String newPassword = in.readLine();
        if (ServerManager.isLogged(username)) {
            out.write("ERROR: User already logged in.\n");
            out.flush();
        } else {
            int result = ServerManager.handleUpdateCredentials(username, password, newPassword);
            out.write(result + "\n");
            out.flush();
        }
    }

    /**
     * Handles the LOGOUT command.
     * Reads the username from the input buffer, sends it to the ServerManager,
     * and writes the result back to the output buffer.
     * This method keeps the TCP connection open.
     *
     * @param in  The input buffer from the client.
     * @param out The output buffer to the client.
     * @throws IOException If an I/O error occurs during communication.
     */
    private void handleLogout(BufferedReader in, BufferedWriter out) throws IOException {
        String username = in.readLine();
        int result = ServerManager.logout(username);
        out.write(result + "\n");
        out.flush();
        System.out.println("TCPServer: User " + username + " logically logged out, TCP connection remains open.");
    }

    /**
     * Handles the MARKET_ORDER command.
     * Reads order details from the input buffer, sends them to the OrderManager,
     * and writes the result back to the output buffer.
     *
     * @param in  The input buffer from the client.
     * @param out The output buffer to the client.
     * @throws IOException If an I/O error occurs during communication.
     */
    private void handleMarketOrder(BufferedReader in, BufferedWriter out) throws IOException {
        String username = in.readLine();
        int ask = Integer.parseInt(in.readLine());
        int size = Integer.parseInt(in.readLine());
        if (ServerManager.isLogged(username)) {
            int result = orderManager.handleMarketOrder(ask, size, username);
            out.write(result + "\n");
            out.flush();
        } else {
            out.write("ERROR: User not logged in.\n");
            out.flush();
        }
    }

    /**
     * Handles the LIMIT_ORDER command.
     * Reads order details from the input buffer, sends them to the OrderManager,
     * and writes the result back to the output buffer.
     *
     * @param in  The input buffer from the client.
     * @param out The output buffer to the client.
     * @throws IOException If an I/O error occurs during communication.
     */
    private void handleLimitOrder(BufferedReader in, BufferedWriter out) throws IOException {
        int result = 101;
        String username = in.readLine();
        int ask = Integer.parseInt(in.readLine());
        int size = Integer.parseInt(in.readLine());
        int price = Integer.parseInt(in.readLine());
        if (ServerManager.isLogged(username)) {
            result = orderManager.handleLimitOrder(username, ask, size, price);
        } else {
            out.write("ERROR: User not logged in.\n");
            out.flush();
            return;
        }
        out.write(result + "\n");
        out.flush();
    }

    /**
     * Handles the PRINT command.
     * Reads the username, retrieves active orders from the OrderManager,
     * and sends them back to the client.
     *
     * @param in  The input buffer from the client.
     * @param out The output buffer to the client.
     * @throws IOException If an I/O error occurs during communication.
     */
    private void handlePrint(BufferedReader in, BufferedWriter out) throws IOException {
        String username = in.readLine();
        Set<Integer> result = null;
        if (ServerManager.isLogged(username)) {
            result = orderManager.handlePrint(username);
        } else {
            out.write("ERROR: User not logged in.\n");
            out.write("END\n");
            out.flush();
            return;
        }
        if (result != null) {
            for (Integer orderId : result) {
                out.write("Order ID: " + orderId + "\n");
            }
        }
        out.write("END\n");
        out.flush();
    }

    /**
     * Handles the CANCEL command.
     * Reads the username and order ID, sends to the OrderManager for cancellation,
     * and writes the result back to the output buffer.
     *
     * @param in  The input buffer from the client.
     * @param out The output buffer to the client.
     * @throws IOException If an I/O error occurs during communication.
     */
    private void handleCancelOrder(BufferedReader in, BufferedWriter out) throws IOException {
        String username = in.readLine();
        String readOrderId = in.readLine();
        int orderId = Integer.parseInt(readOrderId);

        int result = 0;
        if (ServerManager.isLogged(username)) {
            result = orderManager.handleCancelOrder(username, orderId);
        } else {
            out.write("ERROR: User not logged in.\n");
            out.flush();
            return;
        }
        out.write(result + "\n");
        out.flush();
    }

    /**
     * Handles the STOP_ORDER command.
     * Reads order details from the input buffer, sends them to the OrderManager,
     * and writes the result back to the output buffer.
     *
     * @param in  The input buffer from the client.
     * @param out The output buffer to the client.
     * @throws IOException If an I/O error occurs during communication.
     */
    private void handleStopOrder(BufferedReader in, BufferedWriter out) throws IOException {
        try {
            String username = in.readLine();
            int askBid = Integer.parseInt(in.readLine());
            int size = Integer.parseInt(in.readLine());
            int price = Integer.parseInt(in.readLine());

            if (ServerManager.isLogged(username)) {
                int result = orderManager.handleStopOrder(username, askBid, size, price);
                out.write(result + "\n");
                out.flush();
            } else {
                out.write("ERROR: User not logged in.\n");
                out.flush();
            }
        } catch (NumberFormatException e) {
            System.err.println("Errore di formato numero in handleStopOrder: " + e.getMessage());
            out.write("ERROR: Invalid number format.\n");
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            out.write("ERROR: Server internal error.\n");
            out.flush();
        }
    }

    /**
     * Handles the REGISTER_PRICE_INTEREST command.
     * Reads the username and multicast port, registers interest via ServerManager,
     * and writes the result back to the output buffer.
     *
     * @param in  The input buffer from the client.
     * @param out The output buffer to the client.
     * @throws IOException If an I/O error occurs during communication.
     */
    private void handleRegisterPriceInterest(BufferedReader in, BufferedWriter out) throws IOException {
        String username = in.readLine();
        int multicastPort = Integer.parseInt(in.readLine());
        if (ServerManager.isLogged(username)) {
            ServerManager.updateActivity(username);
            int result = ServerManager.handleRegisterMulticastInterest(username, multicastPort);
            out.write(result + "\n");
            out.flush();
        } else {
            out.write("ERROR: User not logged in.\n");
            out.flush();
        }
    }

    /**
     * Handles the PRICE_HISTORY command.
     * Reads the month from the input buffer, processes historical trade data
     * from a JSON file (via OrdersFileManager), calculates open, close, high, low prices for each day
     * of that month (across all years), and sends the daily price data back to the client.
     * Dates are considered in GMT.
     *
     * @param in  The input buffer from the client.
     * @param out The output buffer to the client.
     * @throws IOException If an I/O error occurs during communication.
     */
    private void handlePriceHistory(BufferedReader in, BufferedWriter out) throws IOException {
        try {
            String username = in.readLine();
            int month = Integer.parseInt(in.readLine());

            ServerManager.updateActivity(username);

            if (!ServerManager.isLogged(username)) {
                out.write("ERROR: User not logged in.\n");
                out.write("END_HISTORY\n");
                out.flush();
                return;
            }

            List<Trade> allTrades = OrdersFileManager.loadTradesFromStoricoOrdini();

            if (allTrades.isEmpty()) {
                out.write("NO_DATA: Nessun dato storico disponibile nel file o file vuoto.\n");
                out.write("END_HISTORY\n");
                out.flush();
                return;
            }

            Map<LocalDate, List<Trade>> tradesByDay = new TreeMap<>();

            for (Trade trade : allTrades) {
                LocalDateTime tradeDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(trade.getTimestamp()), ZoneOffset.UTC);

                if (tradeDateTime.getMonthValue() == month) {
                    LocalDate tradeDate = tradeDateTime.toLocalDate();
                    tradesByDay.computeIfAbsent(tradeDate, k -> new ArrayList<>()).add(trade);
                }
            }

            if (tradesByDay.isEmpty()) {
                out.write("NO_DATA: Nessun dato trovato per il mese " + month + " in alcun anno.\n");
                out.write("END_HISTORY\n");
                out.flush();
                return;
            }

            for (Map.Entry<LocalDate, List<Trade>> entry : tradesByDay.entrySet()) {
                LocalDate date = entry.getKey();
                List<Trade> dailyTrades = entry.getValue();

                dailyTrades.sort(Comparator.comparingLong(Trade::getTimestamp));

                long openPrice = dailyTrades.get(0).getPrice();
                long closePrice = dailyTrades.get(dailyTrades.size() - 1).getPrice();
                long highPrice = Long.MIN_VALUE;
                long lowPrice = Long.MAX_VALUE;

                for (Trade trade : dailyTrades) {
                    if (trade.getPrice() > highPrice) {
                        highPrice = trade.getPrice();
                    }
                    if (trade.getPrice() < lowPrice) {
                        lowPrice = trade.getPrice();
                    }
                }

                DailyPriceData dailyData = new DailyPriceData(date, openPrice, closePrice, highPrice, lowPrice);
                out.write(dailyData.toString() + "\n");
            }

            out.write("END_HISTORY\n");
            out.flush();

        } catch (NumberFormatException e) {
            out.write("ERROR: Invalid month format.\n");
            out.write("END_HISTORY\n");
            out.flush();
            System.err.println("Errore di formato numero in handlePriceHistory: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Errore I/O durante il caricamento dei dati storici dal file: " + e.getMessage());
            out.write("ERROR: Server internal error during price history retrieval (file I/O).\n");
            out.write("END_HISTORY\n");
            out.flush();
        } catch (Exception e) {
            System.err.println("Errore inatteso in handlePriceHistory: " + e.getMessage());
            e.printStackTrace();
            out.write("ERROR: Server internal error during price history retrieval.\n");
            out.write("END_HISTORY\n");
            out.flush();
        }
    }


}