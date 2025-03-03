public enum orderType {
    MARKET("market"),
    LIMIT("limit"),
    STOP("stop");
    private final String type;

    orderType(String type) {
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
