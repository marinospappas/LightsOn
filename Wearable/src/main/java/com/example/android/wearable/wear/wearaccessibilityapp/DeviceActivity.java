package com.example.android.wearable.wear.wearaccessibilityapp;

/**
 * Device Activity called when a device is selected from the room menu
 */
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DeviceActivity extends FragmentActivity implements
    AmbientModeSupport.AmbientCallbackProvider {

    private static final String TAG = "DeviceActivity";
    private SmartHouse h = MainActivity.h;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the Intent that started this activity
        Intent intent = getIntent();
        String deviceName = intent.getStringExtra(RoomActivity.EXTRA_DEVICENAME);
        h.getCurRoom().setCurDevice(deviceName);
        Log.i(TAG, "onCreate called for device: " + deviceName);

        // Set content view and header
        setContentView(R.layout.activity_device);
        // set the string as the the text of the view
        TextView textView = findViewById(R.id.device_header_text_view);
        textView.setText(deviceName);

        AmbientModeSupport.attach(this);

        // Menu items is the actual Room List in HueHouse
        DeviceRecyclerViewAdapter appListAdapter =
                new DeviceRecyclerViewAdapter(this, h.getCurRoom().getCurDevice().getDeviceOptionsSet());

        WearableRecyclerView recyclerView = findViewById(R.id.device_recycler_view);

        // Customizes scrolling so items farther away form center are smaller.
        ScalingScrollLayoutCallback scalingScrollLayoutCallback = new ScalingScrollLayoutCallback();
        recyclerView.setLayoutManager(
                new WearableLinearLayoutManager(this, scalingScrollLayoutCallback));

        recyclerView.setEdgeItemsCenteringEnabled(true);
        recyclerView.setAdapter(appListAdapter);

        Log.i(TAG, "onCreate return");
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new DeviceActivity.MyAmbientCallback();
    }

    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {}

}




