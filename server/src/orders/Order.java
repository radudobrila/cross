package orders;

public abstract class Order {
    private static int nextOrderId = 0;

    private final int orderID;
    private TypeAB typeAB;
    private OrderType OrderType;
    private int size;
    private int price;
    private long timestamp;
    private String username;

    /**
     * Metodo costruttore
     * Metodo per la creazione di un nuovo ordine
     *
     * @param typeAB    enum per identificare se si tratta di un ordine di ASK o BID
     * @param orderType enum per identidicare il tipo di ordine
     * @param size      numero di coin da vendere/comprare
     * @param price     prezzo singola coin
     * @param timestamp Data creazione dell'ordine
     */
    public Order(TypeAB typeAB, OrderType orderType, int size, int price, long timestamp, String username) {
        this.orderID = nextOrderId++;
        this.typeAB = typeAB;
        this.OrderType = orderType;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
        this.username = username;
    }

    public Order() {
        this.orderID = nextOrderId++;
        this.typeAB = null;
        this.OrderType = null;
        this.size = 0;
        this.price = 0;
        this.timestamp = 0;
        this.username = "";
    }

    /**
     * Restituisce l'identificatore univoco dell'ordine.
     *
     * @return un intero che identidica univocamente l'ordine
     */
    public int getOrderID() {
        return orderID;
    }

    /**
     * Restituisce il tipo di ordine ASK/Bid
     *
     * @return ASK/BID
     */
    public TypeAB getTypeAB() {
        return typeAB;
    }

    /**
     * Restituisce il tipo di ordine MarketOrder, LimitOrder, StopOrder
     *
     * @return MarketOrder, LimitOrder, StopOrder
     */
    public OrderType getOrderType() {
        return OrderType;
    }

    /**
     * Restituisce il numero di coin nell'ordine
     *
     * @return numero di coin nell'ordine
     */
    public int getSize() {
        return size;
    }

    /**
     * Restituisce il prezzo della singola coin
     *
     * @return prezzo per coin
     */
    public int getPrice() {
        return price;
    }

    /**
     * Restituisce la data di creazione dell'ordine
     *
     * @return data dell'ordine
     */
    public long getTimestamp() {
        return timestamp;
    }

    public void setOrderType(OrderType orderType) {
        this.OrderType = orderType;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setTypeAB(TypeAB typeAB) {
        this.typeAB = typeAB;
    }

    public String getUsername() {
        return username;
    }

    public static void setNextOrderId(int nextId) {
        nextOrderId = nextId;
    }

    public static int getNextOrderId() {
        return nextOrderId;
    }

    public static void incrementNextOrderId() {
        nextOrderId++;
    }

}
