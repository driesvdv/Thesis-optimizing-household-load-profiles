import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import helpers.DeviceReader;
import helpers.PriceReader;
import helpers.SolarReader;
import objects.Device;
import objects.PowerUsage;

public class Algorithm_no_battery {
    private static double upperBound = Double.MAX_VALUE;

    private static int[] bestStartTimes;
    private static double[] bestUsageArray = new double[96];

    // Battery parameters
    private static double initialBatteryCapacity = 0;
    private static double maxBatteryCapacity = 5;
    private static double batteryEfficiency = 0.9;
    private static double inverterPower = 2.5;

    public static int amountOfNodes = 0;

    private static double[] bestBatteryUsageArray = new double[96];

    public static void main(String[] args) throws IOException {
        // double[] prices = new
        // PriceReader("src/main/resources/priceFiles/2023-09-06_hilly.json").getPrices();
        double[] prices = new PriceReader("src/main/resources/priceFiles/2023-09-06_hilly.json").getPrices();

        double[] solarPower = new SolarReader("src/main/resources/solarFiles/11_may_normalized.csv").getSolarPower(); // 11
                                                                                                                      // may
                                                                                                                      // ->
                                                                                                                      // sunny
                                                                                                                      // day,
                                                                                                                      // 15
                                                                                                                      // may
                                                                                                                      // cloudy
                                                                                                                      // day

        PowerUsage powerUsage = new PowerUsage(solarPower, prices);

        ArrayList powerList = new ArrayList<Double>();
        String line;
        String csvSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader("src/main/resources/base_profile.csv"))) {
            // Skip the header
            br.readLine();

            while ((line = br.readLine()) != null) {
                // Use comma as separator
                String[] power = line.split(csvSplitBy);
                for (String p : power) {
                    try {
                        powerList.add(Double.parseDouble(p.trim()));
                    } catch (NumberFormatException e) {
                        System.err.println("Skipping invalid number: " + p);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Convert List to array
        double[] powerArray = new double[powerList.size()];
        for (int i = 0; i < powerList.size(); i++) {
            powerArray[i] = (double) powerList.get(i) / 1000;
        }

        ArrayList<Device> devices = new ArrayList<>();

        // VERY HARD INSTANCE
        // ArrayList<Device> devices = new ArrayList<>();
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/dishwasher.csv",
        // "Dishwasher").getDevice());
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/washer.csv",
        // "Washer").getDevice());
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/dryer.csv",
        // "Dryer").getDevice());
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/tesla.csv",
        // "Tesla").getDevice());
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/dishwasher.csv",
        // "Dishwasher").getDevice());
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/dishwasher.csv",
        // "Dishwasher").getDevice());
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/test1.csv",
        // "Test1").getDevice());
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/test2.csv",
        // "Test2").getDevice());
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/test3.csv",
        // "Test3").getDevice());

        // LOAD ALL DEVICES FROM FOLDER
        // "src/main/resources/outputs/2/output_folder_a_2/.."

        // Load all devices from folder
        // "src/main/resources/outputs/2/output_folder_a_2/.."
        String devicesFolderPath = "src/main/resources/outputs/2/output_folder_a_2";
        try (Stream<Path> paths = Files.list(Paths.get(devicesFolderPath))) {
            devices.addAll(paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".csv"))
                    .map(path -> {
                        try {
                            return new DeviceReader(path.toString(), path.getFileName().toString()).getDevice();
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .collect(Collectors.toList()));
        }

        bestStartTimes = new int[devices.size()];

        // devices.add(new DeviceReader("src/main/resources/devices/dishwasher.csv",
        // "Dishwasher").getDevice());
        // devices.add(new DeviceReader("src/main/resources/devices/dishwasher.csv",
        // "Dishwasher").getDevice());
        // devices.add(new DeviceReader("src/main/resources/devices/dishwasher.csv",
        // "Dishwasher").getDevice());

        // Sort devices by highest peak power usage first
        // devices.sort((d1, d2) -> Double.compare(d2.getPeakPower(),
        // d1.getPeakPower()));
        devices.sort((d1, d2) -> Double.compare(d2.getTotalPower(), d1.getTotalPower()));
        // devices.sort((d1, d2) -> Double.compare(d2.getAveragePower(),
        // d1.getAveragePower()));
        // devices.sort((d1, d2) -> Double.compare(d1.getMinPower(), d2.getMinPower()));

        // Calculate the original Upper Bound
        // upperBound = calculateStartUpperBound(devices, powerUsage);
        upperBound = Double.MAX_VALUE;

        double totalDuration = 0;
        int runs = 1;

        for (int run = 0; run < runs; run++) {
            double[] usageArray = new double[96];

            usageArray = powerArray.clone();

            // Start timing variable
            long startTime = System.nanoTime();

            findOptimalStartTimes(0, devices, bestStartTimes, usageArray, powerUsage);

            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000; // divide by 1000000 to get milliseconds.
            totalDuration += duration;

            // Reset the used variables
            if (run < runs - 1) {
                // upperBound = calculateStartUpperBound(devices, powerUsage);
                upperBound = Double.MAX_VALUE;
                bestStartTimes = new int[devices.size()];
                bestUsageArray = new double[96];
                usageArray = new double[96];
            }
        }

        double averageDuration = totalDuration / runs;

        // convert from ms to s round to 1 decimal after comma e.g. 400ms should be 0.4s
        averageDuration = (Math.round(averageDuration / 100.0) / 10.0);
        System.out.println("\nAverage time taken over " + runs + " runs: " + averageDuration + "s");
        // for (int tmp : bestStartTimes) {
        // System.out.println("Best start time: " + tmp);
        // }

        // Round upperbound to 4 decimals
        upperBound = Math.round(upperBound * 10000.0) / 10000.0;
        System.out.println("Best cost: " + upperBound);

        // Path to the CSV file
        String filePath = "output.csv";

        // Write the array to CSV
        try (FileWriter writer = new FileWriter(filePath)) {
            // Convert each double to string and join them with a comma
            for (int i = 0; i < bestUsageArray.length; i++) {
                writer.append(String.valueOf(bestUsageArray[i]));
                // Add a comma between elements except for the last one
                if (i < bestUsageArray.length - 1) {
                    writer.append(",");
                }
            }
            writer.append("\n"); // Optionally, add a new line at the end

            // System.out.println("Array written to CSV successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Amount of nodes: " + amountOfNodes + "\n\n");
    }

    private static void findOptimalStartTimes(int deviceIndex, ArrayList<Device> devices, int[] startTimes,
            double[] usageArray, PowerUsage powerUsage) {
        amountOfNodes++;
        if (deviceIndex == devices.size()) {
            double cost = powerUsage.calculateCost(usageArray);
            if (cost < upperBound) {
                upperBound = cost;
                bestStartTimes = startTimes.clone();
                bestUsageArray = usageArray.clone();
            }
            return;
        }

        Device currDevice = devices.get(deviceIndex);

        // Check Lower Bound

        ArrayList<Device> nextDevices = new ArrayList<>(devices.subList(deviceIndex + 1, devices.size()));
        double lowerBound = calculateLowerBound(currDevice, powerUsage, usageArray);
        // double lowerBound = 0;
        for (Device device : nextDevices) {
            lowerBound += calculateLowerBound(device, powerUsage, usageArray);
        }

        // Not really usefull according to times...
        lowerBound += calculateAdditionalCapacityTarrif(usageArray, nextDevices);

        // double smallestPeakPower = 1000;
        // for (Device device : nextDevices) {
        // if (device.getPeakPower() < smallestPeakPower) {
        // smallestPeakPower = device.getPeakPower();
        // }
        // }

        // In very specific edge cases, it could be usefull to take the smallest peak
        // power into account for the capacity tariff
        // This however is never the case, so this can be neglected

        if (powerUsage.calculateCost(usageArray) + lowerBound > upperBound) {
            return;
        }

        int bestStartIndex = findBestStartIndex(currDevice, powerUsage, usageArray);

        // Loop to check from bestStartIndex down to 0
        for (int start = bestStartIndex; start >= 0; start--) {
            // Add the device's usage to the array
            for (int j = 0; j < currDevice.getPowerUsage().size(); j++) {
                usageArray[start + j] += currDevice.getPowerUsage().get(j);
            }

            // Check if the current configuration is better
            if (powerUsage.calculateCost(usageArray) < upperBound) {
                startTimes[deviceIndex] = start;
                findOptimalStartTimes(deviceIndex + 1, devices, startTimes, usageArray, powerUsage);
            }

            // Remove the device's usage from the array (backtrack)
            for (int j = 0; j < currDevice.getPowerUsage().size(); j++) {
                usageArray[start + j] -= currDevice.getPowerUsage().get(j);
            }
        }

        // Loop to check from bestStartIndex + 1 to the latest start time
        for (int start = bestStartIndex + 1; start <= currDevice.getLatestStart(); start++) {
            // Add the device's usage to the array
            for (int j = 0; j < currDevice.getPowerUsage().size(); j++) {
                usageArray[start + j] += currDevice.getPowerUsage().get(j);
            }

            // Check if the current configuration is better
            if (powerUsage.calculateCost(usageArray) < upperBound) {
                startTimes[deviceIndex] = start;
                findOptimalStartTimes(deviceIndex + 1, devices, startTimes, usageArray, powerUsage);
            }

            // Remove the device's usage from the array (backtrack)
            for (int j = 0; j < currDevice.getPowerUsage().size(); j++) {
                usageArray[start + j] -= currDevice.getPowerUsage().get(j);
            }
        }
        return;
    }

    // private static double calculateStartUpperBound(ArrayList<Device> devices,
    // PowerUsage powerUsage) {
    // double[] usageArray = new double[96];
    // double[] costArray = powerUsage.getPriceArray();
    // int deviceCount = devices.size();
    //
    // // List to store the indices of local minima
    // ArrayList<Integer> minimaIndices = new ArrayList<>();
    //
    // // Step 1: Find local minima spaced far enough apart
    // int segmentLength = 48 / deviceCount;
    //
    // for (int i = 0; i < deviceCount; i++) {
    // int segmentStart = i * segmentLength;
    // int segmentEnd = Math.min(segmentStart + segmentLength, 96); // Ensure we
    // don't go out of bounds
    //
    // // Find the minimum value's index within this segment
    // int minIndex = segmentStart;
    // for (int j = segmentStart + 1; j < segmentEnd; j++) {
    // if (costArray[j] < costArray[minIndex]) {
    // minIndex = j;
    // }
    // }
    //
    // minimaIndices.add(minIndex);
    // }
    //
    // // Step 2: Sort devices by power usage, starting with the highest power usage
    // // devices.sort((d1, d2) -> Double.compare(d2.getTotalPower(),
    // // d1.getTotalPower()));
    //
    // // Step 3: Assign each device to a local minimum (or close to it)
    // for (int i = 0; i < deviceCount && i < minimaIndices.size(); i++) {
    // int start = minimaIndices.get(i);
    //
    // // Adjust start time if necessary to avoid boundary issues
    // if (start + devices.get(i).getPowerUsage().size() > 96) {
    // start = 96 - devices.get(i).getPowerUsage().size();
    // }
    //
    // // Apply the device usage to the usage array
    // for (int j = 0; j < devices.get(i).getPowerUsage().size(); j++) {
    // usageArray[start + j] += devices.get(i).getPowerUsage().get(j);
    // }
    // }
    //
    // // Step 4: Calculate and return the total cost with the chosen schedule
    // return powerUsage.calculateCost(usageArray);
    // }

    private static double calculateLowerBound(Device device, PowerUsage powerUsage, double[] usageArray) { // Partial //
                                                                                                           // lower //
                                                                                                           // bound //
                                                                                                           // calculation

        double initialPrice = powerUsage.calculateCost(usageArray);
        double bestPrice = 999.0;

        for (int start = 0; start <= device.getLatestStart(); start++) {
            // Add the device's usage to the array
            for (int j = 0; j < device.getPowerUsage().size(); j++) {
                usageArray[start + j] += device.getPowerUsage().get(j);
            }

            // Check if the current configuration is better
            if (powerUsage.calculateCost(usageArray) < bestPrice) {
                bestPrice = powerUsage.calculateCost(usageArray);
            }

            // Remove the device's usage from the array (backtrack)
            for (int j = 0; j < device.getPowerUsage().size(); j++) {
                usageArray[start + j] -= device.getPowerUsage().get(j);
            }
        }

        double additionalPrice = bestPrice - initialPrice;

        return additionalPrice;
    }

    private static double calculateAdditionalCapacityTarrif(double[] usageArray, ArrayList<Device> devices) {
        double maxPowerUsage = 2.5;
        for (int i = 0; i < 96; i++) {
            maxPowerUsage = Math.max(maxPowerUsage, usageArray[i]);
        }

        double maxDevicesPeakPower = 0;
        for (Device device : devices) {
            maxDevicesPeakPower = Math.max(maxDevicesPeakPower, device.getPeakPower());
        }

        double addedPowerPeak = maxDevicesPeakPower - maxPowerUsage;

        if (addedPowerPeak <= 0) {
            return 0;
        }

        return addedPowerPeak * (40 / 12 / 30);
    }

    private static int findBestStartIndex(Device currDevice, PowerUsage powerUsage, double[] usageArray) {
        int bestStartIndex = 0;
        double minCost = Double.MAX_VALUE;

        for (int start = 0; start <= currDevice.getLatestStart(); start++) {
            // Add the device's usage to the array
            for (int j = 0; j < currDevice.getPowerUsage().size(); j++) {
                usageArray[start + j] += currDevice.getPowerUsage().get(j);
            }

            // Calculate the cost with the current start index
            double currentCost = powerUsage.calculateCost(usageArray);

            // If the cost is lower, update the best start index and minimum cost
            if (currentCost < minCost) {
                minCost = currentCost;
                bestStartIndex = start;
            }

            // Remove the device's usage from the array (backtrack)
            for (int j = 0; j < currDevice.getPowerUsage().size(); j++) {
                usageArray[start + j] -= currDevice.getPowerUsage().get(j);
            }
        }

        return bestStartIndex;
    }

}
