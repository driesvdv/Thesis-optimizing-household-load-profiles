import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import helpers.DeviceReader;
import helpers.PriceReader;
import helpers.SolarReader;
import objects.Device;
import objects.PowerUsage;

public class Test_2 {
    private static double lowerBound = Double.MAX_VALUE;
    private static int[] bestStartTimes = new int[5]; // to store best start times for all devices
    
    // Battery variables
    private static double initialBatteryCapacity = 0;
    private static double maxBatteryCapacity = 2;
    private static double maxBatteryPower = 2;
    private static double batteryPowerGrannularity = 0.1;
    private static double batteryEfficiency = 1; // 100% efficiency when charging or discharging so 0% loss

    private static double[] bestBatteryUsageArray = new double[96];

    public static void main(String[] args) throws IOException {
        double[] prices = new PriceReader("src/main/resources/priceFiles/pricing.json").getPrices();
        double[] solarPower = new SolarReader("src/main/resources/solarFiles/solar.csv").getSolarPower();

        PowerUsage powerUsage = new PowerUsage(solarPower, prices);

        ArrayList <Device> devices = new ArrayList<>();
        devices.add(new DeviceReader("src/main/resources/deviceFiles/dishwasher.csv", "Dishwasher").getDevice());
        // devices.add(new DeviceReader("src/main/resources/devices/washer.csv", "Washer").getDevice());
        //devices.add(new DeviceReader("src/main/resources/devices/dryer.csv", "Dryer").getDevice());
        // devices.add(new DeviceReader("src/main/resources/devices/dryer.csv", "Test1").getDevice());
        // devices.add(new DeviceReader("src/main/resources/devices/dryer.csv", "Test2").getDevice());

        double[] usageArray = new double[96];

        // start timing variable
        long startTime = System.nanoTime();

        // Battery variables

        double[] batteryUsageArray = new double[96];

        // Loop through all posssible battery combinations
        findOptimalBatteryUsage(0, batteryUsageArray, devices.toArray(new Device[0]), powerUsage, usageArray);


        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000; // divide by 1000000 to get milliseconds.

        System.out.println("\nTime taken: " + duration + "ms\n");

        System.out.println("Best dishwasher start time: " + bestStartTimes[0]);
        System.out.println("Best washer start time: " + bestStartTimes[1]);
        System.out.println("Best dryer start time: " + bestStartTimes[2]);
        // System.out.println("Best test1 start time: " + bestStartTimes[3]);
        // System.out.println("Best test2 start time: " + bestStartTimes[4]);

        System.out.println("Best cost: " + lowerBound);

        // Print battery usage
        System.out.println("Battery usage:");
        for (int i = 0; i < bestBatteryUsageArray.length; i++) {
            System.out.println(bestBatteryUsageArray[i]);
        }

        System.out.println("Finished");
    }

    private static void findOptimalBatteryUsage(int index, double[] batteryUsageArray, Device[] devices, PowerUsage powerUsage, double[] usageArray) {
        // Check if all battery usage values have been set
        if (index == batteryUsageArray.length) {
            for (int i = 0; i < batteryUsageArray.length; i++) {
                usageArray[i] += batteryUsageArray[i];
            }

            findOptimalStartTimes(0, new int[1], devices, powerUsage, usageArray, batteryUsageArray);
            
            for (int i = 0; i < batteryUsageArray.length; i++) {
                usageArray[i] -= batteryUsageArray[i];
            }            
            return;
        }

        for (double i=0; i <= 2; i+=2) {
            batteryUsageArray[index] = i;
            if (checkBatteryUsageFeasibility(batteryUsageArray, index)) {
                findOptimalBatteryUsage(index + 1, batteryUsageArray, devices, powerUsage, usageArray);
            }
        }

        // for (double i=-1; i >= -2; i--) {
        //     batteryUsageArray[index] = i;
        //     if (checkBatteryUsageFeasibility(batteryUsageArray, index)) {
        //         findOptimalBatteryUsage(index + 1, batteryUsageArray, devices, powerUsage, usageArray);
        //     }
        // }
    }
    
    /**
     * Check if battery does not exceed
     * @param batteryUsageArray
     * @return
     */
    private static boolean checkBatteryUsageFeasibility(double[] batteryUsageArray, int index) {
        double batteryCapacity = initialBatteryCapacity;
        for (int i = 0; i < index; i++) {
            batteryCapacity += batteryUsageArray[i] * batteryEfficiency;
            if (batteryCapacity < 0 || batteryCapacity > maxBatteryCapacity) {
                return false;
            }
        }
        return true;
    }

    private static void findOptimalStartTimes(int deviceIndex, int[] startTimes, Device[] devices,
            PowerUsage powerUsage, double[] usageArray, double[] batteryUsageArray) {
        if (deviceIndex == devices.length) {
            double cost = powerUsage.calculateCost(usageArray);
            if (cost < lowerBound) {
                lowerBound = cost;
                System.arraycopy(startTimes, 0, bestStartTimes, 0, startTimes.length);
                System.arraycopy(batteryUsageArray, 0, bestBatteryUsageArray, 0, usageArray.length);
            }
            return;
        }

        Device currentDevice = devices[deviceIndex];
        for (int start = 0; start < currentDevice.getLatestStart(); start++) {
            // Check if adding this device's usage exceeds current best cost
            if (canAddDevice(usageArray, currentDevice, start, powerUsage)) {
                // Add the device's usage to the array
                for (int j = 0; j < currentDevice.getPowerUsage().size(); j++) {
                    usageArray[start + j] += currentDevice.getPowerUsage().get(j);
                }

                startTimes[deviceIndex] = start;
                findOptimalStartTimes(deviceIndex + 1, startTimes, devices, powerUsage, usageArray, batteryUsageArray);

                // Remove the device's usage from the array (backtrack)
                for (int j = 0; j < currentDevice.getPowerUsage().size(); j++) {
                    usageArray[start + j] -= currentDevice.getPowerUsage().get(j);
                }
            }
        }
    }

    /**
     * Check if adding a device to the usage array is feasible
     * 
     * @param usageArray
     * @param device
     * @param start
     * @param powerUsage
     * @return
     */
    private static boolean canAddDevice(double[] usageArray, Device device, int start, PowerUsage powerUsage) {
        for (int j = 0; j < device.getPowerUsage().size(); j++) {
            usageArray[start + j] += device.getPowerUsage().get(j);
        }

        boolean isFeasible = powerUsage.calculateCost(usageArray) < lowerBound;

        for (int j = 0; j < device.getPowerUsage().size(); j++) {
            usageArray[start + j] -= device.getPowerUsage().get(j);
        }

        return isFeasible;
    }
}
