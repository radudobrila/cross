package orders;

public enum TypeAB {
    BID("bid"),
    ASK("ask");

    private final String type;

    /**
     * Metodo costruttore
     *
     * @param type tipo ordine (ask/bid)
     */
    TypeAB(String type) {
        this.type = type;
    }

    /**
     * Restituisce il tipo di ordine (akm/bid)
     *
     * @return che tipo di ordine è
     */
    public String getType() {
        return type;
    }

    /**
     * @return stringa del tipo di ordine (ask/bid)
     */
    @Override
    public String toString() {
        return type;
    }
}
