package com.example.android.wearable.wear.wearaccessibilityapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

/**
 * House Activity called immediately from the Main Activity - supports the main menu for the whole house
 */
public class HouseActivity extends FragmentActivity implements
        AmbientModeSupport.AmbientCallbackProvider {

    private static final String TAG = "HouseActivity";
    public static final String EXTRA_ROOMNAME = "com.example.android.wearable.wear.huelights.ROOMNAME";
    private SmartHouse h = MainActivity.h;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate called for house");

        // Set content view and header
        setContentView(R.layout.activity_house);
        // set the string as the the text of the view
        TextView textView = findViewById(R.id.house_header_text_view);
        textView.setText(R.string.app_name);

        AmbientModeSupport.attach(this);

        // Menu items is the actual Room List in HueHouse
        HouseRecyclerViewAdapter appListAdapter = new HouseRecyclerViewAdapter(this, h.getRoomSet());

        WearableRecyclerView recyclerView = findViewById(R.id.house_recycler_view);

        // Customizes scrolling so items farther away form center are smaller.
        ScalingScrollLayoutCallback scalingScrollLayoutCallback = new ScalingScrollLayoutCallback();
        recyclerView.setLayoutManager(
                new WearableLinearLayoutManager(this, scalingScrollLayoutCallback));

        recyclerView.setEdgeItemsCenteringEnabled(true);
        recyclerView.setAdapter(appListAdapter);

        ImageView imageView = findViewById(R.id.house_icon_view);
        if (MainActivity.wifiMgr.isWifiEnabled()
                &&  MainActivity.wifiMgr.getConnectionInfo() != null)
            imageView.setImageResource(R.drawable.wifi_ok);
        else
            imageView.setImageResource(R.drawable.no_wifi);

        Log.i(TAG, "onCreate - return");
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    private static class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {}

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop started");
        this.finishAffinity();    // when we come back just exit the whole program
    }

    @Override
    protected void onResume() {
        super.onResume();
        // update wifi icon
        ImageView imageView = findViewById(R.id.house_icon_view);
        if (MainActivity.wifiMgr.isWifiEnabled()
                &&  MainActivity.wifiMgr.getConnectionInfo() != null)
            imageView.setImageResource(R.drawable.wifi_ok);
        else
            imageView.setImageResource(R.drawable.no_wifi);
    }
}
