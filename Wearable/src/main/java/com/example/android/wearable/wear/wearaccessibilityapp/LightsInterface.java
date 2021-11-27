/*
 * Support for Philips Hue communication
 * uses HTTP URL Connection
 */
package com.example.android.wearable.wear.wearaccessibilityapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public interface LightsInterface {

    // Method signatures
    void setContext(Context context);
    void setCurRoomName(String roomName);
    void setCurDeviceName(String deviceName);
    void setCurCommand(String command);
    void verify();
    void deviceOn(int roomId, int deviceId);
    void deviceOff(int roomId, int deviceId);
    void deviceDim(int roomId, int deviceId, int dimLevel);
    void deviceCt(int roomId, int deviceId, int ctLevel);
    void deviceColour(int roomId, int deviceId, int colour);
    void roomAllOn(int roomId);
    void roomAllOff(int roomId);
    void roomAllDim(int roomId, int dimLevel);
    void roomAllCt(int roomId, int ctLevel);
    void roomAllCol(int roomId, int colour);
    void houseAllOff();

} // interface LightsInterface