package com.example.realtimeweatherlocationtrafficsystem.models;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.provider.Settings;
import android.widget.EditText;

import androidx.core.app.ActivityCompat;

import com.example.realtimeweatherlocationtrafficsystem.R;

import java.util.Date;

public class Utils {

    public final static int VALID = 0;
    public final static int INVALID_COORDINATES = 1;
    public final static int INVALID_CODE = 2;
    public final static int INVALID_TEMPERATURE = 3;
    public final static int INVALID_HUMIDITY = 4;
    public final static int INVALID_AIR = 5;

    public final static int COLOR_RED = 1;
    public final static int COLOR_GREEN = 2;
    public final static int COLOR_BLUE = 3;

    //bluetooth
    public final static int MAX_RECEIVE_BOX_LENGTH = 1024; //6400

    public static int isDataValid(String coordinates, String code, String temperature, String humidity, String air) {
        if (!isCoordinatesValid(coordinates)) return INVALID_COORDINATES;
        else if (!isCodeValid(code)) return INVALID_CODE;
        else if (!isTemperatureValid(temperature)) return INVALID_TEMPERATURE;
        else if (isBetweenOneHundred(humidity)) return INVALID_HUMIDITY;
        else if (isBetweenOneHundred(air)) return INVALID_AIR;
        else return VALID;
    }

    public static String getInvalidMessage(int field, Context activity) {
        Resources resources = activity.getResources();
        if (field == INVALID_COORDINATES) return resources.getString(R.string.invalid_coordinates);
        else if (field == INVALID_CODE) return resources.getString(R.string.invalid_code);
        else if (field == INVALID_TEMPERATURE)
            return resources.getString(R.string.invalid_temperature);
        else if (field == INVALID_HUMIDITY) return resources.getString(R.string.invalid_humidity);
        else if (field == INVALID_AIR) return resources.getString(R.string.invalid_air);
        else if (field == VALID) return resources.getString(R.string.valid_data);
        else return resources.getString(R.string.invalid_data);
    }

    public static boolean isCoordinatesValid(String coordinates) {
        String[] coordinatesSplited = getCoordinatesSplited(coordinates);
        if (coordinatesSplited == null) return false;
        return isNumber(coordinatesSplited[0]) && isNumber(coordinatesSplited[1]) && isNumber(coordinatesSplited[2]) && isNumber(coordinatesSplited[3])
                && getInt(coordinatesSplited[0]) >= -90 && getInt(coordinatesSplited[0]) <= 90
                && getInt(coordinatesSplited[2]) >= -180 && getInt(coordinatesSplited[2]) <= 180;
    }

    public static boolean areFieldsCompleted(EditText coordinates, EditText code, EditText temperature, EditText humidity, EditText air) {
        return !(coordinates == null || code == null || temperature == null || humidity == null || air == null
                || coordinates.getText().toString().equals("") || temperature.getText().toString().equals("")
                || humidity.getText().toString().equals("") || air.getText().toString().equals(""));
    }

    public static boolean isCodeValid(String code) {
        return isNumber(code) && getInt(code) >= 100 && getInt(code) <= 499;
    }

    public static boolean isTemperatureValid(String temperature) {
        return isNumber(temperature) && getInt(temperature) >= -50 && getInt(temperature) <= 50;
    }

    public static boolean isBetweenOneHundred(String number) {
        return !isNumber(number) || getInt(number) < 0 || getInt(number) > 100;
    }

    public static boolean isNumber(String number) {
        if (number.length() >= String.valueOf(Integer.MAX_VALUE).length()) {
            number = number.substring(0, 9);
        }
        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static int getInt(String number) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    public static String[] getCoordinatesSplited(String coordinates) {
        String[] result = coordinates.replace(".", " ").replace(",", " ").split(" ");
        if (result.length != 4) return null;
        return result;
    }

    public static String getCoordinatesFormat(String coordinates, int decimals, String sep) {
        if (isCoordinatesValid(coordinates)) {
            String[] result = getCoordinatesSplited(coordinates);
            if (result == null) return null;
            String result1 = result[1];
            String result3 = result[3];
            if (result1.length() >= decimals) result1 = result1.substring(0, decimals);
            if (result3.length() >= decimals) result3 = result3.substring(0, decimals);
            return result[0] + sep + result1 + " " + result[2] + sep + result3;
        }
        return null;
    }

    public static String getCoordinatesForDataBase(String coordinates, int decimals) {
        return getCoordinatesFormat(coordinates, decimals, " ");
    }

    public static String getCoordinatesWithPoint(String coordinates) {
        String[] splitedCoordinates = getCoordinatesSplited(coordinates);
        if (splitedCoordinates == null) return null;
        return splitedCoordinates[0] + "." + splitedCoordinates[1] + " " + splitedCoordinates[2] + "." + splitedCoordinates[3];
    }

    public static boolean isLocationEnabled(ContentResolver contentResolver, Context context) {
        int locationMode = 0;
        boolean permissionCheck = true;
        try {
            locationMode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if (ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionCheck = false;
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF && permissionCheck;
    }

    public static int getColorARGB(int color) {
        if (color == Utils.COLOR_RED) return Color.argb(255, 255, 0, 0);
        else if (color == Utils.COLOR_GREEN) return Color.argb(255, 0, 255, 0);
        else if (color == Utils.COLOR_BLUE) return Color.argb(255, 0, 0, 255);
        return -1;
    }

    public static boolean containsByte(byte[] buffer, byte toFind) {
        for (byte iterator : buffer) {
            if (iterator == toFind) {
                return true;
            }
            if (iterator == 0) break;
        }
        return false;
    }

    public static boolean isInRange(int number, int min, int max) {
        return number >= min && number <= max;
    }

    public static String getCurrentDateAndTime() {
        return android.text.format.DateFormat.format("yy:MM:dd:kk:mm", new java.util.Date()).toString();
    }

    public static String getTime() {
        return android.text.format.DateFormat.format("kk:mm:ss", new java.util.Date()).toString();
    }

    public static Date getDate() {
        return new java.util.Date();
    }

    public static long getTimeDifference(Date date) {
        return getDate().getTime() - date.getTime();
    }
}
