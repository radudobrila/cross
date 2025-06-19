package orderBook;

import manager.OrdersFileManager;
import manager.UdpSessionManager;
import orders.*;

import java.util.*;

public class OrderBook {
    private TreeMap<Integer, LinkedHashSet<Integer>> bidsByPrice = new TreeMap<>(Collections.reverseOrder());
    private TreeMap<Integer, LinkedHashSet<Integer>> asksByPrice = new TreeMap<>();
    private TreeMap<Integer, List<Integer>> stopAsksByPrice = new TreeMap<>();
    private TreeMap<Integer, List<Integer>> stopBidsByPrice = new TreeMap<>();
    private Map<Integer, LimitOrder> ask;
    private Map<Integer, LimitOrder> bid;
    private Map<Integer, StopOrder> stopOrders = new HashMap<>();
    private Map<String, Set<Integer>> usersOrders = new HashMap<>();
    private int lastPrice = 0;
    private static final int GLOBAL_BTC_PRICE_TRIGGER = 10;

    /**
     * Constructor. Initializes the OrderBook by loading existing orders from files.
     */
    public OrderBook() {
        loadAllOrders();
    }

    /**
     * Constructor for OrderBook, used for initial setup or testing with pre-existing data.
     *
     * @param asks            all the ask in orderBook
     * @param bids            all the bid in orderBook
     * @param stopOrders      all the stop Orders
     * @param stopAsksByPrice a TreeMap with key the prices and a list of ask ordersID with that price
     * @param stopBidsByPrice a TreeMap with key the prices and a list of bid ordersID with that price
     */
    public OrderBook(Map<Integer, LimitOrder> asks, Map<Integer, LimitOrder> bids, Map<Integer, StopOrder> stopOrders, TreeMap<Integer, List<Integer>> stopAsksByPrice, TreeMap<Integer, List<Integer>> stopBidsByPrice) {
        this.ask = asks;
        this.bid = bids;
        this.stopOrders = stopOrders;
        this.stopAsksByPrice = stopAsksByPrice;
        this.stopBidsByPrice = stopBidsByPrice;
        OrderBookHelper.populatePriceBook(asks, asksByPrice);
        OrderBookHelper.populatePriceBook(bids, bidsByPrice);

        OrderBookHelper.populateUserOrders(ask, usersOrders);
        OrderBookHelper.populateUserOrders(bid, usersOrders);
    }

    /**
     * Manage the Order and trys to execute it if there is a remaining size add it to the orderBook as a new Order.
     * This method modifies the state of the order book.
     *
     * @param order the order to manage.
     * @return The order ID if added, otherwise 100 -> OK.
     */
    public int addOrder(LimitOrder order) {
        synchronized (this) {
            int remainingSize = matchOrder(order);

            checkAndActivateStopOrders(getLatestMarketPrice());

            if (remainingSize > 0) {
                order.setSize(remainingSize);
                addToOrderBook(order);
                OrdersFileManager.saveOrders(bid, ask);
                return order.getOrderID();
            }

            OrdersFileManager.saveOrders(bid, ask);
            return 100;
        }
    }

    /**
     * Handles the incoming order and trys first to execute it or to add it to the orderBook and saves the changes.
     * This method modifies the state of the order book.
     *
     * @param incomingOrder the order to manage.
     * @return the remaining size.
     */
    private int matchOrder(LimitOrder incomingOrder) {
        boolean isAsk = incomingOrder.getTypeAB() == TypeAB.ASK;

        TreeMap<Integer, LinkedHashSet<Integer>> oppositeBook = OrderBookHelper.getOppositeBook(isAsk, bidsByPrice, asksByPrice);
        Map<Integer, LimitOrder> oppositeOrders = OrderBookHelper.getOppositeOrders(isAsk, bid, ask);

        TreeMap<Integer, LinkedHashSet<Integer>> sameBook = OrderBookHelper.getSameBook(isAsk, asksByPrice, bidsByPrice);
        Map<Integer, LimitOrder> sameOrders = OrderBookHelper.getSameOrders(isAsk, ask, bid);

        int remainingSize = processMatching(incomingOrder, oppositeBook, oppositeOrders);

        if (remainingSize > 0) {
            addRemainingOrder(incomingOrder, sameBook, sameOrders, remainingSize);
        }

        OrdersFileManager.saveOrders(bid, ask);
        return remainingSize;
    }

    /**
     * Search if there is a compatible price and try to execute the order.
     * This method modifies the state of the order book.
     *
     * @param incomingOrder  the order to manage.
     * @param oppositeBook   the list of orders to look in.
     * @param oppositeOrders the id's to look in.
     * @return remaining size.
     */
    private int processMatching(LimitOrder incomingOrder, TreeMap<Integer, LinkedHashSet<Integer>> oppositeBook, Map<Integer, LimitOrder> oppositeOrders) {
        int remainingSize = incomingOrder.getSize();
        Iterator<Map.Entry<Integer, LinkedHashSet<Integer>>> iterator = oppositeBook.entrySet().iterator();

        while (iterator.hasNext() && remainingSize > 0) {
            Map.Entry<Integer, LinkedHashSet<Integer>> entry = iterator.next();
            int price = entry.getKey();

            if (!isPriceCompatible(incomingOrder, price)) break;

            LinkedHashSet<Integer> orderIds = entry.getValue();
            Iterator<Integer> idIterator = orderIds.iterator();

            while (idIterator.hasNext() && remainingSize > 0) {
                int orderId = idIterator.next();
                LimitOrder bookOrder = oppositeOrders.get(orderId);
                if (bookOrder == null) continue;

                int matchedSize = Math.min(remainingSize, bookOrder.getSize());
                saveExecution(incomingOrder, bookOrder, matchedSize, price);

                remainingSize = OrderBookHelper.getRemainingSize(oppositeOrders, remainingSize, idIterator, orderId, bookOrder, matchedSize, usersOrders);
            }

            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
        return remainingSize;
    }

    /**
     * Finds out if the price is compatible with the asking price.
     * This is a read-only operation.
     *
     * @param incomingOrder order to manage.
     * @param bookPrice     the price in OrderBook.
     * @return true -> if the price is compatible, false -> if the price isn't compatible.
     */
    private boolean isPriceCompatible(LimitOrder incomingOrder, int bookPrice) {
        int Price = incomingOrder.getPrice();

        if (incomingOrder.getTypeAB() == TypeAB.ASK) {
            return bookPrice >= Price;
        } else {
            return bookPrice <= Price;
        }
    }

    /**
     * Saves in file transaction's details and notifies users.
     * This method modifies 'lastPrice' and accesses 'UdpSessionManager'.
     *
     * @param incomingOrder the order to manage.
     * @param bookOrder     the order in OrderBook.
     * @param matchedSize   transaction's size.
     * @param price         transaction's price.
     */
    private void saveExecution(Order incomingOrder, Order bookOrder, int matchedSize, int price) {
        String buyer, seller;

        if (incomingOrder.getTypeAB() == TypeAB.ASK) {
            buyer = bookOrder.getUsername();
            seller = incomingOrder.getUsername();
        } else {
            buyer = incomingOrder.getUsername();
            seller = bookOrder.getUsername();
        }

        ExecutedOrder executed = new ExecutedOrder(
                incomingOrder.getOrderID(),
                buyer,
                seller,
                matchedSize,
                price,
                System.currentTimeMillis(),
                incomingOrder.getOrderType()
        );
        lastPrice = price;
        OrdersFileManager.saveExecutedOrder(executed);
        String buyerMessage = String.format("[%d]: You have Bought %d bitcoin at %d price each.",
                incomingOrder.getOrderID(), matchedSize, price);
        UdpSessionManager.notifyTradeExecution(buyer, buyerMessage);

        String sellerMessage = String.format("[%d]: You have Sold %d bitcoin at %d price each.",
                incomingOrder.getOrderID(), matchedSize, price);
        UdpSessionManager.notifyTradeExecution(seller, sellerMessage);

        if (lastPrice >= GLOBAL_BTC_PRICE_TRIGGER) {
            System.out.println("SERVER: Global BTC price trigger met! Notifying via multicast...");
            UdpSessionManager.sendBtcPriceMulticast(lastPrice);
        }
    }

    /**
     * Add to the OrderBook the remaining size from the Order.
     * This method modifies the state of the order book.
     *
     * @param incomingOrder the order to manage.
     * @param sameBook      the book to add the remaining order.
     * @param sameOrders    the orders to add the remaining order.
     * @param remainingSize the order's size.
     */
    private void addRemainingOrder(LimitOrder incomingOrder, TreeMap<Integer, LinkedHashSet<Integer>> sameBook, Map<Integer, LimitOrder> sameOrders, int remainingSize) {
        incomingOrder.setSize(remainingSize);

        int orderId = incomingOrder.getOrderID();
        sameOrders.put(orderId, incomingOrder);

        sameBook
                .computeIfAbsent(incomingOrder.getPrice(), k -> new LinkedHashSet<>())
                .add(orderId);

        usersOrders.computeIfAbsent(incomingOrder.getUsername(), k -> new HashSet<>()).add(orderId);

        System.out.println("Added remaining " + remainingSize + " @ " + incomingOrder.getPrice());
    }

    /**
     * Sets the bids map.
     *
     * @param bids The new map of bid orders.
     */
    public void setBids(Map<Integer, LimitOrder> bids) {
        synchronized (this) {
            this.bid = bids;
        }
    }

    /**
     * Sets the asks map.
     *
     * @param asks The new map of ask orders.
     */
    public void setAsks(Map<Integer, LimitOrder> asks) {
        synchronized (this) {
            this.ask = asks;
        }
    }

    /**
     * Add to orderBook an Order.
     * This method modifies the state of the order book.
     *
     * @param order the order to add.
     */
    private void addToOrderBook(LimitOrder order) {
        boolean isAsk = order.getTypeAB() == TypeAB.ASK;
        Map<Integer, LimitOrder> orderMap = isAsk ? ask : bid;
        TreeMap<Integer, LinkedHashSet<Integer>> bookByPrice = isAsk ? asksByPrice : bidsByPrice;

        int orderId = order.getOrderID();
        orderMap.put(orderId, order);

        bookByPrice
                .computeIfAbsent(order.getPrice(), k -> new LinkedHashSet<>())
                .add(orderId);

        usersOrders.computeIfAbsent(order.getUsername(), k -> new HashSet<>()).add(orderId);

        System.out.println("Added remaining " + order.getSize() + " @ " + order.getPrice());
    }

    /**
     * Looks for all the orders of a user.
     * This is a read-only operation.
     *
     * @param username the user.
     * @return a Set of orderID.
     */
    public Set<Integer> getUserOrderIDs(String username) {
        synchronized (this) {
            return OrderBookHelper.getUserOrderIDs(username, usersOrders);
        }
    }

    /**
     * Removes from everywhere the order.
     * This method modifies the state of the order book.
     *
     * @param username username.
     * @param orderId  the orderID.
     * @return 100 -> OK, 101 -> Error.
     */
    public int cancelOrder(String username, int orderId) {
        synchronized (this) {
            if (!OrderBookHelper.isOrderPresent(username, orderId, usersOrders)) {
                return 101;
            }

            LimitOrder order = OrderBookHelper.getOrderById(orderId, ask, bid);
            if (order == null) {
                return 101;
            }

            boolean isAsk = order.getTypeAB() == TypeAB.ASK;

            OrderBookHelper.removeFromOrderMap(orderId, isAsk, ask, bid);
            OrderBookHelper.removeFromPriceBook(order, isAsk, asksByPrice, bidsByPrice);
            OrderBookHelper.removeFromUserOrders(username, orderId, usersOrders);
            OrdersFileManager.saveOrders(bid, ask);
            return 100;
        }
    }

    /**
     * Executes market order.
     * This method modifies the state of the order book.
     *
     * @param marketOrder the order to execute.
     * @return true -> OK, false -> ERROR.
     */
    public boolean executeMarketOrder(MarketOrder marketOrder) {
        synchronized (this) {
            boolean isAsk = marketOrder.getTypeAB() == TypeAB.ASK;
            TreeMap<Integer, LinkedHashSet<Integer>> oppositeBook = OrderBookHelper.getOppositeBook(isAsk, bidsByPrice, asksByPrice);
            Map<Integer, LimitOrder> oppositeOrders = OrderBookHelper.getOppositeOrders(isAsk, bid, ask);

            int available = simulateMatching(marketOrder, oppositeBook, oppositeOrders);

            if (available < marketOrder.getSize()) {
                System.out.println("Market order failed: not enough liquidity.");
                System.out.println("ASK map size: " + ask.size());
                System.out.println("BID map size: " + bid.size());

                System.out.println("asksByPrice size: " + asksByPrice.size());
                System.out.println("bidsByPrice size: " + bidsByPrice.size());
                return false;
            }

            processMatchingMarketOrder(marketOrder, oppositeBook, oppositeOrders);

            OrdersFileManager.saveOrders(bid, ask);
            checkAndActivateStopOrders(getLatestMarketPrice());
            return true;
        }
    }

    /**
     * Process the market order.
     * This method modifies the state of the order book.
     *
     * @param marketOrder    the order to process.
     * @param oppositeBook   the book to look in.
     * @param oppositeOrders the list of orders to look in.
     */
    private void processMatchingMarketOrder(MarketOrder marketOrder, TreeMap<Integer, LinkedHashSet<Integer>> oppositeBook, Map<Integer, LimitOrder> oppositeOrders) {
        int remainingSize = marketOrder.getSize();
        Iterator<Map.Entry<Integer, LinkedHashSet<Integer>>> iterator = oppositeBook.entrySet().iterator();

        while (iterator.hasNext() && remainingSize > 0) {
            Map.Entry<Integer, LinkedHashSet<Integer>> entry = iterator.next();
            LinkedHashSet<Integer> orderIds = entry.getValue();
            Iterator<Integer> idIterator = orderIds.iterator();

            while (idIterator.hasNext() && remainingSize > 0) {
                int orderId = idIterator.next();
                LimitOrder bookOrder = oppositeOrders.get(orderId);
                if (bookOrder == null) continue;

                int matchedSize = Math.min(remainingSize, bookOrder.getSize());
                saveExecution(marketOrder, bookOrder, matchedSize, entry.getKey());

                remainingSize = OrderBookHelper.getRemainingSize(oppositeOrders, remainingSize, idIterator, orderId, bookOrder, matchedSize, usersOrders);
            }

            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    /**
     * Simulates matching to see if there's enough coin available.
     * This is a read-only operation (doesn't modify the state of the order book).
     *
     * @param marketOrder    the order.
     * @param oppositeBook   the book to look in.
     * @param oppositeOrders the list of the orders to look in.
     * @return total amount of coin matched.
     */
    private int simulateMatching(MarketOrder marketOrder, TreeMap<Integer, LinkedHashSet<Integer>> oppositeBook, Map<Integer, LimitOrder> oppositeOrders) {
        int remainingSize = marketOrder.getSize();
        int totalMatched = 0;

        for (Map.Entry<Integer, LinkedHashSet<Integer>> entry : oppositeBook.entrySet()) {
            for (int orderId : entry.getValue()) {
                LimitOrder bookOrder = oppositeOrders.get(orderId);
                if (bookOrder == null) continue;

                int matchedSize = Math.min(remainingSize, bookOrder.getSize());
                totalMatched += matchedSize;
                remainingSize -= matchedSize;

                if (remainingSize == 0) break;
            }

            if (remainingSize == 0) break;
        }
        return totalMatched;
    }

    /**
     * Adds a stop order.
     * This method modifies the state of the order book.
     *
     * @param order the order to add.
     * @return the order ID.
     */
    public int addStopOrder(StopOrder order) {
        synchronized (this) {
            stopOrders.put(order.getOrderID(), order);

            int stopPrice = order.getLimitPrice();
            if (order.getTypeAB() == TypeAB.ASK) {
                stopAsksByPrice.computeIfAbsent(stopPrice, k -> new ArrayList<>()).add(order.getOrderID());
            } else if (order.getTypeAB() == TypeAB.BID) {
                stopBidsByPrice.computeIfAbsent(stopPrice, k -> new ArrayList<>()).add(order.getOrderID());
            }
            usersOrders.computeIfAbsent(order.getUsername(), k -> new HashSet<>()).add(order.getOrderID());
            OrdersFileManager.saveStopOrders(this.stopOrders);

            System.out.println("Stop Order added: " + order);
            return order.getOrderID();
        }
    }

    /**
     * Checks if any stop orders should be activated based on the current market price.
     * This method modifies the state of the order book.
     *
     * @param currentMarketPrice The current market price to check against stop prices.
     */
    public void checkAndActivateStopOrders(int currentMarketPrice) {
        List<Integer> stopSellToActivate = findActivatedStopOrders(currentMarketPrice, TypeAB.BID);
        List<Integer> stopBuyToActivate = findActivatedStopOrders(currentMarketPrice, TypeAB.ASK);
        processActivatedStopOrders(stopSellToActivate, currentMarketPrice);
        processActivatedStopOrders(stopBuyToActivate, currentMarketPrice);
        if (!stopSellToActivate.isEmpty() || !stopBuyToActivate.isEmpty()) {
            OrdersFileManager.saveStopOrders(this.stopOrders);
        }
    }

    /**
     * Finds stop orders that should be activated based on the current market price and order type.
     * This method modifies the state of stopAsksByPrice or stopBidsByPrice by removing activated orders.
     *
     * @param currentMarketPrice The current market price.
     * @param typeAB             The type of order (BID for stop-sells, ASK for stop-buys).
     * @return A list of order IDs that have been activated.
     */
    private List<Integer> findActivatedStopOrders(int currentMarketPrice, TypeAB typeAB) {
        List<Integer> activatedOrderIds = new ArrayList<>();
        TreeMap<Integer, List<Integer>> targetTreeMap;
        Iterator<Map.Entry<Integer, List<Integer>>> iterator;

        if (typeAB == TypeAB.BID) {
            targetTreeMap = stopBidsByPrice;
            iterator = targetTreeMap.descendingMap().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, List<Integer>> entry = iterator.next();
                int stopPrice = entry.getKey();
                if (currentMarketPrice <= stopPrice) {
                    activatedOrderIds.addAll(entry.getValue());
                    iterator.remove();
                } else {
                    break;
                }
            }
        } else if (typeAB == TypeAB.ASK) {
            targetTreeMap = stopAsksByPrice;
            iterator = targetTreeMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, List<Integer>> entry = iterator.next();
                int stopPrice = entry.getKey();
                if (currentMarketPrice >= stopPrice) {
                    activatedOrderIds.addAll(entry.getValue());
                    iterator.remove();
                } else {
                    break;
                }
            }
        }
        return activatedOrderIds;
    }

    /**
     * Processes a list of activated Stop Order IDs.
     * Converts each activated StopOrder into a MarketOrder and executes it.
     * This method modifies the 'stopOrders' map and potentially other order book maps via executeMarketOrder.
     *
     * @param activatedOrderIds  The list of Order IDs to process.
     * @param currentMarketPrice The market price at the time of activation.
     */
    private void processActivatedStopOrders(List<Integer> activatedOrderIds, int currentMarketPrice) {
        for (Integer orderId : activatedOrderIds) {
            StopOrder stopOrder = stopOrders.remove(orderId);
            if (stopOrder != null) {
                usersOrders.computeIfPresent(stopOrder.getUsername(), (key, value) -> {
                    value.remove(orderId);
                    return value.isEmpty() ? null : value;
                });

                String orderTypeStr = (stopOrder.getTypeAB() == TypeAB.BID) ? "Stop-SELL" : "Stop-BUY";
                System.out.println(orderTypeStr + " order " + orderId +
                        " (LimitPrice(StopPrice): " + stopOrder.getLimitPrice() + ") activated at market price " + currentMarketPrice);

                MarketOrder marketOrder = new MarketOrder(
                        stopOrder.getTypeAB(),
                        OrderType.MARKET,
                        stopOrder.getSize(),
                        System.currentTimeMillis(),
                        stopOrder.getUsername()
                );
                executeMarketOrder(marketOrder);
            }
        }
    }

    /**
     * Loads all existing limit and stop orders from file storage into the OrderBook.
     * This method should ideally be called only once during initialization of the OrderBook.
     */
    private void loadAllOrders() {
        Map<String, Map<Integer, LimitOrder>> loadedLimitOrders = OrdersFileManager.loadOrdersFromOrderBook();
        this.ask = loadedLimitOrders.get("asks");
        this.bid = loadedLimitOrders.get("bids");

        OrderBookHelper.populatePriceBook(this.ask, this.asksByPrice);
        OrderBookHelper.populatePriceBook(this.bid, this.bidsByPrice);

        OrderBookHelper.populateUserOrders(this.ask, this.usersOrders);
        OrderBookHelper.populateUserOrders(this.bid, this.usersOrders);
        Map<Integer, StopOrder> loadedStopOrders = OrdersFileManager.loadStopOrders();
        if (loadedStopOrders != null && !loadedStopOrders.isEmpty()) {
            this.stopOrders.putAll(loadedStopOrders);

            for (StopOrder order : loadedStopOrders.values()) {
                int stopPrice = order.getLimitPrice();
                if (order.getTypeAB() == TypeAB.ASK) {
                    this.stopAsksByPrice.computeIfAbsent(stopPrice, k -> new ArrayList<>()).add(order.getOrderID());
                } else if (order.getTypeAB() == TypeAB.BID) {
                    this.stopBidsByPrice.computeIfAbsent(stopPrice, k -> new ArrayList<>()).add(order.getOrderID());
                }
                this.usersOrders.computeIfAbsent(order.getUsername(), k -> new HashSet<>()).add(order.getOrderID());
            }
        }
        List<ExecutedOrder> executed = OrdersFileManager.loadExecutedOrders();
        if (!executed.isEmpty()) {
            this.lastPrice = executed.get(executed.size() - 1).getPrice();
        }
    }

    /**
     * Retrieves the latest market price.
     * This is a read-only operation.
     *
     * @return The latest executed price, or an average/top price from the order book if no trades occurred.
     */
    private int getLatestMarketPrice() {
        if (this.lastPrice != 0) {
            return this.lastPrice;
        }
        if (!bidsByPrice.isEmpty() && !asksByPrice.isEmpty()) {
            return (bidsByPrice.firstKey() + asksByPrice.firstKey()) / 2;
        } else if (!bidsByPrice.isEmpty()) {
            return bidsByPrice.firstKey();
        } else if (!asksByPrice.isEmpty()) {
            return asksByPrice.firstKey();
        }
        return 0;
    }
}