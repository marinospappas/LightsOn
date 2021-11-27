package com.example.android.wearable.wear.wearaccessibilityapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

/**
 * Room Activity called when a room is selected from the main menu
 */
public class RoomActivity extends FragmentActivity implements
        AmbientModeSupport.AmbientCallbackProvider {

    private static final String TAG = "RoomActivity";
    public static final String EXTRA_DEVICENAME = "com.example.android.wearable.wear.huelights.DEVICENAME";
    private SmartHouse h = MainActivity.h;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the Intent that started this activity
        Intent intent = getIntent();
        String roomName = intent.getStringExtra(HouseActivity.EXTRA_ROOMNAME);
        h.setCurRoom(roomName);
        Log.i(TAG, "onCreate called for room: " + roomName);

        // Set content view and header
        setContentView(R.layout.activity_room);
        // set the string as the the text of the view
        TextView textView = findViewById(R.id.room_header_text_view);
        textView.setText(roomName);

        AmbientModeSupport.attach(this);

        // Menu items is the actual Device List in this Room
        RoomRecyclerViewAdapter appListAdapter =
                new RoomRecyclerViewAdapter(this, h.getCurRoom().getDeviceSet());

        WearableRecyclerView recyclerView = findViewById(R.id.room_recycler_view);

        // Customizes scrolling so items farther away form center are smaller.
        ScalingScrollLayoutCallback scalingScrollLayoutCallback = new ScalingScrollLayoutCallback();
        recyclerView.setLayoutManager(
                new WearableLinearLayoutManager(this, scalingScrollLayoutCallback));

        recyclerView.setEdgeItemsCenteringEnabled(true);
        recyclerView.setAdapter(appListAdapter);

        Log.i(TAG, "onCreate - return");
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new RoomActivity.MyAmbientCallback();
    }

    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {}
}
