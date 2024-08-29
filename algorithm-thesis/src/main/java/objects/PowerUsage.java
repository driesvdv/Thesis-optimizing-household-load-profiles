package objects;

public class PowerUsage {
    private double[] solarPower;
    private double[] prices;

    public PowerUsage( double[] solarPower, double[] prices) {
        this.solarPower = solarPower;
        this.prices = prices;
    }
    
    public double calculateCost(double[] powerUsage) {
        double price = 0.0;
        double maxPowerUsage = 0;

        for (int i = 0; i < 96; i++) {
            double netUsage = powerUsage[i] - solarPower[i];
            if (netUsage >= 0) {
                price += (0.15 + prices[i]) * netUsage;
            } else {
                price += (prices[i] * 0.1) * netUsage;
            }

            maxPowerUsage = Math.max(maxPowerUsage, netUsage);
        }

        double maxPowerPeak = Math.max(2.5, maxPowerUsage);

        // Capacity tariff
        price += maxPowerPeak  *  40 * (40 / 12 / 30);


        return price;
    }

    // public double[] getPriceArray() {
    //     return prices;
    // }

    public double getPrice(int hour) {
        if (hour < 0 || hour >= 96) {
            throw new IllegalArgumentException("Hour must be between 0 and 95");
        }
        return prices[hour] + 0.15;
    }

    public double[] getSolarArray() {
        return solarPower;
    }
}
