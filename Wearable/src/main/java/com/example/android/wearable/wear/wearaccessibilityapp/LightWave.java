package com.example.android.wearable.wear.wearaccessibilityapp;

/*
Support for LightWave smart switches communication
 */
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.json.JSONObject;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class LightWave implements LightsInterface {

    // Lightwave commands
    private static final int CMD_VER = 0;
    private static final int CMD_ON = 1;
    private static final int CMD_OFF = 2;
    private static final int CMD_DIM = 3;
    private static final int CMD_ALL_OFF = 4;
    // private static final int CMD_FLIP_FLOP = 5;

    private static int serverPort = 9760; // load default value
    private static int clientPort = 9761; // load default value
    private static int responseTimeout = 500; // load default value
    private static final String TAG = "LightWave";

    // Bgnd task return codes
    private static final long CMD_RET_SUCCESS  = 0L;
    private static final long CMD_RET_VER_OK   = 1L;
    private static final long CMD_RET_BAD_ARG  = 2L;
    private static final long CMD_RET_NO_COMM  = 3L;
    private static final long CMD_RET_VER_FAIL = 4L;
    private static final long CMD_RET_CMD_FAIL = 5L;
    // private static final long CMD_RET_UNKNOWN  = 99L;

    // private Boolean networkInitialised = false;
    private static int txnId;
    // private static Boolean registered;
    private static DatagramSocket outSocket, inSocket;
    private static InetAddress serverAddress;

    private static Context appContext;
    private static String curRoomName;
    private static String curDeviceName;
    private static String curCommand;

    private static boolean connectedToBridge;

    /*
    main constructor
    initialise all private vars
    */
    @RequiresApi(api = Build.VERSION_CODES.O)
    LightWave() {

        Log.i(TAG, "initialising LightWave Class");

        txnId = 0; // LW transaction id
        String bcastAddress = "255.255.255.255"; // load default value

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
        Log.i(TAG, "about to read LightWave settings: "+data);
        try {
            JSONObject jBridge = new JSONObject(data);
            JSONObject jHue = jBridge.getJSONObject(TAG);
            serverPort = jHue.getInt("serverPort");
            clientPort = jHue.getInt("clientPort");
            responseTimeout = jHue.getInt("responseTimeout");
            Log.i(TAG,"initialised setting: serverPort = " + serverPort);
            Log.i(TAG,"initialised setting: clientPort = " + clientPort);
            Log.i(TAG,"initialised setting: responseTimeout = " + responseTimeout);
            Log.i(TAG,"initialised setting: bcastAddress = " + bcastAddress);
        } catch (Exception e) {
            Log.i(TAG, "Problem with Hue config JSON - URL and User not set");
        }

        try {
            outSocket = new DatagramSocket();    // transmit socket and address
            serverAddress = InetAddress.getByName(bcastAddress);

            inSocket = new DatagramSocket(clientPort);  // receive socket
            inSocket.setBroadcast(true);
            inSocket.setSoTimeout(responseTimeout);
            Log.i(TAG, "completed LW initialisation");
        } catch (SocketException e) {
            Log.e(TAG, "socket error " + e.getMessage());
        } catch (UnknownHostException e) {
            Log.e(TAG, "unknown host error " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "error " + e.toString());
        }
    } // Lightwave() constructor

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
    public void deviceOn (int room, int device) {
        new SendCmd().execute(CMD_ON, room, device);
    }
    @Override
    public void deviceOff (int room, int device) {
        new SendCmd().execute(CMD_OFF, room, device);
    }
    @Override
    public void deviceDim (int room, int device, int dim) {
        new SendCmd().execute(CMD_DIM, room, device, dim);
    }
    @Override
    public void roomAllOff (int room) {
        new SendCmd().execute(CMD_ALL_OFF, room);
    }

    /** the below are not supported by LightWave */
    @Override
    public void deviceCt(int roomId, int deviceId, int ctLevel) {}
    @Override
    public void deviceColour(int roomId, int deviceId, int colour) {}
    @Override
    public void roomAllOn(int roomId) {}
    @Override
    public void roomAllDim(int roomId, int dimLevel) {}
    @Override
    public void roomAllCt(int roomId, int ctLevel) {}
    @Override
    public void roomAllCol(int roomId, int colour) {}
    @Override
    public void houseAllOff() {}
    /********************************************/

    /*
    Send Command to LightWave in the background using AsyncTask
     */
    private static class SendCmd extends AsyncTask<Integer, Void, Long> {

        // execute task in background
        @SuppressLint("DefaultLocale")
        protected Long doInBackground(Integer... cmdArgs) {
            Log.i(TAG, "background task started");
            // this is the command string
            String cmdString;

            // check for valid arguments
            if (cmdArgs.length < 1) return CMD_RET_BAD_ARG;

            // if not connected to Bridge force Verify
            if (!Objects.equals(cmdArgs[0],CMD_VER) && !connectedToBridge) {
                Log.i(TAG, "Not Connected to Bridge, " + cmdArgs[0] + " command will be changed to CMD_VER");
                cmdArgs[0] = CMD_VER;
            }

            switch (cmdArgs[0]) {
                case CMD_VER:
                    cmdString = String.format("%d,!F*p", ++txnId);
                    break;
                case CMD_ON:
                    if (cmdArgs.length < 3) return CMD_RET_BAD_ARG;
                    cmdString = String.format("%d,!R%dD%dF1", ++txnId, cmdArgs[1], cmdArgs[2]);
                    break;
                case CMD_OFF:
                    if (cmdArgs.length < 3) return CMD_RET_BAD_ARG;
                    cmdString = String.format("%d,!R%dD%dF0", ++txnId, cmdArgs[1], cmdArgs[2]);
                    break;
                case CMD_DIM:
                    if (cmdArgs.length < 4) return CMD_RET_BAD_ARG;
                    cmdString = String.format("%d,!R%dD%dFdP%d", ++txnId, cmdArgs[1], cmdArgs[2], cmdArgs[3]);
                    break;
                case CMD_ALL_OFF:
                    if (cmdArgs.length < 2) return CMD_RET_BAD_ARG;
                    cmdString = String.format("%d,!R%dFa", ++txnId, cmdArgs[1]);
                    break;
                default:
                    Log.i(TAG, "(bgnd) unrecognised command " + cmdArgs[0]);
                    return CMD_RET_BAD_ARG;
            }
            Log.i(TAG, "(bgnd) will send command " + cmdString);
            // transmit command
            byte[] outBuf = cmdString.getBytes();
            try {
                DatagramPacket outPacket = new DatagramPacket(outBuf, outBuf.length, serverAddress, serverPort);
                String s = new String(outPacket.getData(), 0, outPacket.getLength());
                outSocket.send(outPacket);
                Log.i(TAG, "(bgnd) sent command " + s + " to " + outPacket.getAddress().getHostAddress() + ":" + outPacket.getPort());
                // and check for reply - non-blocking read
                byte[] inBuf = new byte[512];
                DatagramPacket inPacket = new DatagramPacket(inBuf, 256);
                String stringIn;
                try {
                    inSocket.receive(inPacket);
                    serverAddress = inPacket.getAddress();   // update LightWave address from response
                    stringIn = new String(inPacket.getData(), 0, inPacket.getLength());
                    Log.i(TAG, "(bgnd) received response " + stringIn + " from " + inPacket.getAddress().getHostAddress());
                } catch (SocketTimeoutException e) {
                    Log.i(TAG, "(bgnd) read timeout " + e.getMessage());
                    return CMD_RET_NO_COMM;
                }

                // now check response
                if (stringIn.length() == 0) {
                    Log.i(TAG, "(bgnd) received no data from bridge");
                    return CMD_RET_NO_COMM;
                }
                String[] token = stringIn.split(",");
                if (token.length != 2) {
                    if (Objects.equals(curCommand, "Verify")) {
                        Log.i(TAG, "(bgnd) Verify command failed");
                        return CMD_RET_VER_FAIL;
                    }
                    else {
                        Log.i(TAG, "(bgnd) command "+ curCommand + " failed");
                        return CMD_RET_CMD_FAIL;
                    }
                }
                else {
                    if (Objects.equals(curCommand, "Verify")) {
                        if (txnId == Integer.parseInt(token[0]) && Objects.equals("?V=", token[1].substring(0, 3))) {
                            connectedToBridge = true;
                            Log.i(TAG, "(bgnd) Verify command succeeded");
                            return CMD_RET_VER_OK;
                        }
                        else {
                            Log.i(TAG, "(bgnd) Verify command bad response");
                            return CMD_RET_VER_FAIL;
                        }
                    }
                    else {
                        if (txnId == Integer.parseInt(token[0]) && Objects.equals("OK", token[1].substring(0, 2))) {
                            Log.i(TAG, "(bgnd) command " + curCommand + " succeeded");
                            return CMD_RET_SUCCESS;
                        }
                        else {
                            Log.i(TAG, "(bgnd) command "+ curCommand + " bad response");
                            return CMD_RET_CMD_FAIL;
                        }
                    }
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

} // class LightWave
