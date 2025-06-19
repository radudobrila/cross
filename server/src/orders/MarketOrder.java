package orders;

public class MarketOrder extends Order {

    public MarketOrder(TypeAB typeAB, OrderType ordertype, int size, long timestamp, String username) {
        super(typeAB, ordertype, size, 0, timestamp, username);
    }

    @Override
    public String toString() {
        return String.format(
                "orders.MarketOrder[OrderID=%d, orders.TypeAB=%s, orders.OrderType=%s, Size=%d, Price=%d, Timestamp=%d]",
                getOrderID(), getTypeAB(), getOrderType(), getSize(), getPrice(), getTimestamp()
        );
    }
}
