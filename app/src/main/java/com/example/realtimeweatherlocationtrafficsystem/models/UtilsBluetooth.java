package com.example.realtimeweatherlocationtrafficsystem.models;

import android.content.Context;

import com.example.realtimeweatherlocationtrafficsystem.services.BluetoothService;

import java.util.UUID;

/**
 * Class that stores static values and methods that helps for Bluetooth communication.
 */
public class UtilsBluetooth {

    // Define the Bluetooth connection states
    // There are used usually for handler messages
    public final static int STATE_CONNECTING = 1;                       // connecting to Bluetooth device
    public final static int STATE_CONNECTED = 2;                        // connected to Bluetooth device
    public final static int STATE_CONNECTION_FAILED = 3;                // couldn't connect to Bluetooth device
    public final static int STATE_READING_WRITING_FAILED = 4;           // error in reading or writing
    public final static int STATE_MESSAGE_RECEIVED = 5;                 // received a valid message from Bluetooth device
    public final static int STATE_MESSAGE_SEND = 6;                     // thread successfully send a message to Bluetooth device
    public final static int STATE_FAILED_RECEIVING_MESSAGE_LIMIT = 7;   // received too many invalid messages from Bluetooth device

    public final static String MESSAGE_TIME_START = "[";                // the start String before current time of the message
    public final static String MESSAGE_TIME_END = "] \n";               // the String after current time of the message

    public final static String MUST_GET_LOCATION = "#";                 // indicates that the message contains invalid GPS coordinates and must be replaced
    public final static int BLUETOOTH_BUFFER_SIZE = 1024;               // the maximum buffer size that is used to receive messages from Bluetooth device
    public final static int BLUETOOTH_ONE_RECORD_SIZE = 40;             // the maximum length a valid message must have
    public final static UUID MY_UUID =                                  // default Bluetooth UUID
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public final static String MAIN_BLUETOOTH_DEVICE = "HC-05";         // the most common Bluetooth device used for this kind of system - used for auto select
    public final static String MAIN_AUXILIARY_BLUETOOTH_DEVICE = "HC-06"; // another common Bluetooth device used for this kind of system - used for auto select
    public final static String INVALID_GPS_COORDINATES = "0.0 0.0";     // GPS coordinates that indicates the GPS module is not working
    public final static String INVALID_GPS_COORDINATES1 = ".0 0.0";     // GPS coordinates that indicates the GPS module is not working
    public final static String DIRECTION_UNKNOWN = "unknown";           // replacement for unknown direction
    public final static String MSG_REGION_START = "Region:";            // region string used before GPS coordinates of the region

    // There values must have one single character
    public final static String BLUETOOTH_RECEIVE_DELIMITER = "\r";      // delimiter for messages received from Bluetooth device
    public final static String BLUETOOTH_RECEIVE_STATE_DELIMITER = ":"; // delimiter for state messages received from Bluetooth device

    // List of available commands to send for Bluetooth device
    public final static String BLUETOOTH_COMMANDS_LIST =
            "-1\t\t|\t\trs\t\t\t|\t\treset\nSoft reset Arduino\n------------------------------------\n" +
                    " 0\t\t|\t\tgr\t\t|\t\tping\nGet response from Arduino\n------------------------------------\n" +
                    " 1\t\t|\t\tgd\t\t|\t\tget data\nGet sensors value\n------------------------------------\n" +
                    " 2\t\t|\t\tpd\t\t|\t\tproxy fail\nWarn proxy sensor failure\n------------------------------------\n" +
                    " 3\t\t|\t\tgc\t\t|\t\tget coordinates\nSet Arduino to send GPS coordinates every second\n------------------------------------\n" +
                    " 4\t\t|\t\tdgc\t\t|\t\tdisable get coordinates\nDisable get coordinates feature\n------------------------------------\n";

    // Define Bluetooth state messages received from Bluetooth device
    public final static String STATE_START =                            // the hardware system started / restarted
            "RTWL:START" + BLUETOOTH_RECEIVE_DELIMITER;
    public final static String STATE_PING =                             // state sent by Bluetooth device after received a PING command
            "PONG!" + BLUETOOTH_RECEIVE_DELIMITER;
    public final static int STATE_ERROR = 0;                            // there was an error or an invalid command sent
    public final static int STATE_GPS_NOT_WORKING = 2;                  // GPS module is not working
    public final static int STATE_GPS_INVALID_DATA = 4;                 // GPS module sent invalid coordinates
    public final static int STATE_TEMP_HUM_SENSOR_NOT_WORKING = 6;      // the temperature and humidity sensor is not working
    public final static int STATE_TEMP_INVALID_DATA = 8;                // the temperature value is not valid
    public final static int STATE_HUM_INVALID_DATA = 10;                // the humidity value is not valid
    public final static int STATE_PROXY_SENSOR_NOT_WORKING = 12;        // the proximity sensor is not working
    public final static int STATE_WIND_SENSOR_NOT_WORKING = 14;         // the wind sensor is not working
    public final static int STATE_WIND_SENSOR_INVALID_DATA = 16;        // the wind sensor sent invalid data
    public final static int STATE_WEATHER_CODE_INVALID = 18;            // the weather code is not valid (must between 100 and 499)
    public final static int STATE_AIR_SENSOR_NOT_WORKING = 20;          // the air quality sensor is not working
    public final static int STATE_AIR_SENSOR_INVALID_DATA = 22;         // the air quality sensor sent invalid data
    public final static int STATE_GET_GPS_COORDINATES_ENABLED = 24;     // send GPS coordinates feature was enabled
    public final static int STATE_GET_GPS_COORDINATES_DISABLED = 26;    // send GPS coordinates feature was disabled
    public final static int UNKNOWN_COMMAND = 28;                       // unknown received command

    // Define all the available Bluetooth device states into a single string separated by the corresponding separator
    // Each state must have ONLY ONE CHARACTER and ONE SEPARATOR
    // The states that can have a value will have BLUETOOTH_RECEIVE_STATE_DELIMITER as delimiter
    // The states that do NOT HAVE a value will have BLUETOOTH_RECEIVE_DELIMITER as delimiter
    public final static String STATES_STRING =
            "E" + BLUETOOTH_RECEIVE_DELIMITER +
                    "G" + BLUETOOTH_RECEIVE_STATE_DELIMITER +
                    "g" + BLUETOOTH_RECEIVE_STATE_DELIMITER +
                    "D" + BLUETOOTH_RECEIVE_STATE_DELIMITER +
                    "t" + BLUETOOTH_RECEIVE_STATE_DELIMITER +
                    "h" + BLUETOOTH_RECEIVE_STATE_DELIMITER +
                    "P" + BLUETOOTH_RECEIVE_DELIMITER +
                    "W" + BLUETOOTH_RECEIVE_DELIMITER +
                    "w" + BLUETOOTH_RECEIVE_STATE_DELIMITER +
                    "c" + BLUETOOTH_RECEIVE_STATE_DELIMITER +
                    "A" + BLUETOOTH_RECEIVE_STATE_DELIMITER +
                    "a" + BLUETOOTH_RECEIVE_STATE_DELIMITER +
                    "L" + BLUETOOTH_RECEIVE_DELIMITER +
                    "l" + BLUETOOTH_RECEIVE_DELIMITER +
                    "u" + BLUETOOTH_RECEIVE_STATE_DELIMITER;

    /**
     * Translate the received Bluetooth message.
     *
     * @param message is the message received from the Bluetooth device.
     * @param context is the calling activity context used to get strings from resources.
     */
    public static String getReceivedMessage(String message, Context context) {
        if (message == null) return null;
        // Initialize the String result object that will be returned
        String result = "";
        // Create the current time of the message
        String time = MESSAGE_TIME_START + Utils.getTime() + MESSAGE_TIME_END;

        // Check what kind of message is using the states variables and the default formats
        if (message.contains(STATE_START)) {
            result = "RTWL System started\n";
        } else if (message.contains(STATE_PING)) {
            result = "Arduino is active.\n";
            // The message contains GPS coordinates
        } else if (message.contains(".")) {
            // Split the message to spaces
            String[] string = message.split(" ");
            // Check if the message contains the components
            // The received message must have the format below:
            // latitude longitude weather_code temperature humidity air_quality speed direction
            if (string.length == 8) {
                // Check if the received GPS coordinates are valid
                if (message.contains(INVALID_GPS_COORDINATES) || message.contains(INVALID_GPS_COORDINATES1)) {
                    // Update the static variable that indicates the working state of GPS module
                    BluetoothService.GPS_MODULE_WORKING = false;
                    // Replace the invalid GPS coordinates with a character that will indicate
                    // that the GPS coordinates must be taken from the location service
                    result = MUST_GET_LOCATION;
                } else {
                    // Update the static variable that indicates the working state of the GPS module
                    BluetoothService.GPS_MODULE_WORKING = true;
                    // Write the GPS coordinates in the result String
                    result = MSG_REGION_START + " " + string[0] + " " + string[1];
                }
                // Initialize the unknown direction
                String direction = DIRECTION_UNKNOWN;
                // Check if the direction received from Bluetooth device contains the character
                // that indicates that the direction is unknown
                if (!string[7].contains("U")) {
                    // The direction is known
                    try {
                        // Try to get the direction from the received degrees
                        // If the direction is not in degrees, the catch instructions will be executed
                        direction = UtilsGoogleMaps.getDirection(Integer.parseInt(string[7]));
                    } catch (NumberFormatException e) {
                        // The direction is already expressed as String (ex: NW, NS, SW)
                        direction = string[7];
                    }
                }
                // Add to result String the sensors values translated
                result +=
                        "\n\t-\t" + UtilsGoogleMaps.getWeatherString(           // translate the weather_code in weather string
                                UtilsGoogleMaps.getWeatherStringIndex(
                                        Integer.parseInt(string[2])), context)
                                + "\n\t-\tTemperature: " + string[3]            // insert the temperature value
                                + "\n\t-\tHumidity: " + string[4]               // insert the humidity value
                                + "\n\t-\tAir: " + string[5]                    // insert the air quality value
                                + "\n\t-\tSpeed: " +                            // insert the speed value
                                (string[6].equals("-1") ? "0" : string[6])
                                + "\n\t-\tDirection: " + direction + "\n";      // insert the direction
                // Insert in result string the translated message and the received message separated by '@' character
                result = result + "@" + message;
            }
            // Check if the message contains a state
        } else if (message.contains(STATES_STRING.substring(STATE_ERROR, STATE_ERROR + 2))) {
            result = "An error occurred to Arduino microprocessor\n";
        } else if (message.contains(STATES_STRING.substring(STATE_GPS_NOT_WORKING, STATE_GPS_NOT_WORKING + 2))) {
            result = "GPS is not working!\n";
        } else if (message.contains(STATES_STRING.substring(STATE_GPS_INVALID_DATA, STATE_GPS_INVALID_DATA + 2))) {
            result = "Invalid data from GPS\n";
        } else if (message.contains(STATES_STRING.substring(STATE_TEMP_HUM_SENSOR_NOT_WORKING, STATE_TEMP_HUM_SENSOR_NOT_WORKING + 2))) {
            result = "Temperature and humidity sensor is not working!\n";
        } else if (message.contains(STATES_STRING.substring(STATE_TEMP_INVALID_DATA, STATE_TEMP_INVALID_DATA + 2))) {
            result = "Invalid temperature value! Received value: " + message.split(":")[1];
        } else if (message.contains(STATES_STRING.substring(STATE_HUM_INVALID_DATA, STATE_HUM_INVALID_DATA + 2))) {
            result = "Invalid humidity value! Received value: " + message.split(":")[1];
        } else if (message.contains(STATES_STRING.substring(STATE_PROXY_SENSOR_NOT_WORKING, STATE_PROXY_SENSOR_NOT_WORKING + 2))) {
            result = "Proxy sensor is not working!\n";
        } else if (message.contains(STATES_STRING.substring(STATE_WIND_SENSOR_NOT_WORKING, STATE_WIND_SENSOR_NOT_WORKING + 2))) {
            result = "Wind sensor not working!\n";
        } else if (message.contains(STATES_STRING.substring(STATE_WIND_SENSOR_INVALID_DATA, STATE_WIND_SENSOR_INVALID_DATA + 2))) {
            result = "Invalid wind sensor value! Received value: " + message.split(":")[1];
        } else if (message.contains(STATES_STRING.substring(STATE_WEATHER_CODE_INVALID, STATE_WEATHER_CODE_INVALID + 2))) {
            result = "Invalid weather code value Received value: " + message.split(":")[1];
        } else if (message.contains(STATES_STRING.substring(STATE_AIR_SENSOR_NOT_WORKING, STATE_AIR_SENSOR_NOT_WORKING + 2))) {
            result = "Air quality sensor is not working\n";
        } else if (message.contains(STATES_STRING.substring(STATE_AIR_SENSOR_INVALID_DATA, STATE_AIR_SENSOR_INVALID_DATA + 2))) {
            result = "Invalid air quality sensor value Received value: " + message.split(":")[1];
        } else if (message.contains(STATES_STRING.substring(STATE_GET_GPS_COORDINATES_ENABLED, STATE_GET_GPS_COORDINATES_ENABLED + 2))) {
            result = "Get coordinates every second feature is enabled!\n";
        } else if (message.contains(STATES_STRING.substring(STATE_GET_GPS_COORDINATES_DISABLED, STATE_GET_GPS_COORDINATES_DISABLED + 2))) {
            result = "Get coordinates every second feature is disabled!\n";
        } else if (message.contains(STATES_STRING.substring(UNKNOWN_COMMAND, UNKNOWN_COMMAND + 2))) {
            result = "Unknown command!\n";
        } else result = null;

        if (result == null) return null;
        // Check if the result string exists and is empty
        if (result.length() == 0)
            return null;    // don't send if is empty
        // Return the result string with the current time ahead
        return time + result;
    }
}