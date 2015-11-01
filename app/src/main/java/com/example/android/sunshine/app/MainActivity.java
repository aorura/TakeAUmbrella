/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.android.sunshine.app.bt.TBlue;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    private boolean mTwoPane;
    private String mLocation;
    //dongwook2.shin
    private String mWeather;

    //move from ComBlueTooth.java to MainActivity.java of sunshine
    // start of move
    static TBlue tBlue;
    byte[] LED_COLOR = new byte[5];
    int idx = 0;
    // end of move

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocation = Utility.getPreferredLocation(this);
        mWeather = Utility.getPreferredRainbow(this);
        // Toast.makeText(getBaseContext(), mWeather, Toast.LENGTH_LONG).show();

        //move from ComBlueTooth.java to MainActivity.java of sunshine
        // start of move
        LED_COLOR[0] = 0x01;    // blue
        LED_COLOR[1] = 0x02;    // Green
        LED_COLOR[2] = 0x04;    // RED
        LED_COLOR[3] = 0x08;    // YELLOW
        LED_COLOR[4] = 0x10;    // WHITE

        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(BTReceiver, filter1);
        this.registerReceiver(BTReceiver, filter2);
        this.registerReceiver(BTReceiver, filter3);
        this.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // end of move

        setContentView(R.layout.activity_main);
        if (findViewById(R.id.weather_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
            getSupportActionBar().setElevation(0f);
        }

        ForecastFragment forecastFragment = ((ForecastFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_forecast));
        forecastFragment.setUseTodayLayout(!mTwoPane);

        SunshineSyncAdapter.initializeSyncAdapter(this);


        //dongwook2.shin
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onSend(Utility.getWeatherConditionForBT(ForecastAdapter.WEATHER), ForecastAdapter.TEMP);
                        Toast.makeText(getBaseContext(), "" + Utility.getWeatherConditionForBT(ForecastAdapter.WEATHER), Toast.LENGTH_LONG).show();
                        //Toast.makeText(getBaseContext(), ""+ForecastAdapter.WEATHER, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }, 5000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String location = Utility.getPreferredLocation(this);
        String weather = Utility.getPreferredRainbow(this);


        // update the location in our second pane using the fragment manager
        if (location != null && !location.equals(mLocation)) {
            ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            if (null != ff) {
                ff.onLocationChanged();
            }
            DetailFragment df = (DetailFragment) getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if (null != df) {
                df.onLocationChanged(location);
            }
            mLocation = location;
        }


        // change LED light when weather value is chagend
        if (weather != null && weather != getString(R.string.pref_rainbow_default) && !weather.equals(mWeather)) {
            if (weather.equals(getString(R.string.pref_rainbow_sunny))) {
                onSend(Utility.CLEAR, ForecastAdapter.TEMP);
            } else if (weather.equals(getString(R.string.pref_rainbow_cloud))) {
                onSend(Utility.CLOUDS, ForecastAdapter.TEMP);
            } else if (weather.equals(getString(R.string.pref_rainbow_rainy))) {
                //settings menu can be added. so I used else if statement.
                onSend(Utility.RAIN, ForecastAdapter.TEMP);
            }
            //onSend(weather);
            Toast.makeText(getBaseContext(), weather, Toast.LENGTH_LONG).show();

        }
    }

    @Override
    public void onItemSelected(Uri contentUri) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, contentUri);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(contentUri);
            startActivity(intent);
        }
    }

    //move from ComBlueTooth.java to MainActivity.java of sunshine
    // start of move

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver BTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Do something if connected
                Toast.makeText(getApplicationContext(), "flone connected", Toast.LENGTH_SHORT).show();
                // floneConnected = true;
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Do something if disconnected
                Toast.makeText(getApplicationContext(), "flone disconnected", Toast.LENGTH_SHORT).show();
                //floneConnected = false;
                try {
                    tBlue.close();

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "flone disconnected error", Toast.LENGTH_SHORT).show();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Done searching
                Toast.makeText(getApplicationContext(), "Bluetooth discovery finished.", Toast.LENGTH_SHORT).show();
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Device found
                Toast.makeText(getApplicationContext(), "Found Bluetooth device.", Toast.LENGTH_SHORT).show();
            }
        }
    };


    public void onDestroy() {
        this.unregisterReceiver(BTReceiver);
        super.onDestroy();
    }

    public void onStart() {
        super.onStart();

        tBlue = new TBlue(this);
    }

    public void onStop() {
        if (tBlue != null)
            tBlue.close();
        super.onStop();
    }

    public void onConnect(View view) {
        Toast.makeText(this, "onConnect", Toast.LENGTH_SHORT).show();
       /* if (!blueName.getText().toString().equals(""))
            bluetoothId = blueName.getText().toString(); */
        try {
            tBlue.connect();
        } catch (NullPointerException ex) {
            Toast.makeText(this, "Warning: Flone not connected", Toast.LENGTH_SHORT).show();
        }
    }

    public void onSend(int WeatherId, double temp) {
        if (btConnected()) {
            Toast.makeText(this, "isConnect == true", Toast.LENGTH_SHORT).show();

            byte[] command = new byte[6];

            command[0] = 0x08;  // Weather Command

            switch (WeatherId) {
                case Utility.THUNDERSTORM:
                    command[1] = 0x01; //blue
                    break;
                case Utility.DRIZZLE:
                    command[1] = 0x08; //yellow
                    break;
                case Utility.RAIN:
                    command[1] = 0x01; //blue
                    break;
                case Utility.SNOW:
                    command[1] = 0x10; //WHITE
                    break;
                case Utility.ATMOSPHERE:
                    command[1] = 0x08; //yellow
                    break;
                case Utility.CLOUDS:
                    command[1] = 0x02; //green
                    break;
                case Utility.CLEAR:
                    command[1] = 0x04; //RED
                    break;
                default:
                    //TODO
                    break;
            }

            // select a celcius or fahrenheit
            if (Utility.isMetric(this)) {
                command[2] = 0x04; // 0x04 = celcius, 0x02 = fahrenheit
            } else {
                command[2] = 0x02;
            }

            String strTemp = Utility.formatTemperatureWithoutDegree(this, temp);
            Log.d("park", "strTemp: " + strTemp);
            // command[3] : 10's digit number
            // command[4] : 1's digit number
            int length = strTemp.length();
            if (length == 1) {
                command[3] = 0x00;
                command[4] = (byte) (strTemp.charAt(0) - 0x30);
            } else if (length == 2) {
                command[3] = (byte) (strTemp.charAt(0) - 0x30);
                command[4] = (byte) (strTemp.charAt(1) - 0x30);
            } else {
                command[3] = 0x09;
                command[4] = 0x09;
            }

            command[5] = 0x7F;  // Use this value to end the command.

            tBlue.write(command);
        } else {
            Toast.makeText(this, "isConnect == false", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean btConnected() {
        boolean btConnected = false;
        try {
            btConnected = tBlue.streaming();
        } catch (Exception ex) {
            btConnected = false;
        }
        return btConnected;
    }
    // end of move
}
