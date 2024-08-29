package helpers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class SolarReader {
    private ArrayList<Double> solarPower;

    public SolarReader(String path) throws IOException {
        solarPower = new ArrayList<>();

        try (var reader = Files.newBufferedReader(Paths.get(path))) {
            var records = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader().parse(reader);
            for (CSVRecord record : records) {
                double power = Double.parseDouble(record.get("Most recent forecast"));
                solarPower.add((power));
            }
        }
    }

    public void addSolarPower(double power) {
        solarPower.add(power);
    }

    /**
     * Get list of solar power (in KWh) for each quarter of the day
     * @return
     */
    public double[] getSolarPower() {
        double[] solarPower = new double[this.solarPower.size()];
        for (int i = 0; i < this.solarPower.size(); i++) {
            solarPower[i] = this.solarPower.get(i);
        }
        return solarPower;
    }
}
