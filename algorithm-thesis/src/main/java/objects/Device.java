package objects;

import java.util.ArrayList;

public class Device {
    private String name;
    private ArrayList<Double> powerUsage;

    public Device(String name) {
        this.name = name;
        this.powerUsage = new ArrayList<>();
    }

    public void addPowerUsage(double power) {
        this.powerUsage.add(power / 1000);
    }

    public String getName() {
        return this.name;
    }

    public ArrayList<Double> getPowerUsage() {
        return this.powerUsage;
    }

    public int getLatestStart() {
        return 96 - this.powerUsage.size();
    }

    public double getPeakPower() {
        return this.powerUsage.stream().mapToDouble(Double::doubleValue).max().getAsDouble();
    }

    public double getTotalPower() {
        return this.powerUsage.stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getAveragePower() {
        return this.powerUsage.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
    }

    public double getMinPower() {
        return this.powerUsage.stream().mapToDouble(Double::doubleValue).min().getAsDouble();
    }
}
