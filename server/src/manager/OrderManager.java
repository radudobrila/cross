package manager;

import javafx.scene.paint.Stop;
import orderBook.OrderBook;
import orders.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class OrderManager {

    private final OrderBook orderBook;

    public OrderManager(OrderBook orderBook) {
        this.orderBook = orderBook;
    }

    public int handleMarketOrder(int ask, int size, String username) {
        TypeAB typeAB = null;
        OrderType orderType = OrderType.MARKET;
        if (ask == 0) {
            typeAB = TypeAB.ASK;
        } else if (ask == 1) {
            typeAB = TypeAB.BID;
        }
        MarketOrder order = new MarketOrder(typeAB, orderType, size, System.currentTimeMillis(), username);
        boolean result = orderBook.executeMarketOrder(order);
        return 100;
    }

    /**
     * Function that handles the LimitOrder request, takes the data and sends a call to OrderBook
     *
     * @param username to keep track of who place the order
     * @param ask      0 -> ask, 1 -> bid
     * @param size     quantity to sell/buy
     * @param price    cost
     * @return the operation's result
     */
    public int handleLimitOrder(String username, int ask, int size, int price) {
        TypeAB typeAB = null;
        OrderType orderType = OrderType.LIMIT;
        if (ask == 0) {
            typeAB = TypeAB.ASK;
        } else if (ask == 1) {
            typeAB = TypeAB.BID;
        }
        LimitOrder order = new LimitOrder(typeAB, size, price, System.currentTimeMillis(), username);
        return orderBook.addOrder(order);
    }

    public List<ExecutedOrder> handleHistoryOrder(int year, int month) {
        return OrdersFileManager.getOrdersByMonth(year, month);
    }

    public Set<Integer> handlePrint(String username) {
        return orderBook.getUserOrderIDs(username);
    }

    public int handleCancelOrder(String username, int orderId) {
        return orderBook.cancelOrder(username, orderId);
    }

    /**
     * Takes data, create a StopOrder and sends
     *
     * @param username
     * @param askBid
     * @param size
     * @param price
     * @return
     */
    public int handleStopOrder(String username, int askBid, int size, int price) {
        TypeAB typeAB = null;
        OrderType orderType = OrderType.STOP;
        if (askBid == 0) {
            typeAB = TypeAB.ASK;
        } else if (askBid == 1) {
            typeAB = TypeAB.BID;
        }

        StopOrder stopOrder = new StopOrder(typeAB, orderType, size, System.currentTimeMillis(), price, username);

        orderBook.addStopOrder(stopOrder);

        return stopOrder.getOrderID();
    }
}
