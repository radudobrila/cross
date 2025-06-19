package manager;


public class Trade {
    private int orderId;
    private String type;
    private String orderType;
    private int size;
    private long price;
    private long timestamp;


    public Trade() {
    }

    public Trade(int orderId, String type, String orderType, int size, long price, long timestamp) {
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
    }


    public int getOrderId() {
        return orderId;
    }

    public String getType() {
        return type;
    }

    public String getOrderType() {
        return orderType;
    }

    public int getSize() {
        return size;
    }

    public long getPrice() {
        return price;
    }

    public long getTimestamp() {
        return timestamp;
    }
    
    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "orderId=" + orderId +
                ", type='" + type + '\'' +
                ", orderType='" + orderType + '\'' +
                ", size=" + size +
                ", price=" + price +
                ", timestamp=" + timestamp +
                '}';
    }
}