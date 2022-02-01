package com.example.elekter;

import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Handler handler = new Handler();
    Runnable refresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Makes the app update itself every 1 minute
        refresh = new Runnable() {
            public void run() {
                mainRun();
                handler.postDelayed(refresh, 60000);
            }
        };
        handler.post(refresh);
    }

    /**
     * A callable method of the main working of the app
     */
    public void mainRun(){
        // Try to show data, if not possible(most likely missing internet) show error page
        try {
            setContentView(R.layout.activity_main);
            showData();
            // Button to calculate optimal hours to put electronics to work
            Button calculate = findViewById(R.id.calculate);
            calculate.setOnClickListener(v -> calculateHours());
        } catch (Exception ignored) {
            // Show another page and tell the user they most likely don't have internet
            setContentView(R.layout.internet_error);
            TextView error = findViewById(R.id.error);
            error.setText("Kontrolli internetiühendust");

            Button refresh = findViewById(R.id.refresh);
            refresh.setOnClickListener(v -> mainRun());
        }
    }

    /**
     * Method to calculate the best(read cheapest) continuous hours to put stuff to work
     */
    private void calculateHours() {
        List<String> prices_string = new JSONReader().JSONtoDevice();
        List<Double> prices = new ArrayList<>();

        // If possible, turn data into numbers
        for (String element:prices_string) {
            try {
                double price = Double.parseDouble(element);
                prices.add(price);
            }catch (Exception ignored){} //ignored since missing prices replaced by "Not published"
        }
        // How many continuous hours the user needs
        TextView inputHour = findViewById(R.id.inputHour);
        int howMany = Integer.parseInt(inputHour.getText().toString());

        // Gets the times that are used to answer the user
        List<String> times = times();
        double currentCheapest = Double.MAX_VALUE;
        String startTime = "";

        // In case of misclicks or missing info
        if (howMany > prices.size())
            howMany = prices.size();

        //calculate the cheapest hours by analyzing each possible continuous needed hours combination
        for (int i = 0; i < prices.size()-howMany; i++) {
            double currentValue = 0.0;
            // Gets the value of the currently analyzed combination
            for (int j = 0; j < howMany; j++) {
                currentValue += prices.get(i+j);
            }
            // If the value is the cheapest thus far, mark it as such
            if (currentValue < currentCheapest){
                currentCheapest = currentValue;
                startTime = times.get(i+1).split("-")[0]; // Get the start hour
            }
        }
        TextView answer = findViewById(R.id.answer);
        answer.setText("Parim aeg alustada on kell: "+startTime+":00");
    }

    /**
     * Method to recieve all the required data and then show it in the app
     */
    public void showData(){
        List<String> prices_list = new JSONReader().JSONtoDevice();
        List<String> times = times();
        List<String> info_list = new ArrayList<>();

        // Headers for the data
        info_list.add("Kellaaeg    senti/kWh");

        // Goes through the times and prices lists and adds them together
        for (int i = 0; i < times.size(); i++) {
            info_list.add(times.get(i)+"          "+prices_list.get(i));
        }

        ListView infoView = findViewById(R.id.info);
        // Displays the gathered info
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_1 , info_list);
        infoView.setAdapter(adapter);

        String currentHourPrice = prices_list.get(0);
        TextView currentPrice = findViewById(R.id.currentPrice);
        currentPrice.setText(currentHourPrice);
    }

    /**
     * Method to get the appropriate times needed to explain the data
     * @return List of start-end times of hours for the price data
     */
    public List<String> times(){
        DateTime dt = new DateTime();
        int currentHour  = dt.getHourOfDay();
        List<String> times = new ArrayList<>();

        // Next 24 hours, adds spaces to make it more readable in the app.
        // If x, then more spaces, if xx then less spaces(i.e 19 vs 7)
        for (int i = 0; i < 24; i++) {
            if (currentHour > 23)
                currentHour = 0;
            String spaces = "";
            if (currentHour < 10) {
                spaces = "     ";
                if (currentHour == 9)
                    spaces = "  ";
            }

            times.add(currentHour + "-" + (currentHour + 1) + spaces);
            currentHour += 1;
        }
        return times;
    }
}