package com.example.realtimeweatherlocationtrafficsystem.models;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.provider.Settings;
import android.widget.EditText;

import androidx.core.app.ActivityCompat;

import com.example.realtimeweatherlocationtrafficsystem.R;

import java.util.Date;

public class Utils {

    // Defines the invalid values codes
    // These are used by some functions to check which fields have invalid values
    public final static int VALID = 0;
    public final static int INVALID_COORDINATES = 1;
    public final static int INVALID_CODE = 2;
    public final static int INVALID_TEMPERATURE = 3;
    public final static int INVALID_HUMIDITY = 4;
    public final static int INVALID_AIR = 5;

    // Define the maximum number of characters the text area can show
    // This value is used by the receiveBox view from TerminalActivity
    // If the number of characters is exceeded, all content is erased and new message is written
    public final static int MAX_RECEIVE_BOX_LENGTH = 1024;

    /**
     * Check if all fields have valid values.
     *
     * @param coordinates specifies the coordinates (ex: 46.23 26.20).
     * @param code        specifies the weather code (from 100 to 499).
     * @param temperature specifies the weather temperature (between -50 and 50 Celsius).
     * @param humidity    specifies the weather humidiyu (between 0 and 100).
     * @param air         specifies the region air quality or pollution (between 0 and 100).
     * @return the validation code that indicates which field is not valid or if all are valid.
     */
    public static int isDataValid(String coordinates, String code, String temperature, String humidity, String air) {
        if (!isCoordinatesValid(coordinates)) return INVALID_COORDINATES;
        else if (!isCodeValid(code)) return INVALID_CODE;
        else if (!isTemperatureValid(temperature)) return INVALID_TEMPERATURE;
        else if (isBetweenOneHundred(humidity)) return INVALID_HUMIDITY;
        else if (isBetweenOneHundred(air)) return INVALID_AIR;
        else return VALID;
    }

    /**
     * Using the validation code, return the validation string.
     *
     * @param code     specifies the validation code.
     * @param activity current activity that called the function (used for resources).
     * @return the validations string.
     */
    public static String getValidityMessage(int code, Context activity) {
        // Get the resources from current activity
        Resources resources = activity.getResources();
        if (code == INVALID_COORDINATES) return resources.getString(R.string.invalid_coordinates);
        else if (code == INVALID_CODE) return resources.getString(R.string.invalid_code);
        else if (code == INVALID_TEMPERATURE)
            return resources.getString(R.string.invalid_temperature);
        else if (code == INVALID_HUMIDITY) return resources.getString(R.string.invalid_humidity);
        else if (code == INVALID_AIR) return resources.getString(R.string.invalid_air);
        else if (code == VALID)
            return resources.getString(R.string.valid_data); // all fields are valid, so...
        else return resources.getString(R.string.invalid_data);
    }

    /**
     * Check if the coordinates are valid.
     * The limits are:
     * - for latitude: between -90 and 90;
     * - for longitude: between -180 and 180.
     *
     * @param coordinates specifies the coordinates that must be checked.
     * @return true if the coordinates are in limits and with valid format.
     */
    public static boolean isCoordinatesValid(String coordinates) {
        // Split the coordinates in latitude and longitude
        String[] coordinatesSplited = getCoordinatesSplited(coordinates);
        // Check if the both components exists
        if (coordinatesSplited == null) return false;
        // Check if the latitude and longitude are numbers
        // Check if the latitude and longitude values are in limits
        // Return true if the coordinates are valid
        return isNumber(coordinatesSplited[0]) && isNumber(coordinatesSplited[1]) && isNumber(coordinatesSplited[2]) && isNumber(coordinatesSplited[3])
                && getInt(coordinatesSplited[0]) >= -90 && getInt(coordinatesSplited[0]) <= 90
                && getInt(coordinatesSplited[2]) >= -180 && getInt(coordinatesSplited[2]) <= 180;
    }

    /**
     * Check if all fields have at least one character written inside.
     *
     * @return true if all fields have at least one character, false otherwise.
     */
    public static boolean areFieldsCompleted(EditText coordinates, EditText code, EditText temperature, EditText humidity, EditText air) {
        // Check if all fields exists
        // Check if all fields have empty strings
        // Negate the result -> one field have empty string, meaning that is not completed
        return !(coordinates == null || code == null || temperature == null || humidity == null || air == null
                || coordinates.getText().toString().equals("") || temperature.getText().toString().equals("")
                || humidity.getText().toString().equals("") || air.getText().toString().equals(""));
    }

    /**
     * Check if the weather code is valid.
     * The limits are minimum 100 and maximum 499.
     *
     * @param code specifies the weather code.
     * @return true if the code is in limits, false otherwise.
     */
    public static boolean isCodeValid(String code) {
        return isNumber(code) && getInt(code) >= 100 && getInt(code) <= 499;
    }

    /**
     * Check if the temperature value is valid.
     * The limits are minimum -50 and maximum 50 Celsius.
     *
     * @param temperature specifies the weather temperature.
     * @return true if the temperature value is in limits, false otherwise.
     */
    public static boolean isTemperatureValid(String temperature) {
        return isNumber(temperature) && getInt(temperature) >= -50 && getInt(temperature) <= 50;
    }

    /**
     * Check if the number is between 0 and 100. The 0 and 100 are included.
     * The parameter is checked first if is a number.
     *
     * @param number specifies the number that is checked.
     * @return <i>0 <= number <= 100</=></i>
     */
    public static boolean isBetweenOneHundred(String number) {
        return !isNumber(number) || getInt(number) < 0 || getInt(number) > 100;
    }

    /**
     * Check if the parameter is a number (digits).
     *
     * @param number specifies the number that is checked.
     * @return true if the parameter is a number.
     */
    public static boolean isNumber(String number) {
        // Check if the number exceeds the maximum int value
        if (number.length() >= String.valueOf(Integer.MAX_VALUE).length()) {
            // Trim the number with 9 decimals only
            number = number.substring(0, 9);
        }
        // Try to parse the number
        try {
            Integer.parseInt(number);
            // The number successfully parsed, the parameter is a number
            return true;
        } catch (NumberFormatException e) {
            // The number couldn't be parsed, it is not a number
            return false;
        }
    }

    /**
     * Convert the parameter into an integer value.
     *
     * @param number specifies the number that must be converted.
     * @return the parameter converted to integer or Integer.MAX_VALUE if the number is invalid.
     */
    public static int getInt(String number) {
        try {
            // Try to parse the parameter to integer
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            // The parameter is not a number
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Split the coordinates in pieces of numbers.
     * The first piece is the latitude integer value.
     * The second piece is the latitude decimals.
     * The third piece is the longitude integer value.
     * The fourth piece is the longitude decimals.
     * <p>
     * Ex: for 46.23 26.20, the result will be 46 23 26 20
     */
    public static String[] getCoordinatesSplited(String coordinates) {
        // Replace the floating value separators ('.' and/or ',') with a space
        // This will allow to split the values in 4 pieces.
        String[] result = coordinates.replace(".", " ").replace(",", " ").split(" ");
        // Check if all 4 pieces exists
        if (result.length != 4) return null;
        return result;
    }

    /**
     * Format the coordinates with custom separator for floating value and fixed decimals.
     *
     * @param coordinates specifies the coordinates that will be formatted.
     * @param decimals    specifies how many decimals the coordinates will have.
     * @param sep         specifies the floating value separator (ex: sep='#' => 46#23 26#20).
     * @return the coordinates formatted with fixed decimals and custom floating value separator.
     */
    public static String getCoordinatesFormat(String coordinates, int decimals, String sep) {
        // Check if the coordinates are valid
        if (isCoordinatesValid(coordinates)) {
            // Split the coordinates in two, latitude and longitude
            String[] result = getCoordinatesSplited(coordinates);
            // Check if the coordinates exists
            if (result == null) return null;
            // Get the coordinates decimals
            String result1 = result[1];
            String result3 = result[3];
            // Check if the number of decimals is higher than the specified value
            // If the coordinates have more decimals, trim them (without mercy)
            if (result1.length() >= decimals) result1 = result1.substring(0, decimals);
            if (result3.length() >= decimals) result3 = result3.substring(0, decimals);
            // Return the coordinates formatted and add the separators
            return result[0] + sep + result1 + " " + result[2] + sep + result3;
        }
        return null;
    }

    /**
     * Call the extended function that will format the coordinates with fixed decimals and a space
     * separator between the latitude and longitude values.
     *
     * @param coordinates specifies the coordinates that will be formatted.
     * @param decimals    specifies the number of decimals each value will have.
     * @return the coordinates formatted with fixed decimals and custom floating value separator.
     */
    public static String getCoordinatesWithDecimals(String coordinates, int decimals) {
        return getCoordinatesFormat(coordinates, decimals, " ");
    }

    /**
     * Get the coordinates with '.' as separator between the integer value and decimals.
     *
     * @param coordinates specifies the coordinates that will be modified.
     * @return the coordinates with '.' as separator between the integer value and decimals.
     */
    public static String getCoordinatesWithPoint(String coordinates) {
        // Split the coordinates in two parts, latitude and longitude values
        String[] splitedCoordinates = getCoordinatesSplited(coordinates);
        // Check if the coordinates exists
        if (splitedCoordinates == null) return null;
        // Return the coordinates with '.' separator between integer values and decimals and space between values
        return splitedCoordinates[0] + "." + splitedCoordinates[1] + " " + splitedCoordinates[2] + "." + splitedCoordinates[3];
    }

    /**
     * Check if the location on the phone is enabled and the application has location permission.
     * If the location is not enabled, the function will launch an Intent that will ask to enable it.
     *
     * @param contentResolver is the current activity contentResolver used to get the settings.
     * @param context         is the current activity context, used to launch the ask enable Intent over activity.
     */
    public static boolean isLocationEnabled(ContentResolver contentResolver, Context context) {
        // Initialize the variables used for return
        int locationMode = 0;
        boolean permissionCheck = true;
        try {
            // Try to get the location mode
            locationMode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            // Could not get the location mode => the location is not enabled
            e.printStackTrace();
        }
        // Check if the app has permission for location
        if (ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionCheck = false;
        }
        // Calculate if location is enabled
        // If the application doesn't have location permission, the location can't be seen
        return locationMode != Settings.Secure.LOCATION_MODE_OFF && permissionCheck;
    }

    /**
     * Check if a byte is in the list/vector of bytes.
     *
     * @param buffer is the list of bytes that is checked for containing the finding byte.
     * @param toFind is the byte that must be found in the list/vector.
     * @return true if the byte is in list/vector, false otherwise.
     */
    public static boolean containsByte(byte[] buffer, byte toFind) {
        // Loop trough all list/vector
        for (byte iterator : buffer) {
            // Check if the current byte is equal to the one to find
            if (iterator == toFind) {
                // Found it!
                return true;
            }
            // Check if the byte is ending byte
            if (iterator == 0) break;
        }
        // Not found in list/vector
        return false;
    }

    /**
     * Check if the number is in range.
     *
     * @param number specifies the number that is checked.
     * @param min    specifies the left limit.
     * @param max    specifies the right limit.
     * @return true is the number between the limits, false otherwise.
     */
    public static boolean isInRange(int number, int min, int max) {
        return number >= min && number <= max;
    }

    /**
     * Get the current date and time with the default format.
     * Format is "yy:MM:dd:kk:mm"
     *
     * @return the current date and time with "yy:MM:dd:kk:mm" format.
     */
    public static String getCurrentDateAndTime() {
        return android.text.format.DateFormat.format("yy:MM:dd:kk:mm", new java.util.Date()).toString();
    }

    /**
     * Get the current time with the default format.
     * Format is "kk:mm:ss"
     *
     * @return the current time with "kk:mm:ss" format.
     */
    public static String getTime() {
        return android.text.format.DateFormat.format("kk:mm:ss", new java.util.Date()).toString();
    }

    /**
     * Get the current date and time.
     *
     * @return the current date and time as Date object.
     */
    public static Date getDate() {
        return new java.util.Date();
    }

    /**
     * Get the difference of time between the current time and the one from parameter.
     *
     * @param date specifies the start time (end time is the current time).
     * @return the difference between start time and end time.
     */
    public static long getTimeDifference(Date date) {
        return getDate().getTime() - date.getTime();
    }
}
