package manager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DailyPriceData {
    private LocalDate date;
    private long openPrice;
    private long closePrice;
    private long highPrice;
    private long lowPrice;

    public DailyPriceData(LocalDate date, long openPrice, long closePrice, long highPrice, long lowPrice) {
        this.date = date;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getOpenPrice() {
        return openPrice;
    }

    public long getClosePrice() {
        return closePrice;
    }

    public long getHighPrice() {
        return highPrice;
    }

    public long getLowPrice() {
        return lowPrice;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return String.format("Date: %s, Open: %d, Close: %d, High: %d, Low: %d",
                date.format(formatter), openPrice, closePrice, highPrice, lowPrice);
    }
}