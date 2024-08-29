package objects;

public class Price {
    private int hour;
    private double price;

    public Price(int hour, double price) {
        this.hour = hour;
        this.price = price; // EUR PER MEGAWATT
    }

    public int getHour() {
        return hour;
    }

    public double getPrice() {
        return price;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String toString() {
        return "Hour: " + hour + ", Price: " + price;
    }
}
