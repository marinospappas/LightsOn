package com.example.android.wearable.wear.wearaccessibilityapp;

import android.content.Context;

public class MyLights <C extends LightsInterface> implements LightsInterface {

    private C lights;

    // Constructor
    // Will be called with an Object of either PhilipsHue or LightWave Class
    // depending on the "lightsType" setting in homeConfig.json
    MyLights(C obj) {
        lights = obj;
    }


    @Override
    public void setContext(Context context) {
        lights.setContext(context);
    }

    @Override
    public void setCurRoomName(String roomName) {
        lights.setCurRoomName(roomName);
    }

    @Override
    public void setCurDeviceName(String deviceName) {
        lights.setCurDeviceName(deviceName);
    }

    @Override
    public void setCurCommand(String command) {
        lights.setCurCommand(command);
    }

    @Override
    public void verify() {
        lights.verify();
    }

    @Override
    public void deviceOn(int roomId, int deviceId) {
        lights.deviceOn(roomId, deviceId);
    }

    @Override
    public void deviceOff(int roomId, int deviceId) {
        lights.deviceOff(roomId, deviceId);
    }

    @Override
    public void deviceDim(int roomId, int deviceId, int dimLevel) {
        lights.deviceDim(roomId, deviceId, dimLevel);
    }

    @Override
    public void deviceCt(int roomId, int deviceId, int ctLevel) {
        lights.deviceCt(roomId, deviceId, ctLevel);
    }

    @Override
    public void deviceColour(int roomId, int deviceId, int colour) {
        lights.deviceColour(roomId, deviceId, colour);
    }

    @Override
    public void roomAllOn(int roomId) {
        lights.roomAllOn(roomId);
    }

    @Override
    public void roomAllOff(int roomId) {
        lights.roomAllOff(roomId);
    }

    @Override
    public void roomAllDim(int roomId, int dimLevel) {
        lights.roomAllDim(roomId, dimLevel);
    }

    @Override
    public void roomAllCt(int roomId, int ctLevel) {
        lights.roomAllCt(roomId, ctLevel);
    }

    @Override
    public void roomAllCol(int roomId, int colour) {
        lights.roomAllCol(roomId, colour);
    }

    @Override
    public void houseAllOff() {
        lights.houseAllOff();
    }
}
