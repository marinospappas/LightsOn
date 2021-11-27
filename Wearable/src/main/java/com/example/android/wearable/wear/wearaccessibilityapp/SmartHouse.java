package com.example.android.wearable.wear.wearaccessibilityapp;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/* ****************************************
 * Smart House class
 * contains list of Rooms
 */
class SmartHouse {

    private static final String TAG = "SmartHouse";

   /// light features;
    private static final int SUPPORTS_DIM = 0b001;
    private static final int SUPPORTS_CT  = 0b010;
    private static final int SUPPORTS_COL = 0b100;
    // room features
    private static final int SUPPORTS_ALL_ON = 0b10000;
    private static final int SUPPORTS_ALL = 0b11111111;

    // light commands
    static final String STR_ON =  "ON";
    static final String STR_OFF = "OFF";
    static final String STR_DIM = "DIM";
    static final String STR_CT = "Col.Temp";
    static final String STR_COL = "Colour";
    static final String STR_ALL_OFF = "All OFF";
    static final String STR_ALL_ON = "All ON";
    static final String STR_ALL_DIM= "All Dim";
    static final String STR_ALL_CT = "All CT";
    static final String STR_ALL_COL = "All Col";

    private static final String STR_DIM_MIN = "DIM Min";
    private static final String STR_DIM_25 =  "DIM 25%";
    private static final String STR_DIM_50 =  "DIM 50%";
    private static final String STR_DIM_75 =  "DIM 75%";
    private static final String STR_DIM_MAX = "DIM Max";
    private static final String[] STR_DIM_LEVEL = {
        STR_DIM_MIN, STR_DIM_25, STR_DIM_50, STR_DIM_75, STR_DIM_MAX
    };
    private static final int[] DIM_IMAGE = {
        R.drawable.dim_mini_i, R.drawable.dim_25i_i, R.drawable.dim_50i_i,
        R.drawable.dim_75i_i, R.drawable.dim_max_i
    };
    private static final int SIZE_DIM_LEVELS = DIM_IMAGE.length;

    private static final String STR_CT_WARMEST = "CT Warmest";
    private static final String STR_CT_WARM = "CT Warm";
    private static final String STR_CT_MED = "CT Medium";
    private static final String STR_CT_COLD = "CT Cold";
    private static final String STR_CT_COLDEST = "CT Coldest";
    private static final String[] STR_CT_LEVEL = {
        STR_CT_COLDEST, STR_CT_COLD, STR_CT_MED, STR_CT_WARM, STR_CT_WARMEST
    };
    private static final int[] CT_IMAGE = {
        R.drawable.ct1_i, R.drawable.ct2_i, R.drawable.ct3_i,
        R.drawable.ct4_i, R.drawable.ct5_i
    };
    private static final int SIZE_CT_LEVELS = CT_IMAGE.length;

    private static final String STR_COL_RED = "Red";
    private static final String STR_COL_ORANGE = "Orange";
    private static final String STR_COL_PINK = "Pink";
    private static final String STR_COL_YELLOW = "Yellow";
    private static final String STR_COL_GREEN = "Green";
    private static final String STR_COL_FOR_GREEN = "Frst Green";
    private static final String STR_COL_SKY_BLUE = "Sky Blue";
    private static final String STR_COL_BLUE = "Blue";
    private static final String STR_COL_LILLY = "Lilly";
    private static final String STR_COL_VIOLET = "Violet";
    private static final String[] STR_COL_NAME = {
            STR_COL_RED, STR_COL_ORANGE, STR_COL_PINK, STR_COL_YELLOW, STR_COL_GREEN,
            STR_COL_FOR_GREEN, STR_COL_SKY_BLUE, STR_COL_BLUE, STR_COL_LILLY, STR_COL_VIOLET
    };
    private static final int[] COL_IMAGE = {
            R.drawable.colred_i, R.drawable.colorng_i, R.drawable.colpink_i,
            R.drawable.colyell_i, R.drawable.colgreen_i, R.drawable.colforgrn_i,
            R.drawable.colskblue_i, R.drawable.colblue_i, R.drawable.collilly_i,
            R.drawable.colviolet_i
    };
    private static final int SIZE_COLOURS = COL_IMAGE.length;

    private String name;
    private List<SmartRoom> rooms;
    private boolean supportsAllOff = false;
    // Lights interface type
    private LightsInterfaceType lightsIf;

    /** need the smartLights object so that we can send commands
    class MyLights takes a generic Class parameter so that it can support various lights interfaces
     must implement LightsInterface
     */
    private MyLights<?> smartLights;

    // Config status
    private boolean homeConfigOK = true;   // will be changed to false if an exception occurs

    static class DataSet {
        String mItemName;
        int mImageId;

        DataSet(String name, int image) {
            mItemName = name;
            mImageId = image;
        }

        String getItemName() {
            return mItemName;
        }

        int getImageId() {
            return mImageId;
        }

        int getViewType() {
            return SampleAppConstants.NORMAL;
        }
    }

    /*
    return config status
     */
    boolean isConfigOK () { return homeConfigOK; }

    /**
     * Constructor - JSON data and App context
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    SmartHouse(JSONObject jHome, Context context) {

        // first initialise rooms and devices
        try {
            name = jHome.getString("homeName");
            String lType = jHome.getString("lightsType");
            if (Objects.equals(lType, "PhilipsHue"))
                lightsIf = LightsInterfaceType.PHILIPS_HUE;
            if (Objects.equals(lType, "LightWave"))
                lightsIf = LightsInterfaceType.LIGHT_WAVE;

            String attributes = jHome.getString("attributes");
            if (attributes.contains("OFF")) supportsAllOff = true;

            rooms = new ArrayList<>();
            JSONArray jRooms = jHome.getJSONArray("rooms");
            for (int i = 0; i < jRooms.length(); ++i) {
                JSONObject jR = jRooms.getJSONObject(i);
                rooms.add(new SmartRoom(jR));
            }
            printHouse();
        } catch (Exception e) {
            Log.e(TAG,"JSON error 1: "+e.getMessage());
            homeConfigOK = false;
        }

        // now initialise the smartLights object and check communication with bridge
        if (lightsIf == LightsInterfaceType.PHILIPS_HUE) {
            Log.i(TAG, "about to create new PhilipsHue object");
            smartLights = new MyLights<>(new PhilipsHue());
            smartLights.setContext(context);
            Log.i(TAG, "about to verify the Bridge");
            smartLights.verify();
        }
        else if (lightsIf == LightsInterfaceType.LIGHT_WAVE) {
            Log.i(TAG, "about to create new LightWave object");
            smartLights = new MyLights<>(new LightWave());
            smartLights.setContext(context);
            Log.i(TAG, "about to verify the Bridge");
            smartLights.verify();
        }
        else {
            Log.e(TAG, "unrecognised lights interface type: " + lightsIf);
            homeConfigOK = false;
        }

    }

    /*
    set current room
     */
    void setCurRoom(String name) {
        for (SmartRoom r : rooms) {
            r.isCurrent = false; // clear previous current room
            r.setCurDevice(""); // clear current device in all rooms
            if (Objects.equals(r.getName(), name)) {
                r.isCurrent = true;
                smartLights.setCurRoomName(name); // also set current room name in smartLights object
            }
        }
        smartLights.setCurDeviceName(""); // also clear current device in our smartLights object
    }

    /*
    get current room
    */
    SmartRoom getCurRoom() {
        for (SmartRoom r : rooms)
            if (r.isCurrent) return r;
        return null;
    }

    /*
    return the list of all room names in the house
    */
    List<DataSet> getRoomSet() {
        List<DataSet> data = new ArrayList<>();
        // first get all rooms
        for (int i=0; i < rooms.size(); ++i)
            data.add(new DataSet(rooms.get(i).name, rooms.get(i).imgId));
        // now add all_off function if supported
        if (supportsAllOff)
            data.add (new DataSet(STR_ALL_OFF, R.drawable.all_off_i));
        return data;
    }

    /*
    return the list of all Dim levels available - this common for all rooms and devices
    */
    List<DataSet> getDimSet() {
        List<DataSet> data = new ArrayList<>();
        for (int i=0; i < SIZE_DIM_LEVELS; ++i)
            data.add (new DataSet(STR_DIM_LEVEL[i], DIM_IMAGE[i]));
        return data;
    }

    /*
    return the list of all Col.Temp. levels available - this common for all rooms and devices
    */
    List<DataSet> getCtSet() {
        List<DataSet> data = new ArrayList<>();
        for (int i=0; i < SIZE_CT_LEVELS; ++i)
            data.add (new DataSet(STR_CT_LEVEL[i], CT_IMAGE[i]));
        return data;
    }

    /*
    return the list of all Colours available - this common for all rooms and devices
    */
    List<DataSet> getColourSet() {
        List<DataSet> data = new ArrayList<>();
        for (int i=0; i < SIZE_COLOURS; ++i)
            data.add (new DataSet(STR_COL_NAME[i], COL_IMAGE[i]));
        return data;
    }

    /*
    all house off method
    */
    void allOff() {
        smartLights.setCurRoomName(name);
        smartLights.setCurDeviceName("");
        smartLights.setCurCommand(STR_ALL_OFF);
        smartLights.houseAllOff ();
    }

    /*
    print house and all rooms
     */
    private void printHouse () {
        Log.i (TAG, "house: "+name);
        Log.i (TAG, "interface: "+lightsIf);
        Log.i (TAG, "supportsAllOff: "+supportsAllOff);
        for (SmartRoom r : rooms)
            r.printRoom();
    }

    /* ****************************
     * SmartRoom sub-class
     * Contains list of Devices
     */
    class SmartRoom {
        private String name;
        private int roomId;
        private int imgId;
        private boolean isCurrent;
        private int roomAttributes;
        private List<SmartDevice> devices;

        /*
        Constructor - JSON object
        */
        SmartRoom(JSONObject jRoom) {
            Log.i(TAG,"SmartRoom Constructor - processing JSON: "+jRoom.toString());
            try {
                name = jRoom.getString("roomName");
                roomId = jRoom.getInt("roomId");

                String icon = jRoom.getString("roomIcon");
                if (Objects.equals(icon, "livingroom"))
                    this.imgId = R.drawable.livingroom1_i;
                else
                if (Objects.equals(icon, "kitchen"))
                    this.imgId = R.drawable.kitchen_i;
                else
                if (Objects.equals(icon, "hallway"))
                    this.imgId = R.drawable.hallway_i;
                else
                if (Objects.equals(icon, "headoffice"))
                    this.imgId = R.drawable.headoffice_i;
                else
                if (Objects.equals(icon, "bedroom"))
                    this.imgId = R.drawable.bedroom_i;
                else
                    this.imgId = R.drawable.circle_i; // dummy image

                String attributes = jRoom.getString("attributes");
                roomAttributes = 0;  // set up to 0 by default
                if (attributes.contains("DIM")) roomAttributes += SUPPORTS_DIM;
                if (attributes.contains("CT")) roomAttributes += SUPPORTS_CT;
                if (attributes.contains("COL")) roomAttributes += SUPPORTS_COL;
                if (attributes.contains("ON")) roomAttributes += SUPPORTS_ALL_ON;
                if (attributes.contains("ALL")) roomAttributes = SUPPORTS_ALL;

                Log.i(TAG, "SmartRoom Constructor - adding room: " + name + " & " + roomId +
                        " & " + roomAttributes + " & " + icon);

                // initialise devices
                devices = new ArrayList<>();
                JSONArray jDevices = jRoom.getJSONArray("devices");
                for (int i=0; i<jDevices.length(); ++i) {
                    JSONObject jD = jDevices.getJSONObject(i);
                    devices.add(new SmartDevice(jD));
                }
            } catch (Exception e) {
                Log.e(TAG,"JSON error 2: "+e.getMessage());
                homeConfigOK = false;
            }
        }

        /*
        all devices in room methods: on, off, dim, ct, colour
         */
        void allOn() {
            smartLights.setCurDeviceName("");
            smartLights.setCurCommand(STR_ALL_ON);
            smartLights.roomAllOn (roomId);
        }
        void allOff() {
            smartLights.setCurDeviceName("");
            smartLights.setCurCommand(STR_ALL_OFF);
            smartLights.roomAllOff (roomId);
        }
        void allDim(int d) {
            smartLights.setCurDeviceName("");  // just in case
            smartLights.setCurCommand(STR_ALL_DIM);
            smartLights.roomAllDim (roomId, d);
        }
        void allCt(int d) {
            smartLights.setCurDeviceName("");
            smartLights.setCurCommand(STR_ALL_CT);
            smartLights.roomAllCt (roomId, d);
        }
        void allCol(int d) {
            smartLights.setCurDeviceName("");
            smartLights.setCurCommand(STR_ALL_COL);
            smartLights.roomAllCol (roomId, d);
        }

        /*
        return room name
         */
        String getName() {
            return name;
        }

        /*
        set current device
        */
        void setCurDevice(String name) {
            for (SmartDevice d : devices) {
                d.isCurrent = false; // clear previous current device
                if (Objects.equals(d.getName(), name)) {
                    d.isCurrent = true;
                    smartLights.setCurDeviceName(name); // also set current device name in smartLights object
                }
            }
        }

        /*
        get room by room name
        */
        SmartDevice getCurDevice() {
            for (SmartDevice d : devices)
                if (d.isCurrent) return d;
            return null;
        }

        /*
        return the list of all device names in this room
        */
        List<DataSet> getDeviceSet() {
            List<DataSet> data = new ArrayList<>();
            for (int i=0; i < devices.size(); ++i)
                data.add(new DataSet(devices.get(i).name, devices.get(i).imgId));
            if ((roomAttributes & SUPPORTS_ALL_ON) > 0)
                data.add (new DataSet(STR_ALL_ON, R.drawable.all_on_i));
            data.add (new DataSet(STR_ALL_OFF, R.drawable.all_off_i));
            if (supportsDim())
                data.add (new DataSet(STR_ALL_DIM, R.drawable.dim_i));
            if (supportsCt())
                data.add (new DataSet(STR_ALL_CT, R.drawable.coltemp_i));
            if (supportsCol())
                data.add (new DataSet(STR_ALL_COL, R.drawable.colour_i));

            return data;
        }

        /*
        check if the room has devices that support colour
         */
        boolean supportsCol () {
            for (SmartDevice d: devices)
                if (d.supportsCol()&&
                   (roomAttributes & SUPPORTS_CT) > 0) return true;
            return false;
        }
        /*
        check if the room has devices that support colour temp.
         */
        boolean supportsCt () {
            for (SmartDevice d: devices)
                if (d.supportsCt() &&
                   (roomAttributes & SUPPORTS_CT) > 0) return true;
            return false;
        }
        /*
        check if the room has devices that support dim
         */
        boolean supportsDim () {
            for (SmartDevice d: devices)
                if (d.supportsDim()&&
                   (roomAttributes & SUPPORTS_DIM) > 0) return true;
            return false;
        }

        /*
        print room and all devices
         */
        void printRoom () {
            Log.i (TAG, "room: "+name+", "+roomId+", " + roomAttributes);
            for (SmartDevice d : devices)
                d.printDevice();
        }

        /* ***********************
         * SmartDevice sub-class
         */
        class SmartDevice {
            private String name;
            private int devId;
            private int imgId;

            private boolean isCurrent;
            private int type;

            /*
            Constructor - JSON object
            */
            SmartDevice(JSONObject jDev) {
                Log.i(TAG,"SmartDevice Constructor - processing JSON: "+jDev.toString());
                try {
                    name = jDev.getString("name");
                    devId = jDev.getInt("id");
                    String attributes = jDev.getString("attributes");
                    type = 0;  // set up this device as just on/off by default
                    imgId = R.drawable.onoff_i;
                    if (attributes.contains("DIM")) {
                        type += SUPPORTS_DIM;
                        imgId = R.drawable.dimmer_i;
                    }
                    if (attributes.contains("CT")) {
                        type += SUPPORTS_CT;
                        imgId = R.drawable.dimmer_i;
                    }
                    if (attributes.contains("COL")) {
                        type += SUPPORTS_COL;
                        imgId = R.drawable.dimmer_col_i;
                    }
                } catch (Exception e) {
                    Log.e(TAG,"JSON error 3: "+e.getMessage());
                    homeConfigOK = false;
                }

                Log.i(TAG, "SmartDevice Constructor - adding device: " + name + " & " + devId + " & " + type);
            }

             /*
            return the list of all options for this device
            */
            List<DataSet> getDeviceOptionsSet() {
                List<DataSet> data = new ArrayList<>();
                data.add (new DataSet(STR_ON, R.drawable.onbtn));
                data.add (new DataSet(STR_OFF, R.drawable.offbtn));
                if (supportsDim())  // add the dim option only for those devices that support it
                    data.add (new DataSet(STR_DIM, R.drawable.dim_i));
                if (supportsCt())  // add the ct option only for those devices that support it
                    data.add (new DataSet(STR_CT, R.drawable.coltemp_i));
                if (supportsCol())  // add the colour option only for those devices that support it
                    data.add (new DataSet(STR_COL, R.drawable.colour_i));

                return data;
            }

            /*
            Basic Smart device commands (on, off, dim, ct, colour)
             */
            void on() {
                smartLights.setCurCommand(STR_ON);
                smartLights.deviceOn (roomId, devId);
            }
            void off() {
                smartLights.setCurCommand(STR_OFF);
                smartLights.deviceOff (roomId, devId);
            }
            void dim(int d) {
                    smartLights.setCurCommand(STR_DIM_LEVEL[d]);
                    smartLights.deviceDim (roomId, devId, d);
            }
            void ct(int d) {
                smartLights.setCurCommand(STR_CT_LEVEL[d]);
                smartLights.deviceCt(roomId, devId, d);
            }
            void colour(int d) {
                smartLights.setCurCommand(STR_COL_NAME[d]);
                smartLights.deviceColour(roomId, devId, d);
            }

            /*
            return device name
             */
            String getName() {
                return name;
            }

            /*
            check device features
             */
            boolean supportsDim() { return ((type & SUPPORTS_DIM) > 0); }
            boolean supportsCt() {
                return ((type & SUPPORTS_CT) > 0);
            }
            boolean supportsCol() {
                return ((type & SUPPORTS_COL) > 0);
            }

            /*
            print device
             */
            void printDevice () {
                Log.i (TAG, "device: "+name+", "+devId+", "+type+" room id "+roomId);
            }
        } // sub-class SmartDevice

    } // sub-class SmartRoom

} // class SmartHouse