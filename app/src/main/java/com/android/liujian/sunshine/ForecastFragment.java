package com.android.liujian.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.android.liujian.sunshine.data.WeatherContract;
import com.android.liujian.sunshine.sync.SunshineSyncAdapter;
import com.android.liujian.sunshine.utils.Utility;

/**
 * Created by liujian on 15/10/19.
 * ForecastFragment
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String LOG_TAG = ForecastFragment.class.getSimpleName();
    private static final int FORECAST_LOADER_ID = 0;

    private static final String FORECAST_LIST_POSITION = "list_position";



    /** Projection columns*/
    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "."  + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG,
            WeatherContract.LocationEntry.COLUMN_CITY_NAME,
            WeatherContract.LocationEntry.COLUMN_COUNTRY_NAME
    };

    /** These indices are tied to FORECAST_COLUMNS, if FORECAST_COLUMNS changes , those must changes too */
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;
    public static final int COL_LOCATION_COORD_LAT = 7;
    public static final int COL_LOCATION_COORD_LONG = 8;
    public static final int COL_LOCATION_CITY = 9;
    public static final int COL_LOCATION_COUNTRY = 10;


    public ForecastFragment(){

    }

    public interface Callback{
        void onItemSelected(Uri weatherUri);
    }


    private ForecastAdapter mWeatherListAdapter;
    private String mLocationSetting;
    private int mPosition;
    private ListView mForecastList;
    private boolean mIsUseTodayLayout;


    public void setUseTodayLayout(boolean useTodayLayout){
        mIsUseTodayLayout = useTodayLayout;

        if(mWeatherListAdapter != null){
            mWeatherListAdapter.setUseTodayLayout(useTodayLayout);
        }
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if(savedInstanceState != null && savedInstanceState.containsKey(FORECAST_LIST_POSITION)){
            mPosition = savedInstanceState.getInt(FORECAST_LIST_POSITION);
        }

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mForecastList = (ListView) rootView.findViewById(R.id.forecast_weather_list);

        mForecastList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                mPosition = position;

                Cursor cursor = (Cursor)adapterView.getItemAtPosition(position);

                Uri uri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(mLocationSetting, cursor.getLong(COL_WEATHER_DATE));

                ((Callback)getActivity()).onItemSelected(uri);
            }
        });

        /*String locationSetting = Utility.getPreferenceLocation(getContext());

        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC ";
        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting, System.currentTimeMillis());

        Cursor cursor = getContext().getContentResolver().query(weatherUri, null, null, null, sortOrder);*/


        mWeatherListAdapter = new ForecastAdapter(getContext(), null, 0);
        mWeatherListAdapter.setUseTodayLayout(mIsUseTodayLayout);

        mForecastList.setAdapter(mWeatherListAdapter);

        getLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);

        return rootView;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mPosition != ListView.INVALID_POSITION){
            outState.putInt(FORECAST_LIST_POSITION, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * A callback method
     * If the location changed, then load weather information again
     */
    public void onLocationChanged(){
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fragment_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        /*if(id == R.id.action_refresh){
            updateWeather();
            return true;
        }else */if(id == R.id.action_map){
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Get the location from sharedPreferences and update weather information
     */
    public void updateWeather(){
        /*Intent alarmIntent = new Intent(getActivity(), SunshineService.AlarmReceiver.class);
        alarmIntent.putExtra(SunshineService.LOCATION_QUERY_KEY, mLocationSetting);

        PendingIntent pi = PendingIntent.getBroadcast(getContext(), 0, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager)getContext().getSystemService(Context.ALARM_SERVICE);

        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pi);*/

        SunshineSyncAdapter.syncImmediately(getContext());

    }


    public void openPreferredLocationInMap(){

        if(mWeatherListAdapter != null){
            Cursor cursor = mWeatherListAdapter.getCursor();
            if(cursor != null){
                cursor.moveToPosition(0);
                String lat = cursor.getString(COL_LOCATION_COORD_LAT);
                String lon = cursor.getString(COL_LOCATION_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + lat + "," + lon);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if(intent.resolveActivity(getContext().getPackageManager()) != null){
                    startActivity(intent);
                }else{
                    Log.d(LOG_TAG, "Couldn't locate " + ", don't install map app.");
                }
            }
        }

    }



    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        mLocationSetting = Utility.getPreferenceLocation(getContext());

        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC ";
        Uri weatherUri = WeatherContract.WeatherEntry
                .buildWeatherLocationWithStartDate
                        (mLocationSetting, System.currentTimeMillis());


        return new CursorLoader(getContext(), weatherUri, FORECAST_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mWeatherListAdapter.swapCursor(data);

        if(mPosition != ListView.INVALID_POSITION){
            mForecastList.setSelection(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mWeatherListAdapter.swapCursor(null);
    }
}
