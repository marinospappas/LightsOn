/*
 * Copyright (C) 2017 Google Inc. All Rights Reserved.
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
package com.example.android.wearable.wear.wearaccessibilityapp;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainActivity extends FragmentActivity implements
        AmbientModeSupport.AmbientCallbackProvider {

    private static final String TAG = "MainActivity";
    // public static final String EXTRA_ROOMNAME = "com.example.android.wearable.wear.huelights.ROOMNAME";

    // home config file (rooms, lights, and other parameters)
    // JSON format
    //static final String APP_FILE_PATH = "/data/data/com.example.android.wearable.wear.wearaccessibilityapp/files/";
    static final String HOME_CONFIG_FILE = "homeConfig.json";
    static final String BRIDGE_CONFIG_FILE = "bridgeConfig.json";
    static String appFilePath;

    /**
     * The SmartHouse object (House/Rooms/Devices data) is static
     * so that it can be easily accessed by all activities across the application
     */
    public static SmartHouse h;

    public static WifiManager wifiMgr;

    // Bluetooth Adapter will be used to turn bluetooth off and then back on
    static BluetoothAdapter bAdapter;
    boolean initialBlueToothState = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        Log.i(TAG, "onCreate started");
        appFilePath = context.getFilesDir().getPath() + "/";
        Log.i(TAG, "data dir " + appFilePath);

        // First set content view and header
        setContentView(R.layout.activity_main);
        // set the string as the the text of the view
        TextView textView = findViewById(R.id.main_header_text_view);
        textView.setText(R.string.app_name);

        // first check bluetooth state and disable if on
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bAdapter.isEnabled()) {
            Log.i(TAG, "onCreate - disabling bluetooth");
            initialBlueToothState = true;
            bAdapter.disable();
        }
        else
            Log.i(TAG, "onCreate - bluetooth already off");

        // Now read the config file into as string
        String data;
        try {
            (new File(appFilePath+"filename.txt")).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Log.i(TAG, "read from Home config: "+appFilePath+"/"+HOME_CONFIG_FILE);
            data = new String(Files.readAllBytes(Paths.get(appFilePath+"/"+HOME_CONFIG_FILE)));
        } catch(IOException e) {
            Log.e(TAG, "error with the config file: "+e.toString());
            Log.i(TAG, "onCreate - abort");
            Toast toast = Toast.makeText(context,"Error with config file "+HOME_CONFIG_FILE, Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        // build SmartHouse structure
        Log.i(TAG, "about to build SmartHouse: "+data);
        try {
            JSONObject jHome = new JSONObject(data);
            h = new SmartHouse(jHome, context);
        } catch (Exception e) {
            Log.e(TAG,"JSON error with home data - aborting");
            Toast toast = Toast.makeText(context,"Internal Program Error", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        if (!h.isConfigOK()) {
            Log.e(TAG,"error with home config - aborting");
            Toast toast = Toast.makeText(context,"Internal Program Error", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        AmbientModeSupport.attach(this);

        // First screen is just a message

        // setup Wifi Manager
        WifiManager myWifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiMgr = myWifiMgr;

        // register the wifi state callback
        Log.d(TAG, "onCreate - registering wifi status callback");
        registerNetworkCallback(context);

        Log.d(TAG, "onCreate - checking wifi status");
        ImageView imageView = findViewById(R.id.main_icon_view);
        if (myWifiMgr.isWifiEnabled()
        &&  myWifiMgr.getConnectionInfo() != null)
            imageView.setImageResource(R.drawable.wifi_ok);
        else
            imageView.setImageResource(R.drawable.no_wifi);

        Log.i(TAG, "onCreate - return");
    }

    // Launch activity for the main menu (house)
    public void launchActivity(View view) {
        Log.i(TAG,"LaunchActivity - will launch HouseActivity");
        Intent intent = new Intent(this, HouseActivity.class);
        startActivity(intent);
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    private static class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {}

    // register a callback to monitor wifi status
    static boolean houseActivityLaunched = false;

    private void registerNetworkCallback(Context context) {

        final ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "connectivity mgr on Available called - Wifi is on");
                if (!houseActivityLaunched) {
                    Log.i(TAG,"on Available - launching HouseActivity");
                    launchActivity(null);
                    houseActivityLaunched = true;
                }
                else
                    Log.i(TAG,"on Available - HouseActivity already launched");
                ImageView imageView = findViewById(R.id.main_icon_view);
                imageView.setImageResource(R.drawable.wifi_ok);
            }

        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy started");
        // re-enable bluetooth if necessary
        if (initialBlueToothState) {
            Log.i(TAG, "onDestroy - re-enabling bluetooth");
            bAdapter.enable();
        }
    }
}
