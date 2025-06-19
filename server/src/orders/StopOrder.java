package orders;

public class StopOrder extends Order {
    int limitPrice;

    public StopOrder(TypeAB typeAB, OrderType ordertype, int size, long timestamp, int limitPrice, String username) {
        super(typeAB, ordertype, size, 0, timestamp, username);
        this.limitPrice = limitPrice;
    }

    public StopOrder() {
    }

    public int getLimitPrice() {
        return limitPrice;
    }

    @Override
    public int getSize() {
        return super.getSize();
    }

    @Override
    public String toString() {
        return String.format(
                "orders.StopOrder[OrderID=%d, orders.TypeAB=%s, orders.OrderType=%s, Size=%d, Price=%d, Timestamp=%d, LimitPrice=%d]",
                getOrderID(), getTypeAB(), getOrderType(), getSize(), getPrice(), getTimestamp(), getLimitPrice()
        );
    }

}
