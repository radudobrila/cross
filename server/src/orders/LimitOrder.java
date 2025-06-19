package orders;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LimitOrder extends Order {
    int limitPrice;


    public LimitOrder(TypeAB typeAB, int size, int price, long timestamp, String username) {
        super(typeAB, OrderType.LIMIT, size, price, timestamp, username);
        this.limitPrice = price;
    }

    public LimitOrder() {
        super();
        this.limitPrice = 0;
        this.setOrderType(OrderType.valueOf("LIMIT"));
    }

    public int getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(int limitPrice) {
        this.limitPrice = limitPrice;
    }


    @Override
    public String toString() {
        return String.format(
                "orders.LimitOrder[OrderID=%d, orders.TypeAB=%s, orders.OrderType=%s, Size=%d, Price=%d, Timestamp=%d, LimitPrice=%d]",
                getOrderID(), getTypeAB(), getOrderType(), getSize(), getPrice(), getTimestamp(), getLimitPrice()
        );
    }

}
