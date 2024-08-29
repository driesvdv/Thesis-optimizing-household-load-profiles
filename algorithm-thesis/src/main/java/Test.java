public class Test {


    public static int arr_size = 2;

    public static void main(String[] args) {

        long timings[] = new long[96];

        for (int i=2; i<96; i++) {
            arr_size = i;

            int [] batteryUsage = new int[arr_size];
            
            // Start timing
            long startTime = System.nanoTime();
            testMethod(batteryUsage, 0);
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000; // divide by 1000000 to get milliseconds.
            timings[i] = duration;
        }

        for (int i=2; i<96; i++) {
            System.out.println("Array size: " + i + " Time taken: " + timings[i] + "ms");
        }

    }

    public static void testMethod(int[] batteryUsage, int index) {
        if (index == arr_size) {
            // for (int i = 0; i < batteryUsage.length; i++) {
            //     System.out.print(batteryUsage[i] + " ");
            // }
            // System.out.println();
            return;
        }

        for (int i = 0; i <= 2; i+=2) {
            batteryUsage[index] = i;
            if (checkBatteryUsageFeasibility(batteryUsage, index)) {
                testMethod(batteryUsage, index + 1);
            }
        }

    }

    public static boolean checkBatteryUsageFeasibility(int[] batteryUsageArray, int index) {
        int batteryCapacity = 0;
        for (int i = 0; i <= index; i++) {
            batteryCapacity += batteryUsageArray[i];
            if (batteryCapacity < 0 || batteryCapacity > 6) {
                return false;
            }
        }
        return true;
    }
}


