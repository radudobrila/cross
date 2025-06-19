package orderBook;

import orders.LimitOrder;
import orders.StopOrder;
import orders.TypeAB;

import java.util.*;

public class OrderBookHelper {

    /**
     * Loads the OrderBook in a Map with Price as key and a linked hash set of all Order_ID that has that price.
     *
     * @param orders      The Map to load.
     * @param bookByPrice The TreeMap to load in the data.
     */
    public static void populatePriceBook(Map<Integer, LimitOrder> orders, TreeMap<Integer, LinkedHashSet<Integer>> bookByPrice) {
        for (Map.Entry<Integer, LimitOrder> entry : orders.entrySet()) {
            int orderId = entry.getKey();
            LimitOrder order = entry.getValue();
            int price = order.getPrice();

            bookByPrice
                    .computeIfAbsent(price, k -> new LinkedHashSet<>())
                    .add(orderId);
        }
    }

    /**
     * Loads from the OrderBook a HashMap with username and their active orders.
     *
     * @param orders      All the orders.
     * @param usersOrders The Map of user orders to populate.
     */
    public static void populateUserOrders(Map<Integer, LimitOrder> orders, Map<String, Set<Integer>> usersOrders) {
        for (LimitOrder order : orders.values()) {
            String username = order.getUsername();
            usersOrders.computeIfAbsent(username, k -> new HashSet<>()).add(order.getOrderID());
        }
    }

    /**
     * Looks for all the orders of a user.
     *
     * @param username    The user.
     * @param usersOrders The Map of user orders.
     * @return A Set of orderIDs.
     */
    public static Set<Integer> getUserOrderIDs(String username, Map<String, Set<Integer>> usersOrders) {
        return usersOrders.getOrDefault(username, Collections.emptySet());
    }

    /**
     * Returns if the order exists.
     *
     * @param username    Username.
     * @param orderId     The order's ID.
     * @param usersOrders The Map of user orders.
     * @return True -> if the order exists, False -> else.
     */
    public static boolean isOrderPresent(String username, int orderId, Map<String, Set<Integer>> usersOrders) {
        return usersOrders.containsKey(username) && usersOrders.get(username).contains(orderId);
    }

    /**
     * Returns the order.
     *
     * @param orderId The orderID.
     * @param asks    Map of ask orders.
     * @param bids    Map of bid orders.
     * @return A LimitOrder.
     */
    public static LimitOrder getOrderById(int orderId, Map<Integer, LimitOrder> asks, Map<Integer, LimitOrder> bids) {
        if (asks != null && asks.containsKey(orderId)) return asks.get(orderId);
        if (bids != null && bids.containsKey(orderId)) return bids.get(orderId);
        return null;
    }

    /**
     * Removes an order from Order Map.
     *
     * @param orderId The idOrder.
     * @param isAsk   Sets if is ask or bid.
     * @param asks    Map of ask orders.
     * @param bids    Map of bid orders.
     */
    public static void removeFromOrderMap(int orderId, boolean isAsk, Map<Integer, LimitOrder> asks, Map<Integer, LimitOrder> bids) {
        if (isAsk) {
            asks.remove(orderId);
        } else {
            bids.remove(orderId);
        }
    }

    /**
     * Removes an order from PriceBook.
     *
     * @param order       The order.
     * @param isAsk       A boolean to set if is ask or bid.
     * @param asksByPrice TreeMap of asks by price.
     * @param bidsByPrice TreeMap of bids by price.
     */
    public static void removeFromPriceBook(LimitOrder order, boolean isAsk, TreeMap<Integer, LinkedHashSet<Integer>> asksByPrice, TreeMap<Integer, LinkedHashSet<Integer>> bidsByPrice) {
        TreeMap<Integer, LinkedHashSet<Integer>> book = isAsk ? asksByPrice : bidsByPrice;
        int price = order.getPrice();

        LinkedHashSet<Integer> orderSet = book.get(price);
        if (orderSet != null) {
            orderSet.remove(order.getOrderID());
            if (orderSet.isEmpty()) {
                book.remove(price);
            }
        }
    }

    /**
     * Removes an order from the UserOrders.
     *
     * @param username    The username's set of orders.
     * @param orderId     The order to remove.
     * @param usersOrders The Map of user orders.
     */
    public static void removeFromUserOrders(String username, int orderId, Map<String, Set<Integer>> usersOrders) {
        Set<Integer> orders = usersOrders.get(username);
        if (orders != null) {
            orders.remove(orderId);
            if (orders.isEmpty()) {
                usersOrders.remove(username);
            }
        }
    }

    /**
     * Returns the opposite book (bids if order is ask, asks if order is bid).
     *
     * @param isAsk       Indicates if the order is an ask.
     * @param bidsByPrice TreeMap of bids by price.
     * @param asksByPrice TreeMap of asks by price.
     * @return The TreeMap of the opposite book.
     */
    public static TreeMap<Integer, LinkedHashSet<Integer>> getOppositeBook(boolean isAsk, TreeMap<Integer, LinkedHashSet<Integer>> bidsByPrice, TreeMap<Integer, LinkedHashSet<Integer>> asksByPrice) {
        return isAsk ? bidsByPrice : asksByPrice;
    }

    /**
     * Returns the opposite orders map (bid if order is ask, ask if order is bid).
     *
     * @param isAsk Indicates if the order is an ask.
     * @param bid   Map of bid orders.
     * @param ask   Map of ask orders.
     * @return The Map of opposite orders.
     */
    public static Map<Integer, LimitOrder> getOppositeOrders(boolean isAsk, Map<Integer, LimitOrder> bid, Map<Integer, LimitOrder> ask) {
        return isAsk ? bid : ask;
    }

    /**
     * Returns the same type of book (asks if order is ask, bids if order is bid).
     *
     * @param isAsk       Indicates if the order is an ask.
     * @param asksByPrice TreeMap of asks by price.
     * @param bidsByPrice TreeMap of bids by price.
     * @return The TreeMap of the same type of book.
     */
    public static TreeMap<Integer, LinkedHashSet<Integer>> getSameBook(boolean isAsk, TreeMap<Integer, LinkedHashSet<Integer>> asksByPrice, TreeMap<Integer, LinkedHashSet<Integer>> bidsByPrice) {
        return isAsk ? asksByPrice : bidsByPrice;
    }

    /**
     * Returns the same type of orders map (ask if order is ask, bid if order is bid).
     *
     * @param isAsk Indicates if the order is an ask.
     * @param ask   Map of ask orders.
     * @param bid   Map of bid orders.
     * @return The Map of same type orders.
     */
    public static Map<Integer, LimitOrder> getSameOrders(boolean isAsk, Map<Integer, LimitOrder> ask, Map<Integer, LimitOrder> bid) {
        return isAsk ? ask : bid;
    }

    /**
     * Tells how much coins the order needs.
     *
     * @param oppositeOrders A list of orders to look in.
     * @param idIterator     An iterator for id.
     * @param bookOrder      All the orders.
     * @param matchedSize    How much coins we matched.
     * @param usersOrders    The Map of user orders.
     * @return How many coins haven't been matched.
     */
    public static int getRemainingSize(Map<Integer, LimitOrder> oppositeOrders,
                                       int incomingOrderRemainingSize,
                                       Iterator<Integer> idIterator,
                                       int bookOrderId,
                                       LimitOrder bookOrder,
                                       int matchedSize,
                                       Map<String, Set<Integer>> usersOrders) {

        bookOrder.setSize(bookOrder.getSize() - matchedSize);

        if (bookOrder.getSize() == 0) {
            idIterator.remove();
            oppositeOrders.remove(bookOrderId);
            removeFromUserOrders(bookOrder.getUsername(), bookOrderId, usersOrders);
        }

        return incomingOrderRemainingSize - matchedSize;
    }

}