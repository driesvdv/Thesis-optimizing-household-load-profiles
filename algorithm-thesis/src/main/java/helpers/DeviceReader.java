package helpers;

import objects.Device;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;


public class DeviceReader {
    private Device device;

    public DeviceReader (String path, String name) throws FileNotFoundException {
        device =  new Device(name);

        try (Reader in = new FileReader(path) ){
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader("timestamp", "value")
                    .setSkipHeaderRecord(true)
                    .build();

            Iterable<CSVRecord> records = csvFormat.parse(in);

            for (CSVRecord record : records) {
                int value = Integer.parseInt(record.get("value"));

                device.addPowerUsage(value);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Device getDevice() {
        return device;
    }
}
