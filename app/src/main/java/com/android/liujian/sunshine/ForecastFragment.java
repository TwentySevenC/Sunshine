package com.android.liujian.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by liujian on 15/10/19.
 * ForecastFragment
 */
public class ForecastFragment extends Fragment{
    private static final String LOG_TAG = ForecastFragment.class.getSimpleName();

    public ForecastFragment(){

    }

    public class BuildConfig{
        public static final String OPEN_WEATHER_APPID = "8f9aa35c830c4bb0ca1b56b180c54bea";
    }

    private ArrayAdapter<String> mWeatherListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView forecastListView = (ListView) rootView.findViewById(R.id.forecast_weather_list);
        forecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = mWeatherListAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast);

                startActivity(intent);

            }
        });


        mWeatherListAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.list_item_forcast,
                R.id.list_item_forecast_textview,
                new ArrayList<String>());

        forecastListView.setAdapter(mWeatherListAdapter);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_forecast_refresh, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.forecast_refresh){
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Get the location from sharedPreferences and update weather information
     */
    public void updateWeather(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = sharedPreferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        FetchWeatherTask task = new FetchWeatherTask();
        task.execute(location);
    }


    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String>{

        private String getReadableDataString(Long time){
            SimpleDateFormat shortedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortedDateFormat.format(time);
        }


        private String formatHighLows(double high, double low){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = sharedPreferences.getString(getString(R.string.pref_units_key),
                    getString(R.string.pref_units_metric));

            if(unitType.equals(getString(R.string.pref_units_imperial))){
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            }else if(!unitType.equals(getString(R.string.pref_units_metric))){
                Log.d(LOG_TAG, "Unit type not found " + unitType);
            }

            return String.valueOf(Math.round(high)) + "/" + String.valueOf(Math.round(low));
        }



        public String[] getWeatherDataFromJson(String json){

            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_MAIN = "main";
            final String OWM_TEMP = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";


            try {
                JSONObject jsonObject = new JSONObject(json);
                JSONArray jsonArray = jsonObject.getJSONArray(OWM_LIST);
                String[] weatherArray = new String[jsonArray.length()];

                Time dayTime = new Time();
                dayTime.setToNow();

                int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

                dayTime = new Time();

                for(int i = 0; i < jsonArray.length(); i++){
                    JSONObject listObject = jsonArray.getJSONObject(i);

                    long dateTime;

                    dateTime = dayTime.setJulianDay(julianStartDay + i);

                    JSONObject object = listObject.getJSONObject(OWM_TEMP);
                    String tempString = formatHighLows(object.getDouble(OWM_MAX), object.getDouble(OWM_MIN));

                    object = listObject.getJSONArray(OWM_WEATHER).getJSONObject(0);
                    String weather = object.getString(OWM_MAIN);

                    weatherArray[i] = getReadableDataString(dateTime) + " - " + weather + " - " + tempString;
                }

                return weatherArray;

            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }



        @Override
        protected String doInBackground(String... params) {
            if(params[0] == null){
                return null;
            }

            String units = "metric";
            int days = 7;


            HttpURLConnection forecastConnection = null;
            BufferedReader reader = null;
            String forecastWeatherString = null;


            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String UNIT_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "appid";

            try {

                Uri uri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(UNIT_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, String.valueOf(days))
                        .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_APPID)
                        .build();


                URL url = new URL(uri.toString());
                forecastConnection = (HttpURLConnection)url.openConnection();
                forecastConnection.setRequestMethod("GET");
                forecastConnection.connect();

                InputStream inputStream = forecastConnection.getInputStream();

                if(inputStream == null){
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder buffer = new StringBuilder();
                String line;

                while ( (line = reader.readLine()) != null){
                    buffer.append(line).append("\n");
                }

                if(buffer.length() == 0){
                    return null;
                }

                forecastWeatherString = buffer.toString();


            } catch (IOException e){
                e.printStackTrace();
            }finally {
                if(reader != null){
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(forecastConnection != null)
                    forecastConnection.disconnect();
            }

            return forecastWeatherString;
        }


        @Override
        protected void onPostExecute(String weatherData) {
            String[] weathers = getWeatherDataFromJson(weatherData);
            if(weathers != null){
                mWeatherListAdapter.clear();
                mWeatherListAdapter.addAll(new ArrayList<>(Arrays.asList(weathers)));
            }
        }
    }


}
