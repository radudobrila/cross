package orders;

public enum OrderType {
    MARKET("market"),
    LIMIT("limit"),
    STOP("stop");
    private final String type;

    /**
     * Metodo costruttore
     *
     * @param type tipo di ordine
     */
    OrderType(String type) {
        this.type = type;
    }

    /**
     * Restituisce il tipo di ordine
     *
     * @return che tipo di ordine è
     */
    public String getType() {
        return type;
    }

    /**
     * @return stringa del tipo di ordine
     */
    @Override
    public String toString() {
        return type;
    }

}
