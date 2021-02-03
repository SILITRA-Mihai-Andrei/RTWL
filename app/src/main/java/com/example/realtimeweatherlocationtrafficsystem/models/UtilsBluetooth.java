package com.example.realtimeweatherlocationtrafficsystem.models;

import android.content.Context;
import android.widget.Toast;

import java.util.UUID;

public class UtilsBluetooth {

    public final static int STATE_CONNECTING = 1;
    public final static int STATE_CONNECTED = 2;
    public final static int STATE_CONNECTION_FAILED = 3;
    public final static int STATE_READING_WRITING_FAILED = 4;
    public final static int STATE_MESSAGE_RECEIVED = 5;
    public final static int STATE_MESSAGE_SEND = 6;

    public final static String MESSAGE_TIME_END = "] \n";

    public final static String MUST_GET_LOCATION = "#";
    public final static String MUST_GET_LOCATION_STRING = "GPS module not working! \nMobile location will be used.";
    public final static int BLUETOOTH_BUFFER_SIZE = 128;
    public final static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public final static String MAIN_BLUETOOTH_DEVICE = "HC-05";
    public final static String INVALID_GPS_COORDINATES = "0.0 0.0";
    public final static String BLUETOOTH_RECEIVE_DELIMITER = "\r";
    public final static String BLUETOOTH_RECEIVE_STATE_DELIMITER = ":";

    public final static String COMMAND_GET_GPS_COORDINATES_INTEGER = "3";
    public final static String COMMAND_DISABLE_GET_GPS_COORDINATES_INTEGER = "4";

    public final static String BLUETOOTH_COMMANDS_LIST =
            "-1\t\t|\t\trs\t\t\t|\t\treset\nSoft reset Arduino\n------------------------------------\n" +
            " 0\t\t|\t\tgr\t\t|\t\tping\nGet response from Arduino\n------------------------------------\n" +
            " 1\t\t|\t\tgd\t\t|\t\tget data\nGet sensors value\n------------------------------------\n" +
            " 2\t\t|\t\tpd\t\t|\t\tproxy fail\nWarn proxy sensor failure\n------------------------------------\n" +
            " 3\t\t|\t\tgc\t\t|\t\tget coordinates\nSet Arduino to send GPS coordinates every second\n------------------------------------\n" +
            " 4\t\t|\t\tdgc\t\t|\t\tdisable get coordinates\nDisable get coordinates feature\n------------------------------------\n";

    public final static String STATE_START = "RTWL:START"+BLUETOOTH_RECEIVE_DELIMITER;
    public final static String STATE_PING = "PONG!"+BLUETOOTH_RECEIVE_DELIMITER;
    //indexes of STATES_STRING for every state of Arduino microprocessor
    public final static int STATE_ERROR = 0;
    public final static int STATE_GPS_NOT_WORKING = 2;
    public final static int STATE_GPS_INVALID_DATA = 4;
    public final static int STATE_TEMP_HUM_SENSOR_NOT_WORKING = 6;
    public final static int STATE_TEMP_INVALID_DATA = 8;
    public final static int STATE_HUM_INVALID_DATA = 10;
    public final static int STATE_PROXY_SENSOR_NOT_WORKING = 12;
    public final static int STATE_WIND_SENSOR_NOT_WORKING = 14;
    public final static int STATE_WIND_SENSOR_INVALID_DATA = 16;
    public final static int STATE_WEATHER_CODE_INVALID = 18;
    public final static int STATE_AIR_SENSOR_NOT_WORKING = 20;
    public final static int STATE_AIR_SENSOR_INVALID_DATA = 22;
    public final static int STATE_GET_GPS_COORDINATES_ENABLED = 24;
    public final static int STATE_GET_GPS_COORDINATES_DISABLED = 26;
    public final static int UNKNOWN_COMMAND = 28;
    public final static String STATES_STRING =
            "E"+BLUETOOTH_RECEIVE_DELIMITER+
            "G"+BLUETOOTH_RECEIVE_STATE_DELIMITER+
            "g"+BLUETOOTH_RECEIVE_STATE_DELIMITER+
            "D"+BLUETOOTH_RECEIVE_STATE_DELIMITER+
            "t"+BLUETOOTH_RECEIVE_STATE_DELIMITER+
            "h"+BLUETOOTH_RECEIVE_STATE_DELIMITER+
            "P"+BLUETOOTH_RECEIVE_DELIMITER+
            "W"+BLUETOOTH_RECEIVE_DELIMITER+
            "w"+BLUETOOTH_RECEIVE_STATE_DELIMITER+
            "c"+BLUETOOTH_RECEIVE_STATE_DELIMITER+
            "A"+BLUETOOTH_RECEIVE_STATE_DELIMITER+
            "a"+BLUETOOTH_RECEIVE_STATE_DELIMITER+
            "L"+BLUETOOTH_RECEIVE_STATE_DELIMITER+
            "l"+BLUETOOTH_RECEIVE_STATE_DELIMITER+
            "u"+BLUETOOTH_RECEIVE_STATE_DELIMITER;

    public static String getReceivedMessage(String message, Context context) {
        if (message == null) return null;
        /* first string is the message which is displayed
         * second string is the action which must be treated; null means no action * */
        String result = "";
        String time = "[" + Utils.getTime() + MESSAGE_TIME_END;
        if (message.contains(STATE_START)) {
            result = "RTWL System started\n";
        }
        else if(message.contains(STATE_PING)){
            result = "Arduino is active.\n";
        }
        else if(message.contains(".")){
            String[] string = message.split(" ");
            if(string.length==8){
                if(message.contains(INVALID_GPS_COORDINATES+" ")){
                    result = MUST_GET_LOCATION;
                }
                else{
                    result = "Region: " + string[0] + " " + string[1];
                }
                result +=
                          "\n\t-\t" + UtilsGoogleMaps.getWeatherString(UtilsGoogleMaps.getWeatherStringIndex(Integer.parseInt(string[2])), context)
                        + "\n\t-\tTemperature: " + string[3]
                        + "\n\t-\tHumidity: "+string[4]
                        + "\n\t-\tAir: "+string[5]
                        + "\n\t-\tSpeed: "+(string[6].equals("-1") ? "unknown":string[6])
                        + "\n\t-\tDirection: "+(string[7].contains("U") ? "unknown":string[7])+"\n";
                result = result + "@" + message;
            }
        }
        else if(message.contains(STATES_STRING.substring(STATE_ERROR, STATE_ERROR+2))){
            result = "An error occurred to Arduino microprocessor\n";
        }
        else if(message.contains(STATES_STRING.substring(STATE_GPS_NOT_WORKING, STATE_GPS_NOT_WORKING+2))){
            result = "GPS is not working!\n";
        }
        else if(message.contains(STATES_STRING.substring(STATE_GPS_INVALID_DATA, STATE_GPS_INVALID_DATA+2))){
            result = "Invalid data from GPS\n";
        }
        else if(message.contains(STATES_STRING.substring(STATE_TEMP_HUM_SENSOR_NOT_WORKING, STATE_TEMP_HUM_SENSOR_NOT_WORKING+2))){
            result = "Temperature and humidity sensor is not working!\n";
        }
        else if(message.contains(STATES_STRING.substring(STATE_TEMP_INVALID_DATA, STATE_TEMP_INVALID_DATA+2))){
            result = "Invalid temperature value! Received value: " + message.split(":")[1];
        }
        else if(message.contains(STATES_STRING.substring(STATE_HUM_INVALID_DATA, STATE_HUM_INVALID_DATA+2))){
            result = "Invalid humidity value! Received value: " + message.split(":")[1];
        }
        else if(message.contains(STATES_STRING.substring(STATE_PROXY_SENSOR_NOT_WORKING, STATE_PROXY_SENSOR_NOT_WORKING+2))){
            result = "Proxy sensor is not working!\n";
        }
        else if(message.contains(STATES_STRING.substring(STATE_WIND_SENSOR_NOT_WORKING, STATE_WIND_SENSOR_NOT_WORKING+2))){
            result = "Wind sensor not working!\n";
        }
        else if(message.contains(STATES_STRING.substring(STATE_WIND_SENSOR_INVALID_DATA, STATE_WIND_SENSOR_INVALID_DATA+2))){
            result = "Invalid wind sensor value! Received value: " + message.split(":")[1];
        }
        else if(message.contains(STATES_STRING.substring(STATE_WEATHER_CODE_INVALID, STATE_WEATHER_CODE_INVALID+2))){
            result = "Invalid weather code value Received value: " + message.split(":")[1];
        }
        else if(message.contains(STATES_STRING.substring(STATE_AIR_SENSOR_NOT_WORKING, STATE_AIR_SENSOR_NOT_WORKING+2))){
            result = "Air quality sensor is not working\n";
        }
        else if(message.contains(STATES_STRING.substring(STATE_AIR_SENSOR_INVALID_DATA, STATE_AIR_SENSOR_INVALID_DATA+2))){
            result = "Invalid air quality sensor value Received value: " + message.split(":")[1];
        }
        else if(message.contains(STATES_STRING.substring(STATE_GET_GPS_COORDINATES_ENABLED, STATE_GET_GPS_COORDINATES_ENABLED+2))){
            result = "Get coordinates every second feature is enabled!\n";
        }
        else if(message.contains(STATES_STRING.substring(STATE_GET_GPS_COORDINATES_DISABLED, STATE_GET_GPS_COORDINATES_DISABLED+2))){
            result = "Get coordinates every second feature is disabled!\n";
        }
        else if(message.contains(STATES_STRING.substring(UNKNOWN_COMMAND, UNKNOWN_COMMAND+2))){
            result = "Unknown command!\n";
        }
        else result = null;
        if(result != null && result.length() == 0)
            return null;
        if(result!=null) {
            return time + result;
        }
        return null;
    }
}