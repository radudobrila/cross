public enum TypeAB {
    BID("bid"),
    ASK("ask");

    private final String type;

    TypeAB(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }
}
