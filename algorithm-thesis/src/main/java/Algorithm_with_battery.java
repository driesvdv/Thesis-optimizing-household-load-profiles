import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import helpers.DeviceReader;
import helpers.PriceReader;
import helpers.SolarReader;
import objects.Device;
import objects.PowerUsage;

public class Algorithm_with_battery {
    private static double upperBound = Double.MAX_VALUE;

    private static int[] bestStartTimes;
    private static double[] bestUsageArray = new double[96];

    // Battery parameters
    private static double initialBatteryCapacity = 0;
    private static double maxBatteryCapacity = 5; // kw/quarter
    private static double batteryEfficiency = 1;
    private static double inverterPower = 2.5;

    private static double[] bestBatteryUsageArray = new double[96];

    private static long amountOfNodes = 0;

    public static void main(String[] args) throws IOException {
        double[] prices = new PriceReader("src/main/resources/priceFiles/2023-09-06_hilly.json").getPrices();
        //double[] prices = new PriceReader("src/main/resources/priceFiles/2023-10-29_flat.json").getPrices();

        double[] solarPower = new SolarReader("src/main/resources/solarFiles/11_may_normalized.csv").getSolarPower();

        PowerUsage powerUsage = new PowerUsage(solarPower, prices);

        ArrayList<Device> devices = new ArrayList<>();
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/dishwasher.csv", "Dishwasher").getDevice());
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/washer.csv", "Washer").getDevice());
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/dryer.csv", "Dryer").getDevice());
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/tesla.csv", "Tesla").getDevice());
        // devices.add(new DeviceReader("src/main/resources/deviceFiles/dishwasher.csv", "Dishwasher").getDevice());
        // // devices.add(new DeviceReader("src/main/resources/deviceFiles/dishwasher.csv", "Dishwasher").getDevice());
        // // devices.add(new DeviceReader("src/main/resources/deviceFiles/test1.csv", "Test1").getDevice());
        // // devices.add(new DeviceReader("src/main/resources/deviceFiles/test2.csv", "Test2").getDevice());
        // //devices.add(new DeviceReader("src/main/resources/deviceFiles/test4.csv", "Test4").getDevice());

        // bestStartTimes = new int[devices.size()];

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

        // Print the array
        //for (double power : powerArray) {
        //    System.out.println(power);
        //}

        String devicesFolderPath = "src/main/resources/outputs/6/output_folder_a_6";
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

        // Sort devices by highest peak power usage first
        devices.sort((d1, d2) -> Double.compare(d2.getTotalPower(), d1.getTotalPower()));

        // Calculate the original Upper Bound
        // upperBound = calculateStartUpperBound(devices, powerUsage); // NOT NECESSARY SINCE 1st RUN OF BRANCH AND BOUND WILL DO THIS
        upperBound = Double.MAX_VALUE;

        double totalDuration = 0;
        int runs = 1;

        for (int run = 0; run < runs; run++) {
            double[] usageArray = new double[96];

            //usageArray = powerArray.clone();

            // Start timing variable
            double startTime = System.nanoTime();

            findOptimalStartTimes(0, devices, bestStartTimes, usageArray, powerUsage);

            double endTime = System.nanoTime();
            double duration = (endTime - startTime) / 1000000000; // divide by 1000000000 to get seconds.
            totalDuration += duration;

            // Reset the used variables
            if (run < runs - 1) {
                //upperBound = calculateStartUpperBound(devices, powerUsage);
                // upperBound = Double.MAX_VALUE;
                bestStartTimes = new int[devices.size()];
                bestUsageArray = new double[96];
            }
        }

        System.err.println("\n\nAmount of nodes: " + amountOfNodes);

        // round 1 decimal after comma
        double averageDuration = Math.round((double) totalDuration / runs * 10) / 10.0;

        System.out.println("Average time taken over " + runs + " runs: " + averageDuration + "s");
        for (int tmp : bestStartTimes) {
            //System.out.println("Best start time: " + tmp);
        }
        //round upperbound 4 decimal after comma
        upperBound = Math.round(upperBound * 10000) / 10000.0;
        
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
            writer.append("\n"); // Add a new line at the end

            // Print battery usage
            for (int i = 0; i < bestBatteryUsageArray.length; i++) {
                writer.append(String.valueOf(bestBatteryUsageArray[i]));
                // Add a comma between elements except for the last one
                if (i < bestBatteryUsageArray.length - 1) {
                    writer.append(",");
                }
            }

            //System.out.println("Array written to CSV successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void findOptimalStartTimes(int deviceIndex, ArrayList<Device> devices, int[] startTimes,
            double[] usageArray, PowerUsage powerUsage) {
        amountOfNodes++;
        if (deviceIndex == devices.size()) {
            // Add battery
            double[] batteryUsageArray = new double[96];

            double cost = powerUsage.calculateCost(usageArray);
            if (cost < upperBound) {
                //System.out.println("new best solution found... calculating battery...");
                findOptimalBatteryUsage(0, usageArray, batteryUsageArray, powerUsage);


                //upperBound = cost;
                //bestStartTimes = startTimes.clone();
                //bestUsageArray = usageArray.clone();

                //System.out.println("New best cost: " + upperBound);
            }
            return;
        }

        Device currDevice = devices.get(deviceIndex);

        // Check Lower Bound

        ArrayList<Device> nextDevices = new ArrayList<>(devices.subList(deviceIndex + 1, devices.size()));
        double lowerBound = calculateLowerBound(currDevice, powerUsage, usageArray);

        for (Device device : nextDevices) {
            lowerBound += calculateLowerBound(device, powerUsage, usageArray);
        }


        double totalPowerSum = 0;
        for (Device device : devices) {
            totalPowerSum += device.getTotalPower();
        }

        lowerBound -= calculateMaxBatterySavings(totalPowerSum, powerUsage, usageArray);

        // Not really usefull according to times... (NOT VALID WITH BATTERY)
        lowerBound += calculateAdditionalCapacityTarrif(usageArray, nextDevices);

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

    private static double calculateMaxBatterySavings(double totalPowerSum, PowerUsage powerUsage, double[] usageArray) {
        double[] pricing = new double[96];
        for (int i = 0; i < 96; i++) {
            pricing[i] = 0.15 + powerUsage.getPrice(i);
        }
        // Order pricing from lowest to highest
        double[] sortedPricing = pricing.clone();
        Arrays.sort(sortedPricing);

        double leftSolarPower = 0;
        for (int i = 0; i < 96; i++) {
            leftSolarPower += Math.max(0, powerUsage.getSolarArray()[i] - usageArray[i]);
        }

        double savings = 0;
        int helper = 0;
        int intervalsNeededForPower = (int) Math.ceil(totalPowerSum / inverterPower);
        int intervalsNeededForSolar = (int) Math.ceil(leftSolarPower / inverterPower);

        intervalsNeededForPower = Math.min(intervalsNeededForPower, 48);
        intervalsNeededForSolar = Math.min(intervalsNeededForSolar, 48);
        
        // First all the solar power
        for (int i=0; i< Math.min(intervalsNeededForSolar, intervalsNeededForPower); i++) {
            savings += sortedPricing[95-i];
            helper++;
            intervalsNeededForPower--;
        }

        // Then the rest of the power
        for (int i=helper; i<intervalsNeededForPower; i++) {
            savings += sortedPricing[95-i-helper] - sortedPricing[i];
        }

        return savings;
    }

    private static void findOptimalBatteryUsage(int index, double[] usageArray, double[] batteryUsageArray, PowerUsage powerUsage) {
        // only needs to run until the last non zero usageArray index instead of 96
        amountOfNodes++;
        int latestUsageIndex = 95;
        for (int i = 95; i >= 0; i--) {
            if (usageArray[i] > 0) {
                latestUsageIndex = i;
                break;
            }
        }
        if (index >= latestUsageIndex + 1) {
            // double[] solarPower = powerUsage.getSolarArray();
            // Update the usageArray with the battery usage array
            double tempBatteryCharge = initialBatteryCapacity;

            // Make a clone of the usageArray to avoid changing the original
            double[] tmpUsageArray = usageArray.clone();

            for (int i = 0; i < 96; i++) {
                tempBatteryCharge += batteryUsageArray[i] * 0.9;

                // If energy in battery, subtract the usageArray power usage (ONLY IF BATTERY IS NOT CHARGING)
                if (tempBatteryCharge > 0 && usageArray[i] > 0 && batteryUsageArray[i] == 0) {
                    tmpUsageArray[i] -= Math.min(Math.min(tempBatteryCharge, usageArray[i]), inverterPower);
                    tempBatteryCharge -= Math.min(Math.min(tempBatteryCharge, usageArray[i]), inverterPower);
                }

                // If battery is charged, this power should be added to the usage
                tmpUsageArray[i] += batteryUsageArray[i];
            }


            double cost = powerUsage.calculateCost(tmpUsageArray);
            if (cost < upperBound) {
                upperBound = cost;
                bestUsageArray = tmpUsageArray.clone();
                bestBatteryUsageArray = batteryUsageArray.clone();
            }
            return;
        }

        for (double i = 0; i <= 2.5; i += 2.5) {
            batteryUsageArray[index] = i;
            if (checkBatteryUsageFeasibility(batteryUsageArray, index, usageArray)) {
                findOptimalBatteryUsage(index + 1, usageArray, batteryUsageArray, powerUsage);
            }
            else {
                batteryUsageArray[index] = 0;
            }
        }
    }

    private static boolean checkBatteryUsageFeasibility(double[] batteryUsageArray, int index, double[] usageArray) {
        double batteryCapacity = initialBatteryCapacity;
        for (int i = 0; i <= index; i++) {
            batteryCapacity += batteryUsageArray[i];
            if (batteryCapacity < 0 || batteryCapacity > maxBatteryCapacity) {
                return false;
            }

            if (batteryCapacity > 0 && usageArray[i] > 0 && batteryUsageArray[i] == 0) { // == because we cannot charge and discharge at the same time
                batteryCapacity -= Math.min(Math.min(batteryCapacity, usageArray[i]), inverterPower);
            }
        }
        return true;
    }

//    private static double calculateStartUpperBound(ArrayList<Device> devices, PowerUsage powerUsage) {
//        double[] usageArray = new double[96];
//        double[] costArray = powerUsage.getPriceArray();
//        int deviceCount = devices.size();
//
//        // List to store the indices of local minima
//        ArrayList<Integer> minimaIndices = new ArrayList<>();
//
//        // Step 1: Find local minima spaced far enough apart
//        int segmentLength = 96 / deviceCount;
//
//        for (int i = 0; i < deviceCount; i++) {
//            int segmentStart = i * segmentLength;
//            int segmentEnd = Math.min(segmentStart + segmentLength, 96); // Ensure we don't go out of bounds
//
//            // Find the minimum value's index within this segment
//            int minIndex = segmentStart;
//            for (int j = segmentStart + 1; j < segmentEnd; j++) {
//                if (costArray[j] < costArray[minIndex]) {
//                    minIndex = j;
//                }
//            }
//
//            minimaIndices.add(minIndex);
//        }
//
//        // Step 2: Sort devices by power usage, starting with the highest power usage
//        devices.sort((d1, d2) -> Double.compare(d2.getPeakPower(), d1.getPeakPower()));
//
//        // Step 3: Assign each device to a local minimum (or close to it)
//        for (int i = 0; i < deviceCount && i < minimaIndices.size(); i++) {
//            int start = minimaIndices.get(i);
//
//            // Adjust start time if necessary to avoid boundary issues
//            if (start + devices.get(i).getPowerUsage().size() > 96) {
//                start = 96 - devices.get(i).getPowerUsage().size();
//            }
//
//            // Apply the device usage to the usage array
//            for (int j = 0; j < devices.get(i).getPowerUsage().size(); j++) {
//                usageArray[start + j] += devices.get(i).getPowerUsage().get(j);
//            }
//        }
//
//        // Step 4: Calculate and return the total cost with the chosen schedule
//        System.out.println("Initial upper bound: " + powerUsage.calculateCost(usageArray));
//
//        return powerUsage.calculateCost(usageArray);
//    }

    private static double calculateLowerBound(Device device, PowerUsage powerUsage, double[] usageArray) { // Partial // lower // bound // calculation
        
        double initialPrice = powerUsage.calculateCost(usageArray);
        double bestPrice = 999.0;

        for (int start = 0 ; start <= device.getLatestStart(); start++) {
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

    private static double calculateAdditionalCapacityTarrif(double[] usageArray, ArrayList<Device> devices){
        double maxPowerUsage = 2.5;
        for (int i = 0; i < 96; i++) {
            maxPowerUsage = Math.max(maxPowerUsage, usageArray[i]);
        }

        double maxDevicesPeakPower = 0;
        for (Device device : devices) {
            maxDevicesPeakPower = Math.max(maxDevicesPeakPower, device.getPeakPower());
        }

        double addedPowerPeak = maxDevicesPeakPower - maxPowerUsage - 2.5; // 2.5 is battery inverter power

        if (addedPowerPeak <= 0) {
            return 0;
        }
        
        return addedPowerPeak * 40;
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
