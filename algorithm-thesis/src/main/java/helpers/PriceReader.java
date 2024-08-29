package helpers;

import objects.Price;
import org.json.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class PriceReader {
    private ArrayList<Price> prices = new ArrayList<>();

    public PriceReader(String path) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(path)));
        JSONArray jsonArray = new JSONArray(json);

        // Iterate over each JSON object in the array
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);

            String time = jsonObject.getString("Time");
            int hour = Integer.parseInt(time.split(":")[0]);

            double  price =  jsonObject.getDouble("Price") / 1000.0;

            prices.add(new Price(hour, price));
        }

        prices.sort((p1, p2) -> p1.getHour() - p2.getHour());
    }

    public double getPrice(int hour) {
        return prices.get(hour).getPrice();
    }

    /**
     * Get list of prices (in EUR per KWh) for each quarter of the day
     * @return
     */
    public double[] getPrices() {
        double[] prices = new double[this.prices.size() * 4];
        for (int i = 0; i < this.prices.size() * 4; i++) {
            prices[i] = this.prices.get(i / 4).getPrice();
        }
        return prices;
    }
}
