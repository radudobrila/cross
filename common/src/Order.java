public abstract class Order {
    private int orderID;            // ID Univoco per identificare l'ordine
    private TypeAB typeAB;          // enum per identificare se si tratta di un ordine di ASK o BID
    private orderType ordertype;    // enum per identidicare il tipo di ordine
    private int size;               // numero di coin da vendere/comprare
    private int price;              // prezzo singola coin
    private long timestamp;         // Data

    /**
     * Metodo costruttore
     * <p>
     * Metodo per la creazione di un nuovo ordine
     *
     * @param orderID   ID Univoco per identificare l'ordine
     * @param typeAB    enum per identificare se si tratta di un ordine di ASK o BID
     * @param orderType enum per identidicare il tipo di ordine
     * @param size      numero di coin da vendere/comprare
     * @param price     prezzo singola coin
     * @param timestamp Data creazione dell'orcine
     */
    public Order(int orderID, TypeAB typeAB, TypeAB ordertype, int size, int price, long timestamp) {
        this.orderID = orderID;
        this.typeAB = typeAB;
        this.ordertype = ordertype;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
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
    public TypeAB getOrdertype() {
        return ordertype;
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
}
