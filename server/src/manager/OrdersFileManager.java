package manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import orders.ExecutedOrder;
import orders.LimitOrder;
import orders.Order;
import orders.StopOrder;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class OrdersFileManager {
    private static final String FILE_PATH = "orderbook.json";
    private static final String STOP_ORDER_FILE_PATH = "stop_orders.json";
    private static final String EXECUTED_ORDERS_FILE_PATH = "executed_orders.json";
    private static final String STORICO_ORDINI_FILE_PATH = "storicoOrdini.json";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Object ORDERBOOK_LOCK = new Object();
    private static final Object STOP_ORDERS_LOCK = new Object();
    private static final Object EXECUTED_ORDERS_LOCK = new Object();
    private static final Object STORICO_ORDINI_LOCK = new Object();


    /**
     * Saves the current state of bids and asks maps, along with the next available order ID, to a JSON file.
     * This method is synchronized to prevent race conditions during file writing.
     *
     * @param bids A map of bid orders.
     * @param asks A map of ask orders.
     */
    public static void saveOrders(Map<Integer, LimitOrder> bids, Map<Integer, LimitOrder> asks) {
        synchronized (ORDERBOOK_LOCK) {
            Map<String, Object> data = new HashMap<>();
            data.put("bids", bids);
            data.put("asks", asks);
            data.put("nextOrderId", Order.getNextOrderId());

            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), data);
            } catch (IOException e) {
                System.err.println("Errore durante il salvataggio degli ordini: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads the order book (bids and asks) from a JSON file.
     * Also sets the next available order ID based on the loaded data.
     * This method is synchronized to prevent race conditions during file reading.
     *
     * @return A map containing "bids" and "asks" maps.
     */
    public static Map<String, Map<Integer, LimitOrder>> loadOrdersFromOrderBook() {
        synchronized (ORDERBOOK_LOCK) {
            File file = new File(FILE_PATH);

            if (!file.exists() || file.length() == 0) {
                return createEmptyOrderBook();
            }

            try {
                Map<String, Object> fullData = mapper.readValue(
                        file,
                        new TypeReference<Map<String, Object>>() {
                        }
                );

                Object bidsObj = fullData.get("bids");
                Object asksObj = fullData.get("asks");
                Object nextOrderIdObj = fullData.get("nextOrderId");

                Map<Integer, LimitOrder> bids = mapper.convertValue(
                        bidsObj, new TypeReference<Map<Integer, LimitOrder>>() {
                        }
                );
                Map<Integer, LimitOrder> asks = mapper.convertValue(
                        asksObj, new TypeReference<Map<Integer, LimitOrder>>() {
                        }
                );

                if (nextOrderIdObj instanceof Integer) {
                    Order.setNextOrderId((Integer) nextOrderIdObj);
                } else if (nextOrderIdObj instanceof Number) {
                    Order.setNextOrderId(((Number) nextOrderIdObj).intValue());
                }

                Map<String, Map<Integer, LimitOrder>> result = new HashMap<>();
                result.put("bids", bids);
                result.put("asks", asks);

                return result;
            } catch (IOException e) {
                System.err.println("Errore durante il caricamento degli ordini dal file orderbook.json: " + e.getMessage());
                e.printStackTrace();
                return createEmptyOrderBook();
            }
        }
    }

    /**
     * Creates and returns an empty order book structure.
     *
     * @return A map with empty "asks" and "bids" maps.
     */
    private static Map<String, Map<Integer, LimitOrder>> createEmptyOrderBook() {
        Map<String, Map<Integer, LimitOrder>> empty = new HashMap<>();
        empty.put("asks", new HashMap<>());
        empty.put("bids", new HashMap<>());
        return empty;
    }

    /**
     * Loads all executed orders from their JSON file.
     * This method is synchronized to prevent race conditions during file reading.
     *
     * @return A list of executed orders. Returns an empty list if the file does not exist or is empty/corrupt.
     */
    public static List<ExecutedOrder> loadExecutedOrders() {
        synchronized (EXECUTED_ORDERS_LOCK) {
            File file = new File(EXECUTED_ORDERS_FILE_PATH);
            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }

            try {
                return mapper.readValue(file, new TypeReference<List<ExecutedOrder>>() {
                });
            } catch (MismatchedInputException e) {
                System.err.println("Errore di input durante il caricamento degli ordini eseguiti (file vuoto o malformato): " + e.getMessage());
                return new ArrayList<>();
            } catch (IOException e) {
                System.err.println("Errore I/O durante il caricamento degli ordini eseguiti: " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        }
    }

    /**
     * Retrieves executed orders that occurred in a specific month and year.
     * Loads all executed orders and filters them by date.
     * This method internally calls loadExecutedOrders, which is synchronized.
     *
     * @param year  The year to filter by.
     * @param month The month (1-12) to filter by.
     * @return A list of executed orders matching the criteria.
     */
    public static List<ExecutedOrder> getOrdersByMonth(int year, int month) {
        List<ExecutedOrder> allOrders = loadExecutedOrders();
        List<ExecutedOrder> result = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();

        for (ExecutedOrder order : allOrders) {
            calendar.setTimeInMillis(order.getTimestamp());
            int orderYear = calendar.get(Calendar.YEAR);
            int orderMonth = calendar.get(Calendar.MONTH);

            if (orderYear == year && orderMonth == (month - 1)) {
                result.add(order);
            }
        }

        return result;
    }

    /**
     * Appends a new executed order to the list of executed orders and saves the updated list to file.
     * This method is synchronized to prevent race conditions during file reading and writing.
     *
     * @param executedOrder The executed order to save.
     */
    public static void saveExecutedOrder(ExecutedOrder executedOrder) {
        synchronized (EXECUTED_ORDERS_LOCK) {
            List<ExecutedOrder> executedOrders = loadExecutedOrders();
            executedOrders.add(executedOrder);

            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(EXECUTED_ORDERS_FILE_PATH), executedOrders);
            } catch (IOException e) {
                System.err.println("Errore durante il salvataggio dell'ordine eseguito: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves the current state of stop orders to a JSON file.
     * This method is synchronized to prevent race conditions during file writing.
     *
     * @param stopOrders A map of stop orders.
     */
    public static void saveStopOrders(Map<Integer, StopOrder> stopOrders) {
        synchronized (STOP_ORDERS_LOCK) {
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(STOP_ORDER_FILE_PATH), stopOrders);
            } catch (IOException e) {
                System.err.println("Errore durante il salvataggio degli ordini stop: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads all stop orders from their JSON file.
     * This method is synchronized to prevent race conditions during file reading.
     *
     * @return A map of stop orders. Returns an empty map if the file does not exist.
     */
    public static Map<Integer, StopOrder> loadStopOrders() {
        synchronized (STOP_ORDERS_LOCK) {
            File file = new File(STOP_ORDER_FILE_PATH);
            if (!file.exists() || file.length() == 0) {
                return new HashMap<>();
            }

            try {
                return mapper.readValue(file, new TypeReference<Map<Integer, StopOrder>>() {
                });
            } catch (MismatchedInputException e) {
                System.err.println("Errore di input durante il caricamento degli ordini stop (file vuoto o malformato): " + e.getMessage());
                return new HashMap<>();
            } catch (IOException e) {
                System.err.println("Errore I/O durante il caricamento degli ordini stop: " + e.getMessage());
                e.printStackTrace();
                return new HashMap<>();
            }
        }
    }

    /**
     * Groups stop orders by their price, separating them into asks and bids.
     * This method internally calls loadStopOrders, which is synchronized.
     *
     * @return A map containing TreeMaps for "ASK" and "BID" stop orders, grouped by price.
     */
    public static Map<String, TreeMap<Integer, List<Integer>>> groupStopOrdersByPrice() {
        Map<Integer, StopOrder> stopOrders = loadStopOrders();

        TreeMap<Integer, List<Integer>> stopAsksByPrice = new TreeMap<>();
        TreeMap<Integer, List<Integer>> stopBidsByPrice = new TreeMap<>();

        for (StopOrder order : stopOrders.values()) {
            int price = order.getLimitPrice();
            int orderId = order.getOrderID();

            if (order.getTypeAB() != null && "ASK".equalsIgnoreCase(order.getTypeAB().toString())) {
                stopAsksByPrice.computeIfAbsent(price, k -> new ArrayList<>()).add(orderId);
            } else if (order.getTypeAB() != null && "BID".equalsIgnoreCase(order.getTypeAB().toString())) {
                stopBidsByPrice.computeIfAbsent(price, k -> new ArrayList<>()).add(orderId);
            }
        }

        Map<String, TreeMap<Integer, List<Integer>>> result = new HashMap<>();
        result.put("ASK", stopAsksByPrice);
        result.put("BID", stopBidsByPrice);

        return result;
    }

    /**
     * Loads trade data from the **storicoOrdini.json** file using Jackson.
     * This method is synchronized to prevent race conditions during file reading.
     *
     * @return A list of Trade objects. Returns an empty list if the file is not found, cannot be read, or parsing fails.
     */
    public static List<Trade> loadTradesFromStoricoOrdini() {
        synchronized (STORICO_ORDINI_LOCK) {
            File file = new File(STORICO_ORDINI_FILE_PATH);

            System.out.println("DEBUG: Tentativo di caricare storicoOrdini.json da: " + file.getAbsolutePath());

            if (!file.exists() || file.length() == 0) {
                System.err.println("DEBUG: File storicoOrdini.json non trovato o vuoto al percorso: " + file.getAbsolutePath());
                return new ArrayList<>();
            }

            try {
                TradeListContainer container = mapper.readValue(file, TradeListContainer.class);
                return container != null ? container.getTrades() : new ArrayList<>();

            } catch (MismatchedInputException e) {
                System.err.println("Errore di input/formato durante il caricamento dei trades da " + file.getAbsolutePath() + " (file vuoto o malformato): " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            } catch (IOException e) {
                System.err.println("Errore I/O durante la lettura del file JSON dei trades da " + file.getAbsolutePath() + ": " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            } catch (Exception e) {
                System.err.println("Errore inatteso durante il parsing del JSON dei trades da " + file.getAbsolutePath() + ": " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        }
    }
}