/*
 * Support for Philips Hue communication
 * uses HTTP URL Connection
 */
package com.example.android.wearable.wear.wearaccessibilityapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

class PhilipsHue implements LightsInterface {

    // Hue commands
    private static final int CMD_VER = 0;
    private static final int CMD_ON = 1;
    private static final int CMD_OFF = 2;
    private static final int CMD_DIM = 3;
    private static final int CMD_CT = 4;
    private static final int CMD_COLOUR = 5;
    private static final int CMD_ALL_ON = 6;
    private static final int CMD_ALL_OFF = 7;
    private static final int CMD_ALL_DIM = 8;
    private static final int CMD_ALL_CT = 9;
    private static final int CMD_ALL_COL = 10;
    private static final int CMD_HOUSE_OFF = 11;

    private static final String[] philipsColour = {
            "[0.691,0.308]",     // red
            "[0.6,0.36]",        // orange
            "[0.52,0.31]",       // pink
            "[0.5,0.45]",        // yellow
            "[0.35,0.5]",        // green
            "[0.17,0.7]",        // forrest green
            "[0.21,0.25]",       // sky blue
            "[0.15,0.047]",      // blue
            "[0.37,0.2]",        // lilly
            "[0.32,0.12]"       // violet
    };
    private static final String TAG = "PhilipsHue";

    // Bgnd task return codes
    private static final long CMD_RET_SUCCESS  = 0L;
    private static final long CMD_RET_VER_OK   = 1L;
    private static final long CMD_RET_BAD_ARG  = 2L;
    private static final long CMD_RET_NO_COMM  = 3L;
    private static final long CMD_RET_VER_FAIL = 4L;
    private static final long CMD_RET_CMD_FAIL = 5L;
    // private static final long CMD_RET_UNKNOWN  = 99L;


    private static Context appContext;
    private static String curRoomName;
    private static String curDeviceName;
    private static String curCommand;

    private static String hueUrl;
    private static String hueUser;

    private static boolean connectedToBridge;

    /*
    main constructor
    initialise all private vars
    */
    @RequiresApi(api = Build.VERSION_CODES.O)
    PhilipsHue() {

        Log.i(TAG, "initialising PhilipsHue Class");

        /* current room, device and command needed for the "toast" message
           at the end of the execution of each command
         */
        curRoomName = "";
        curDeviceName = "";
        curCommand = "";
        connectedToBridge = false;

        // Now read the config file into as string
        String data;
        try {
            data = new String(Files.readAllBytes(Paths.get(MainActivity.appFilePath+MainActivity.BRIDGE_CONFIG_FILE)));
            Log.i(TAG, "read from Bridge config: "+data);
        } catch(IOException e) {
            Log.e(TAG, "error with the config file: "+e.toString());
            Log.i(TAG, "onCreate - abort");
            return;
        }
        // read Philips Hue settings
        Log.i(TAG, "about to read PhilipsHue settings: "+data);
        try {
            JSONObject jBridge = new JSONObject(data);
            JSONObject jHue = jBridge.getJSONObject(TAG);
            hueUrl = jHue.getString("bridgeURL");
            hueUser = jHue.getString("bridgeUser");
            Log.i(TAG,"initialised setting: hueUrl = " + hueUrl);
            Log.i(TAG,"initialised setting: hueUser = " + hueUser);
        } catch (Exception e) {
            Log.e(TAG, "Problem with Hue config JSON - URL and User not set");
        }


    } // PhilipsHue() constructor

    /*
    Set application context;
     */
    @Override
    public void setContext(Context context) { appContext = context; }
    /*
    Set/Get current room/device/command
     */
    @Override
    public void setCurRoomName (String room) {
        curRoomName = room;
        curDeviceName = "";  // reset the cur dev name as well
        Log.i(TAG, "Current Room/Dev Name set to: " + curRoomName + "/" + curDeviceName);
    }
    @Override
    public void setCurDeviceName (String device) {
        curDeviceName = device;
        Log.i(TAG, "Current Device Name set to: " + curDeviceName);
    }
    @Override
    public void setCurCommand (String command) {
        curCommand = command;
    }

    /*
    Basic Hue functions
     */
    @Override
    public void verify() {
        curCommand = "Verify";
        new SendCmd().execute(CMD_VER);
    }
    @Override
    public void deviceOn(int room, int device) { new SendCmd().execute(CMD_ON, room, device); }
    @Override
    public void deviceOff (int room, int device) { new SendCmd().execute(CMD_OFF, room, device); }
    @Override
    public void deviceDim (int room, int device, int dim) { new SendCmd().execute(CMD_DIM, room, device, dim); }
    @Override
    public void deviceCt (int room, int device, int ct) { new SendCmd().execute(CMD_CT, room, device, ct); }
    @Override
    public void deviceColour (int room, int device, int col) { new SendCmd().execute(CMD_COLOUR, room, device, col); }
    @Override
    public void roomAllOn (int room) { new SendCmd().execute(CMD_ALL_ON, room); }
    @Override
    public void roomAllOff (int room) { new SendCmd().execute(CMD_ALL_OFF, room); }
    @Override
    public void roomAllDim (int room, int dim) { new SendCmd().execute(CMD_ALL_DIM, room, dim); }
    @Override
    public void roomAllCt (int room, int ct) { new SendCmd().execute(CMD_ALL_CT, room, ct); }
    @Override
    public void roomAllCol (int room, int col) { new SendCmd().execute(CMD_ALL_COL, room, col); }
    @Override
    public void houseAllOff () { new SendCmd().execute(CMD_HOUSE_OFF); }

    /*
    Send Command to Bridge in the background using AsyncTask
     */
    private static class SendCmd extends AsyncTask<Integer, Void, Long> {
        // execute task in background
        // parameters: (Command, Room, [Device], [Data1], [Data2], ...)

        @SuppressLint("DefaultLocale")
        protected Long doInBackground(Integer... cmdArgs) {
            Log.i(TAG, "background task started");

            String reqData;
            String urlSuffix;
            String httpMethod;

            // check for valid arguments
            if (cmdArgs.length < 1) return CMD_RET_BAD_ARG;

            // if not connected to Bridge force Verify
            if (!Objects.equals(cmdArgs[0],CMD_VER) && !connectedToBridge) {
                Log.i(TAG, "Not Connected to Bridge, " + cmdArgs[0] + " command will be changed to CMD_VER");
                cmdArgs[0] = CMD_VER;
            }

            int philipsDimLevel;
            int philipsCtLevel;

            switch (cmdArgs[0]) {
                case CMD_VER:
                    urlSuffix = "groups/0";
                    httpMethod = "GET";
                    reqData = null;
                    break;
                case CMD_ON:
                    if (cmdArgs.length < 3) return CMD_RET_BAD_ARG;
                    // 2nd parameter (room) ignored here
                    urlSuffix = String.format("lights/%d/state", cmdArgs[2]);
                    httpMethod = "PUT";
                    reqData = "{\"on\":true}";
                    break;
                case CMD_OFF:
                    if (cmdArgs.length < 3) return CMD_RET_BAD_ARG;
                    // 2nd parameter (room) ignored here
                    urlSuffix = String.format("lights/%d/state", cmdArgs[2]);
                    httpMethod = "PUT";
                    reqData = "{\"on\":false}";
                    break;
                case CMD_DIM:
                    if (cmdArgs.length < 4) return CMD_RET_BAD_ARG;
                    // 2nd parameter (room) ignored here
                    urlSuffix = String.format("lights/%d/state", cmdArgs[2]);
                    httpMethod = "PUT";
                    // convert 0-4 range to Philips Dim range 2 - 254
                    philipsDimLevel = cmdArgs[3] * 63 + 2;
                    reqData = String.format("{\"bri\":%d}", philipsDimLevel);
                    break;
                case CMD_CT:
                    if (cmdArgs.length < 4) return CMD_RET_BAD_ARG;
                    // 2nd parameter (room) ignored here
                    urlSuffix = String.format("lights/%d/state", cmdArgs[2]);
                    httpMethod = "PUT";
                    // convert 0-4 range to Philips CT range 153 - 500
                    philipsCtLevel = cmdArgs[3] * 87 + 153;
                    reqData = String.format("{\"ct\":%d}", philipsCtLevel);
                    break;
                case CMD_COLOUR:
                    if (cmdArgs.length < 4) return CMD_RET_BAD_ARG;
                    // 2nd parameter (room) ignored here
                    urlSuffix = String.format("lights/%d/state", cmdArgs[2]);
                    httpMethod = "PUT";
                    reqData = String.format("{\"xy\":%s}", philipsColour[cmdArgs[3]]);
                    break;
                case CMD_ALL_ON:
                    if (cmdArgs.length < 2) return CMD_RET_BAD_ARG;
                    urlSuffix = String.format("groups/%d/action", cmdArgs[1]);
                    httpMethod = "PUT";
                    reqData = "{\"on\":true}";
                    break;
                case CMD_ALL_OFF:
                    if (cmdArgs.length < 2) return CMD_RET_BAD_ARG;
                    urlSuffix = String.format("groups/%d/action", cmdArgs[1]);
                    httpMethod = "PUT";
                    reqData = "{\"on\":false}";
                    break;
                case CMD_ALL_DIM:
                    if (cmdArgs.length < 3) return CMD_RET_BAD_ARG;
                    urlSuffix = String.format("groups/%d/action", cmdArgs[1]);
                    httpMethod = "PUT";
                    // convert 0-4 range to Philips Dim range 2 - 254
                    philipsDimLevel = cmdArgs[2] * 63 + 2;
                    reqData = String.format("{\"bri\":%d}", philipsDimLevel);
                    break;
                case CMD_ALL_CT:
                    if (cmdArgs.length < 3) return CMD_RET_BAD_ARG;
                    urlSuffix = String.format("groups/%d/action", cmdArgs[1]);
                    httpMethod = "PUT";
                    // convert 0-4 range to Philips CT range 153 - 500
                    philipsCtLevel = cmdArgs[2] * 87 + 153;
                    reqData = String.format("{\"ct\":%d}", philipsCtLevel);
                    break;
                case CMD_ALL_COL:
                    if (cmdArgs.length < 3) return CMD_RET_BAD_ARG;
                    urlSuffix = String.format("groups/%d/action", cmdArgs[1]);
                    httpMethod = "PUT";
                    reqData = String.format("{\"xy\":%s}", philipsColour[cmdArgs[2]]);
                    break;
                case CMD_HOUSE_OFF:
                    urlSuffix = "groups/0/action";
                    httpMethod = "PUT";
                    reqData = "{\"on\":false}";
                    break;
                default:
                    Log.e(TAG, "Background task - unsupported command" + cmdArgs[0]);
                    return CMD_RET_BAD_ARG;
            }
            // transmit command
            String httpString = String.format("%s/%s/%s", hueUrl, hueUser, urlSuffix);
            Log.i(TAG, "(bgnd) will send command " + httpMethod + " " + httpString + " " + reqData);
            try {
                URL url = new URL(httpString);
                // open http connection and send request
                HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
                if (Objects.equals(httpMethod, "PUT")) { httpUrlConnection.setRequestMethod(httpMethod); }
                if (reqData != null) {
                    httpUrlConnection.setDoOutput(true);
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpUrlConnection.getOutputStream());
                    outputStreamWriter.write(reqData);
                    outputStreamWriter.close();
                }
                // receive response
                BufferedReader inBufferedReader = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream()));
                String inputLine;
                String response = null;
                while ((inputLine = inBufferedReader.readLine()) != null) {
                    response = inputLine;
                    Log.i(TAG, "received response: " + response);
                }
                httpUrlConnection.disconnect();
                Log.i(TAG, "(bgnd) sent command " + httpMethod + " " + httpString + " " + reqData);

                // Verify result
                if (response == null) {
                    Log.e(TAG, "Bgnd task: command not sent or response not received");
                    return CMD_RET_NO_COMM;
                }
                Log.i(TAG, "Bgnd task: received response: " + response);

                // checking Verify command first
                if (cmdArgs[0] == CMD_VER) {
                    if (response.startsWith("{\"name\":\"Group 0\"")) {
                        Log.i(TAG, "Bgnd task: verify command completed successfully");
                        connectedToBridge = true;
                        return CMD_RET_VER_OK;
                    }
                    else {
                        Log.e(TAG, "Bgnd task: verify command failed");
                        connectedToBridge = false;
                        return CMD_RET_VER_FAIL;

                    }
                }

                // check other commands
                if (response.startsWith("[{\"success\":")) {
                    Log.i(TAG, "Bgnd task: command completed successfully");
                    return CMD_RET_SUCCESS;
                }
                else {
                    Log.e(TAG, "Bgnd task: command Failed");
                    return CMD_RET_CMD_FAIL;
                }

            } catch (IOException e) {
                Log.e(TAG, "(bgnd) IO error " + e.getMessage());
            } catch (SecurityException e) {
                Log.e(TAG, "(bgnd) security error " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "(bgnd) error " + e.toString());
            }
            Log.e(TAG, "background task failed");
            connectedToBridge = false;
            return CMD_RET_NO_COMM;
        } // doInBackground

        // pop up toast after execution is completed
        // only show the popup if curCommand is not empty
        protected void onPostExecute(Long result) {
            Toast toast;
            switch (result.intValue()) {
                case (int)CMD_RET_SUCCESS:
                    toast = Toast.makeText(appContext,curRoomName+" "+curDeviceName+" "+curCommand,Toast.LENGTH_LONG);
                    toast.show();
                    break;
                case (int)CMD_RET_VER_OK:
                    toast = Toast.makeText(appContext,"Ready!", Toast.LENGTH_LONG);
                    toast.show();
                    break;
                case (int)CMD_RET_VER_FAIL:
                    toast = Toast.makeText(appContext,"Failed to connect to the Bridge", Toast.LENGTH_LONG);
                    toast.show();
                    break;
                case (int)CMD_RET_CMD_FAIL:
                    toast = Toast.makeText(appContext,"Command " + curCommand + " Failed", Toast.LENGTH_LONG);
                    toast.show();
                    break;
                case (int)CMD_RET_BAD_ARG:
                    toast = Toast.makeText(appContext,"Something went wrong with this Command", Toast.LENGTH_LONG);
                    toast.show();
                    break;
                case (int)CMD_RET_NO_COMM:
                    toast = Toast.makeText(appContext,"ERROR! Please check WiFi", Toast.LENGTH_LONG);
                    toast.show();
                    break;
                default:
                    toast = Toast.makeText(appContext,"Unknown Error", Toast.LENGTH_LONG);
                    toast.show();
            }
        } // onPostExecute

    } // class SendCmd

} // class PhilipsHue
