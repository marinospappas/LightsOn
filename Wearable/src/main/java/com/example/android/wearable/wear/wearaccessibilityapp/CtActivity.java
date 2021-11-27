package com.example.android.wearable.wear.wearaccessibilityapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class CtActivity extends FragmentActivity implements
            AmbientModeSupport.AmbientCallbackProvider {
    private static final String TAG = "CtActivity";
    private SmartHouse h = MainActivity.h;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the Intent that started this activity
        Intent intent = getIntent();
        String deviceName;
        if (h.getCurRoom().getCurDevice() == null) {
            // in this case this Activity was actually called for a whole room
            deviceName = h.getCurRoom().getName();
            Log.i(TAG, "onCreate called for room: " + deviceName);
        }
        else {
            deviceName = h.getCurRoom().getCurDevice().getName();
            Log.i(TAG, "onCreate called for device: " + deviceName);
        }

        // Set content view and header
        setContentView(R.layout.activity_ct);
        // set the string as the the text of the view
        TextView textView = findViewById(R.id.ct_header_text_view);
        textView.setText(deviceName);

        AmbientModeSupport.attach(this);

        // Menu items is the actual Room List in HueHouse
        CtRecyclerViewAdapter appListAdapter =
                new CtRecyclerViewAdapter(this, h.getCtSet());

        WearableRecyclerView recyclerView = findViewById(R.id.ct_recycler_view);

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
        return new CtActivity.MyAmbientCallback();
    }

    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {}

}
