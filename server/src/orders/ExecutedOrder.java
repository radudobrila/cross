package orders;

public class ExecutedOrder {
    private int orderID;
    private String buyer;
    private String seller;
    private int size;
    private int price;
    private long timestamp;
    private OrderType orderType;

    public ExecutedOrder() {
    }

    public ExecutedOrder(int orderId, String buyer, String seller, int size, int price, long timestamp, OrderType orderType) {
        this.orderID = orderId;
        this.buyer = buyer;
        this.seller = seller;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
        this.orderType = orderType;
    }


    public String getBuyer() {
        return buyer;
    }

    public void setBuyer(String buyer) {
        this.buyer = buyer;
    }

    public String getSeller() {
        return seller;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getOrderID() { // ASSICURATI CHE QUESTO METODO ESISTA COSÌ
        return orderID;
    }

    public void setOrderId(int orderId) {
        this.orderID = orderId;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }
}
