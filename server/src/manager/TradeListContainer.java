package manager;

import java.util.List;
import java.util.ArrayList;

public class TradeListContainer {

    private List<Trade> trades;


    public TradeListContainer() {
        this.trades = new ArrayList<>();
    }


    public TradeListContainer(List<Trade> trades) {
        this.trades = trades;
    }


    public List<Trade> getTrades() {
        return trades;
    }
    
    public void setTrades(List<Trade> trades) {
        this.trades = trades;
    }

    @Override
    public String toString() {
        return "TradeListContainer{" +
                "trades=" + trades +
                '}';
    }
}