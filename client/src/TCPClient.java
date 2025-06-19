import java.io.*;
import java.net.Socket;

public class TCPClient {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 1234;

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    /**
     * Creates a connection to the server.
     * Synchronizes access to socket and I/O streams during connection.
     *
     * @throws IOException If an I/O error occurs when creating the socket or streams.
     */
    public void connect() throws IOException {
        synchronized (this) {
            if (this.socket == null || this.socket.isClosed()) {
                this.socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            }
        }
    }

    /**
     * Closes the connection to the server.
     * Synchronizes access to socket and I/O streams during closing.
     */
    public void close() {
        synchronized (this) {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Errore durante la chiusura delle risorse del client TCP: " + e.getMessage());
                e.printStackTrace();
            } finally {
                socket = null;
                in = null;
                out = null;
            }
        }
    }

    /**
     * Sends to the server a print orders request.
     *
     * @param username The user to print orders for.
     * @return A String containing all active orders, or "No active orders." if none.
     * @throws IOException If an I/O error occurs during communication.
     */
    public String sendPrintOrders(String username) throws IOException {
        synchronized (this) {
            if (socket == null || socket.isClosed()) {
                throw new IOException("Connessione al server non attiva per visualizzare gli ordini.");
            }
            out.write("PRINT\n");
            out.write(username + "\n");
            out.flush();

            StringBuilder ordersBuilder = new StringBuilder();
            String response;
            while ((response = in.readLine()) != null) {
                if (response.equals("END")) {
                    break;
                }
                ordersBuilder.append(response).append("\n");
            }
            String result = ordersBuilder.toString().trim();
            return result.isEmpty() ? "No active orders." : result;
        }
    }

    /**
     * Sends to the server a Login request.
     *
     * @param username Username.
     * @param password Password.
     * @param clientIp Client's IP address for UDP unicast.
     * @param udpPort  Client's UDP port for unicast.
     * @return true if login is successful, false otherwise.
     * @throws IOException If an I/O error occurs during communication.
     */
    public boolean sendLogin(String username, String password, String clientIp, int udpPort) throws IOException {
        synchronized (this) {
            if (socket == null || socket.isClosed()) {
                throw new IOException("Connessione al server non attiva per il login.");
            }
            out.write("LOGIN\n");
            out.write(username + "\n");
            out.write(password + "\n");
            out.write(clientIp + "\n");
            out.write(String.valueOf(udpPort) + "\n");
            out.flush();

            String response = in.readLine();
            int code = Integer.parseInt(response);
            if (code == 100) {
                System.out.println("User logged in successfully");
                return true;
            }
            if (code == 101) {
                System.out.println("Username or password incorrect");
            }
            if (code == 102) {
                System.out.println("User already logged in");
            }
            if (code == 103) {
                System.out.println("Error logging in");
            }
            return false;
        }
    }

    /**
     * Sends to the server an update password request.
     *
     * @param username    Username that wants to update password.
     * @param password    Old Password.
     * @param newPassword New Password.
     * @return The integer code result from the server.
     * @throws IOException If an I/O error occurs during communication.
     */
    public int sendUpdateCredentials(String username, String password, String newPassword) throws IOException {
        synchronized (this) {
            if (socket == null || socket.isClosed()) {
                throw new IOException("Connessione al server non attiva per l'aggiornamento credenziali.");
            }
            System.out.println("Sending update credentials");
            out.write("UPDATE_CREDENTIALS\n");
            out.write(username + "\n");
            out.write(password + "\n");
            out.write(newPassword + "\n");
            out.flush();
            System.out.println("Sei arrivato dopo la scrittura sul buffer");
            String response = in.readLine();
            System.out.println("Server risponde " + response);
            int code = Integer.parseInt(response);
            if (code == 100) {
                System.out.println("Password updated successfully");
            } else if (code == 101) {
                System.out.println("Invalid new password");
            } else if (code == 102) {
                System.out.println("Username or password incorrect");
            } else if (code == 103) {
                System.out.println("You cannot use the same password");
            }
            return code;
        }
    }

    /**
     * Sends to the server a Logout request.
     * This method sends the logout command and reads the server's response.
     * It does NOT close the socket or await an "EXIT" message.
     *
     * @param username Username that requests the logout.
     * @return The integer code result from the server (100 for success, 101 for error).
     * @throws IOException If an I/O error occurs during communication.
     */
    public int sendLogout(String username) throws IOException {
        synchronized (this) {
            if (socket == null || socket.isClosed()) {
                throw new IOException("Connessione al server non attiva per il logout.");
            }
            out.write("LOGOUT\n");
            out.write(username + "\n");
            out.flush();

            String response = in.readLine();
            int code = Integer.parseInt(response);
            if (code == 100) {
                System.out.println("User logged out successfully (server response).");
            } else if (code == 101) {
                System.out.println("Error logging out (server response).");
            }
            return code;
        }
    }

    /**
     * Sends to the server a request for a new market order.
     *
     * @param username The username placing the order.
     * @param ask      0 for ASK (sell), 1 for BID (buy).
     * @param quantity The amount to trade.
     * @return The integer code result from the server.
     * @throws IOException If an I/O error occurs during communication.
     */
    public int sendMarketOrder(String username, int ask, int quantity) throws IOException {
        synchronized (this) {
            if (socket == null || socket.isClosed()) {
                throw new IOException("Connessione al server non attiva per Market Order.");
            }
            out.write("MARKET_ORDER\n");
            out.write(username + "\n");
            out.write(ask + "\n");
            out.write(quantity + "\n");
            out.flush();
            String response = in.readLine();
            return Integer.parseInt(response);
        }
    }

    /**
     * Sends to the server a request for a new limit order.
     *
     * @param username The username.
     * @param ask      0 if it's an ask (sell), 1 if it's a bid (buy).
     * @param quantity The amount to trade.
     * @param price    The limit price.
     * @return The integer code result from the server.
     * @throws IOException If an I/O error occurs during communication.
     */
    public int sendLimitOrder(String username, int ask, int quantity, int price) throws IOException {
        synchronized (this) {
            if (socket == null || socket.isClosed()) {
                throw new IOException("Connessione al server non attiva per Limit Order.");
            }
            out.write("LIMIT_ORDER\n");
            out.write(username + "\n");
            out.write(ask + "\n");
            out.write(quantity + "\n");
            out.write(price + "\n");
            out.flush();
            String response = in.readLine();
            return Integer.parseInt(response);
        }
    }

    /**
     * Sends to the server a request to cancel an order.
     *
     * @param username The username.
     * @param orderID  The ID of the order to cancel.
     * @return The integer code result from the server.
     * @throws IOException If an I/O error occurs during communication.
     */
    public int sendCancelOrder(String username, int orderID) throws IOException {
        synchronized (this) {
            if (socket == null || socket.isClosed()) {
                throw new IOException("Connessione al server non attiva per la cancellazione dell'ordine.");
            }
            out.write("CANCEL\n");
            out.write(username + "\n");
            out.write(orderID + "\n");
            out.flush();
            String response = in.readLine();
            int result = Integer.parseInt(response);
            if (result == 100) {
                System.out.println("Order [" + orderID + "] Canceled successfully");
            } else {
                System.out.println("Error");
            }
            return result;
        }
    }

    /**
     * Sends to the server a request for a new stop order.
     *
     * @param username The username.
     * @param askBid   0 if it's an ask (sell), 1 if it's a bid (buy).
     * @param size     The amount to trade.
     * @param price    The stop price.
     * @return The integer code result from the server.
     * @throws IOException If an I/O error occurs during communication.
     */
    public int sendStopOrder(String username, int askBid, int size, int price) throws IOException {
        synchronized (this) {
            if (socket == null || socket.isClosed()) {
                throw new IOException("Connessione al server non attiva per Stop Order.");
            }
            out.write("STOP_ORDER\n");
            out.write(username + "\n");
            out.write(askBid + "\n");
            out.write(size + "\n");
            out.write(price + "\n");
            out.flush();

            String response = in.readLine();
            return Integer.parseInt(response);
        }
    }

    /**
     * Sends to the server a request to register interest in price notifications.
     *
     * @param username      The username.
     * @param multicastPort The multicast port to subscribe to.
     * @return The integer code result from the server.
     * @throws IOException If an I/O error occurs during communication.
     */
    public int sendRegisterPriceInterest(String username, int multicastPort) throws IOException {
        synchronized (this) {
            if (socket == null || socket.isClosed()) {
                throw new IOException("Connessione al server non attiva per la registrazione multicast.");
            }
            out.write("REGISTER_PRICE_INTEREST\n");
            out.write(username + "\n");
            out.write(String.valueOf(multicastPort) + "\n");
            out.flush();

            String response = in.readLine();
            System.out.println("Server response for price interest: " + response);
            return Integer.parseInt(response);
        }
    }

    /**
     * Requests historical price data for a given month from the server and prints it directly to console.
     *
     * @param month    The month (1-12) for which to retrieve historical data.
     * @param username The username making the request.
     * @throws IOException If an I/O error occurs during communication.
     */
    public void sendPriceHistory(int month, String username) throws IOException {
        synchronized (this) {
            if (socket == null || socket.isClosed()) {
                throw new IOException("Connessione al server non attiva per la cronologia prezzi.");
            }

            out.write("PRICE_HISTORY\n");
            out.write(username + "\n");
            out.write(month + "\n");
            out.flush();

            System.out.println("\n--- Cronologia Prezzi (Mese: " + month + ") ---");
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("END_HISTORY")) {
                    break;
                }
                System.out.println(line);
            }
            System.out.println("--- Fine Cronologia ---");
        }
    }
}