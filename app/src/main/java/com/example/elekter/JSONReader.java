package com.example.elekter;

import android.os.AsyncTask;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Class for reading in and dealing with JSON
 */
public class JSONReader extends AsyncTask<String, Void, JSONObject> {

    /**
     * Method to get the prices from "https://nordpoolprice.codeborne.com//api/prices"
     * @param strings
     * @return JSON object containing the dates prices
     */
    protected JSONObject doInBackground(String... strings) {
        //Create a connection and issue a GET request to the specified URL
        HttpURLConnection urlConnection;
        try {
            URL url = new URL("https://nordpoolprice.codeborne.com/api/prices");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.connect();

            // Read in the info and build a string with it
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            String jsonString = sb.toString();
            System.out.println("JSON: " + jsonString);
            // create the JSON object that contains the info
            return new JSONObject(jsonString);
        }catch (Exception e){
            System.out.println(e);
        }
        return null;
    }

    /**
     * Method to make the JSON data appropriate for the app
     * @return A list of prices made into strings of the next 24 hours
     */
    protected List<String> JSONtoDevice(){
        AsyncTask<String, Void, JSONObject> json = new JSONReader().execute();

        String[] prices = new String[24];
        int location = 0; // index of the prices array

        DateTime dt = new DateTime();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");

        int currentHour = dt.getHourOfDay();

        int fromTomorrow = currentHour;

        // Get the data for the current remaining day and the next hours that need to be taken from tomorrow
        try {
            for (; currentHour < 24; currentHour++) {
                prices[location] = json.get().getJSONArray(formatter.print(dt)).get(currentHour).toString();
                location += 1;
            }
            // If tomorrows data has yet to be released.
            if (json.get().getJSONArray(formatter.print(dt.plusDays(1))).get(0) != null) {
                for (int i = 0; i < fromTomorrow; i++) {
                    prices[location] = json.get().getJSONArray(formatter.print(dt.plusDays(1))).get(i).toString();
                    location += 1;
                }
            }
        } catch (JSONException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        // Transform the prices to cents/kWh and if no price given, replace it with "Not published"
        for (int i = 0; i < prices.length; i++) {
            if (prices[i] != null)
                prices[i] = Double.toString(Math.round(Double.parseDouble(prices[i])*10.0)/100.0);
            else prices[i] = "Not published";
        }
        return new ArrayList<>(Arrays.asList(prices));
    }
}
