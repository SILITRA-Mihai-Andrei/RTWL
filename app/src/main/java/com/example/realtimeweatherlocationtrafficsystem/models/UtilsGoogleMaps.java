package com.example.realtimeweatherlocationtrafficsystem.models;

import android.graphics.Color;

import com.example.realtimeweatherlocationtrafficsystem.R;
import com.example.realtimeweatherlocationtrafficsystem.services.FireBaseService;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.HashMap;

/**
 * Implement public static functions used for GoogleMaps activity.
 */
public class UtilsGoogleMaps {

    // Declare all directions clockwise
    // The directions index are important because it will calculated using the degrees
    //          N
    //       NW   NE
    //     WN       EN
    //   W             E
    //     WS       ES
    //      SW    SE
    //          S
    public static final String[] DIRECTIONS = {
            "N", "NE", "EN",        // 0-30,     30-60,    60-90
            "E", "ES", "SE",        // 90-120,   120-150,  150-180
            "S", "SW", "WS",        // 180-210,  210-240,  240-270
            "W", "WN", "NW"};       // 270-300,  300-330,  330-360 degrees

    // Define color indicators for region area
    public static final int COLOR_REGION_RED = 0;
    public static final int COLOR_REGION_GREEN = 1;
    public static final int COLOR_REGION_ORANGE = 2;

    // Define the region area polygon distance
    // The maximum distance an region area polygon can have on map
    // Ex: for Region 46.23 26.20, the region area bounds will be
    // Top-left:        46.235 26.195
    // Top-right:       46.235 26.205
    // Bottom-left:     46.225 26.195
    // Bottom-right:    46.225 26.205
    public static final double REGION_AREA = 0.005;

    // Define the weather intensity grades
    // There are 3 grades of weather intensity
    // First intensity grade means nice weather, like sunny, soft rain, soft wind, soft snow-fall
    // Second intensity grade means medium weather conditions, like sun, moderate rain, moderate wind
    // Third intensity grade means danger/torrential weather condition, like heat, torrential rain, massive snow fall
    // All weather codes must be in range of 100 and 499
    // Ex: 100-199 is for sun weather, 100-133 is for sunny, 134-166 is for sun and 167-199 for heat
    public static final int MIN_VALUE_FIRST_GRADE = 0;
    public static final int MAX_VALUE_FIRST_GRADE = 33;
    public static final int MIN_VALUE_SECOND_GRADE = 34;
    public static final int MAX_VALUE_SECOND_GRADE = 66;
    public static final int MIN_VALUE_THIRD_GRADE = 67;
    public static final int MAX_VALUE_THIRD_GRADE = 99;

    /**
     * Get the polygon placed in the center of coordinates received, with the distances received and
     * a custom background color. The fill and stroke color will be the same, but with different opacity.
     *
     * @param coordinate specifies the center of polygon.
     * @param distance   specifies the distance between the center point and the bounds.
     * @param color      specifies the background color (fill) and stroke (higher opacity) of the polygon.
     * @return the polygon view centered to @coordinate point, with the distances between border and
     * center of @distance, a fill and stroke color indicated by @color.
     */
    public static PolygonOptions getPolygonOptions(LatLng coordinate, double distance, int color) {
        // Get the region area color of the polygon according to the weather condition in that region
        int[] colors = getRegionColor(color);
        // Check if the colors exists
        if (colors == null) {
            // Create new colors vector
            colors = new int[2];
            // Create a default color for unknown region weather condition (black)
            colors[0] = Color.argb(40, 0, 0, 0);
            colors[1] = Color.argb(32, 0, 0, 0);
        }
        // Get and return the polygon using the parameters and variables defined
        return new PolygonOptions()
                .add(new LatLng(coordinate.latitude - distance, coordinate.longitude - distance),       // Top-left corner
                        new LatLng(coordinate.latitude + distance, coordinate.longitude - distance),    // Top-right corner
                        new LatLng(coordinate.latitude + distance, coordinate.longitude + distance),    // Bottom-left corner
                        new LatLng(coordinate.latitude - distance, coordinate.longitude + distance))    // Bottom-right corner
                .strokeColor(colors[0])     // the border color of the polygon
                .fillColor(colors[1])       // the fill/inside color of the polygon
                .strokeWidth(5f);           // the border width of the polygon
    }

    /**
     * Get the coordinates received as String object and return them as LatLng object with decimals specified.
     *
     * @param coordinate is the String object containing the coordinates.
     * @param decimals   specifies the number of decimals of the return coordinates.
     * @return the coordinates converted and with fixed decimals.
     */
    public static LatLng getCoordinates(String coordinate, int decimals) {
        // Split the coordinates received by separator '.' or ','
        String[] splitedCoordinates = Utils.getCoordinatesSplited(coordinate);
        // Check if the splitting was successful and coordinates are valid
        if (splitedCoordinates == null || !Utils.isCoordinatesValid(coordinate))
            return null;
        // Get the decimals of the coordinates
        String result1 = splitedCoordinates[1];
        String result3 = splitedCoordinates[3];
        // Remove the extra decimals over the number specified
        if (result1.length() >= decimals) result1 = result1.substring(0, decimals) + "00000000000";
        if (result3.length() >= decimals) result3 = result3.substring(0, decimals) + "00000000000";
        // Return the LatLng object containing the coordinates transformed
        return new LatLng(Double.parseDouble(splitedCoordinates[0] + "." + result1),
                Double.parseDouble(splitedCoordinates[2] + "." + result3));
    }

    /**
     * Get the marker centered to coordinates, with the title, description and icon received.
     * The icon will be anchored to the middle of the image, so the movement and scaling of the marker will
     * keep the good shape and correct position.
     *
     * @param coordinate  specifies the center of the marker.
     * @param title       specifies the marker title.
     * @param description specifies the marker description.
     * @param icon        specifies the marker icon.
     * @return the MarkerOptions object that will configure the marker.
     */
    public static MarkerOptions getMarkerOptions(LatLng coordinate, String title, String description, int icon) {
        // Check if the coordinates and the icon exists
        if (coordinate == null || icon == -1) return null;
        // Create and return the marker with the variables and parameters
        return new MarkerOptions()
                .position(coordinate)                               // center the marker to coordinates
                .title(title)                                       // modify the marker title
                .snippet(description)                               // modify the marker description
                .icon(BitmapDescriptorFactory.fromResource(icon))   // add the marker icon
                .anchor(0.5f, 0.5f);                         // anchor the marker icon to keep it in good shape after view changes
    }

    /**
     * Get the weather index using the weather code.
     *
     * @param weatherCode specifies the weather code (100-499).
     * @return the weather index (0-11), if exists.
     * <p>
     * Ex:
     * - for weather code 100-133, the index will be 0;
     * - for weather code 134-166, the index will be 1;
     * - for weather code 467-499, the index will be 11;
     * - for weather code under 100 or higher than 499, the index will be -1.
     */
    public static int getWeatherStringIndex(int weatherCode) {
        // Loop trough all weather conditions (sun, rain, wind and snowfall)
        for (int i = 1; i <= 4; i++) {
            // Check if the weather code specifies a weather condition of low intensity (sunny, soft rain)
            if (Utils.isInRange(weatherCode, MIN_VALUE_FIRST_GRADE + (i * 100), MAX_VALUE_FIRST_GRADE + (i * 100)))
                return (i - 1) + ((i - 1) * 2); // indexes 0, 3, 6, 9
                // Check if the weather code specifies a weather condition of medium intensity (sun, moderate rain)
            else if (Utils.isInRange(weatherCode, MIN_VALUE_SECOND_GRADE + (i * 100), MAX_VALUE_SECOND_GRADE + (i * 100)))
                return i + ((i - 1) * 2); // indexes 1, 4, 7, 10
                // Check if the weather code specifies a weather condition of high intensity (heat, torrential rain)
            else if (Utils.isInRange(weatherCode, MIN_VALUE_THIRD_GRADE + (i * 100), MAX_VALUE_THIRD_GRADE + (i * 100)))
                return i + 1 + ((i - 1) * 2); // indexes 2, 5, 7, 11
        }
        return -1;
    }

    /**
     * Get the weather grade/intensity using the weather title.
     * Third intensity grade means danger/torrential weather condition, like heat, torrential rain, massive snow fall
     * All weather codes must be in range of 100 and 499
     * Ex: 100-199 is for sun weather, 100-133 is for sunny, 134-166 is for sun and 167-199 for heat
     * There are 3 weather intensities for each weather condition
     * For sun weather, the intensities are Sunny (0), Sun (1), Heat (2)
     *
     * @param weather is the weather title (ex: "Sun", "Soft rain").
     * @return the weather grade/intensity.
     */
    public static int getWeatherGrade(String weather) {
        // Get the weather index using the weather title
        int index = getWeatherStringIndex(weather);
        // Check if the index is valid
        if (index == -1) return -1;
        // Return the weather intensity
        // There are 3 weather intensities for each weather condition
        // For sun weather, the intensities are Sunny, Sun, Heat
        return index / 3;
    }

    /**
     * Check if a point on the map is inside of a region.
     * Check if the region contain the point.
     *
     * @param region       is the region that possibly contain the point.
     * @param point        is the point that is checked if is inside the region.
     * @param areaDistance is the area of the region where a point can be.
     * @return true if the point is inside the region, false otherwise.
     */
    public static boolean isPointInRegion(String region, String point, double areaDistance) {
        // Check the region format
        // The region name/title must be with GPS coordinates format (ex: 46.23 26.20)
        region = Utils.getCoordinatesWithPoint(region);
        // Check if the region name/title is valid
        if (region == null) return false;
        // Convert the region coordinates string into a LatLng object with latitude and longitude with 3 decimals
        LatLng coordinates = getCoordinates(region, 3);
        // Check if the coordinates exists and are valid
        if (coordinates == null) return false;
        // Do the same thing for the point coordinates
        point = Utils.getCoordinatesWithPoint(point);
        if (point == null) return false;
        LatLng pointCoordinates = getCoordinates(point, 3);
        if (pointCoordinates == null) return false;
        // Check if the point coordinates are inside of the region coordinates that forms the region area polygon
        return          // left-top point
                pointCoordinates.latitude <= coordinates.latitude + areaDistance
                        && pointCoordinates.longitude >= coordinates.longitude - areaDistance
                        // right-top point
                        && pointCoordinates.latitude <= coordinates.latitude + areaDistance
                        && pointCoordinates.longitude <= coordinates.longitude + areaDistance
                        // left-bottom point
                        && pointCoordinates.latitude >= coordinates.latitude - areaDistance
                        && pointCoordinates.longitude >= coordinates.longitude - areaDistance
                        // right-bottom point
                        && pointCoordinates.latitude >= coordinates.latitude - areaDistance
                        && pointCoordinates.longitude <= coordinates.longitude + areaDistance;
    }

    /**
     * Transform the direction degrees into direction string.
     * Ex:
     * - for 15 degrees, the direction is N;
     * - for 30 degrees, the direction is NE;
     * - for 60 degrees, the direction is EN;
     * - for 370 degrees, the direction is NW.
     *
     * @param degrees specifies the direction in degrees.
     * @return the direction degrees transformed in direction string.
     */
    public static String getDirection(int degrees) {
        // Check if the degrees are in limits
        if (degrees >= 0 && degrees <= 360) {
            // Calculate and return the direction string using the list of directions with the index calculated
            return DIRECTIONS[(degrees - 15) / 30];
        }
        return DIRECTIONS[0];
    }

    /**
     * Get the prediction data (date-time and values) of the region.
     * Check in the predictions list if the region exists.
     *
     * @param region specifies the finding region.
     * @return null if there is no region for region, otherwise return the prediction data
     */
    public static HashMap<String, Prediction> getPredictions(String region) {
        // Check if the predictions list exists and is not empty
        if (FireBaseService.predictions == null || FireBaseService.predictions.isEmpty())
            return null;
        // Loop through all predictions
        for (HashMap<String, HashMap<String, Prediction>> prediction : FireBaseService.predictions) {
            // Get the key of the HashMap - the key is the region name
            String key = (String) prediction.keySet().toArray()[0];
            // Check if the current region in loop is equal to the finding one
            if (key.equals(region)) {
                // Found the region, return its prediction data
                return prediction.get(key);
            }
        }
        return null;
    }

    /**
     * Get the weather index using the weather string.
     * Ex:
     * - for "Sunny", the index will is the first one, index=0;
     * - for "Sun", the index will be the second one, index=1;
     * - for "Soft rain", the index will be index=3;
     * - for "Massive snow fall", the index will be the last one, index=11.
     *
     * @param weather is the weather string, containing the weather title (ex: "Soft rain").
     * @return the weather index from 0 to 11.
     */
    public static int getWeatherStringIndex(String weather) {
        if (weather.equals(Resources.resources.getString(R.string.weather_sunny))
                || weather.equals(Resources.resources.getString(R.string.weather_sunny_ro)))
            return 0;
        else if (weather.equals(Resources.resources.getString(R.string.weather_sun))
                || weather.equals(Resources.resources.getString(R.string.weather_sun_ro))) return 1;
        else if (weather.equals(Resources.resources.getString(R.string.weather_heat))
                || weather.equals(Resources.resources.getString(R.string.weather_heat_ro)))
            return 2;
        else if (weather.equals(Resources.resources.getString(R.string.weather_soft_rain))
                || weather.equals(Resources.resources.getString(R.string.weather_soft_rain_ro)))
            return 3;
        else if (weather.equals(Resources.resources.getString(R.string.weather_moderate_rain))
                || weather.equals(Resources.resources.getString(R.string.weather_moderate_rain_ro)))
            return 4;
        else if (weather.equals(Resources.resources.getString(R.string.weather_torrential_rain))
                || weather.equals(Resources.resources.getString(R.string.weather_torrential_rain_ro)))
            return 5;
        else if (weather.equals(Resources.resources.getString(R.string.weather_soft_wind))
                || weather.equals(Resources.resources.getString(R.string.weather_soft_wind_ro)))
            return 6;
        else if (weather.equals(Resources.resources.getString(R.string.weather_moderate_wind))
                || weather.equals(Resources.resources.getString(R.string.weather_moderate_wind_ro)))
            return 7;
        else if (weather.equals(Resources.resources.getString(R.string.weather_torrential_wind))
                || weather.equals(Resources.resources.getString(R.string.weather_torrential_wind_ro)))
            return 8;
        else if (weather.equals(Resources.resources.getString(R.string.weather_soft_snow_fall))
                || weather.equals(Resources.resources.getString(R.string.weather_soft_snow_fall_ro)))
            return 9;
        else if (weather.equals(Resources.resources.getString(R.string.weather_moderate_snow_fall))
                || weather.equals(Resources.resources.getString(R.string.weather_moderate_snow_fall_ro)))
            return 10;
        else if (weather.equals(Resources.resources.getString(R.string.weather_massive_snow_fall))
                || weather.equals(Resources.resources.getString(R.string.weather_massive_snow_fall_ro)))
            return 11;
        else return -1;
    }

    /**
     * Get the weather string using the weather code (100-499).
     * Ex:
     * - for weather code 100-133, the weather string is "Sunny";
     * - for weather code 134-166, the weather string is "Sun";
     * - for weather code 467-499, the weather string is "Massive snow fall".
     *
     * @param weatherCode is the weather code (100-499).
     * @return the weather string.
     */
    public static String getWeatherString(int weatherCode) {
        if (weatherCode == 0) return Resources.resources.getString(R.string.weather_sunny);
        else if (weatherCode == 1) return Resources.resources.getString(R.string.weather_sun);
        else if (weatherCode == 2) return Resources.resources.getString(R.string.weather_heat);
        else if (weatherCode == 3)
            return Resources.resources.getString(R.string.weather_soft_rain);
        else if (weatherCode == 4)
            return Resources.resources.getString(R.string.weather_moderate_rain);
        else if (weatherCode == 5)
            return Resources.resources.getString(R.string.weather_torrential_rain);
        else if (weatherCode == 6)
            return Resources.resources.getString(R.string.weather_soft_wind);
        else if (weatherCode == 7)
            return Resources.resources.getString(R.string.weather_moderate_wind);
        else if (weatherCode == 8)
            return Resources.resources.getString(R.string.weather_torrential_wind);
        else if (weatherCode == 9)
            return Resources.resources.getString(R.string.weather_soft_snow_fall);
        else if (weatherCode == 10)
            return Resources.resources.getString(R.string.weather_moderate_snow_fall);
        else if (weatherCode == 11)
            return Resources.resources.getString(R.string.weather_massive_snow_fall);
        else return null;
    }

    /**
     * Get the weather icon using the weather index.
     * Ex:
     * - for index=0, it will be returned an icon for weather "Sunny";
     * - for index=11, if will be returned an icon for weather "Massive snow fall".
     *
     * @param index is the weather index (from 0 to 11).
     * @return the resource weather icon (not the icon).
     */
    public static int getWeatherIcon(int index) {
        if (index == 0) return R.drawable.sunny;
        else if (index == 1) return R.drawable.sun;
        else if (index == 2) return R.drawable.heat;
        else if (index == 3) return R.drawable.soft_rain;
        else if (index == 4) return R.drawable.moderate_rain;
        else if (index == 5) return R.drawable.torrential_rain;
        else if (index == 6) return R.drawable.soft_wind;
        else if (index == 7) return R.drawable.moderate_wind;
        else if (index == 8) return R.drawable.torrential_wind;
        else if (index == 9) return R.drawable.soft_snow_fall;
        else if (index == 10) return R.drawable.moderate_snow_fall;
        else if (index == 11) return R.drawable.massive_snow_fall;
        else return -1;
    }

    /**
     * Get the weather text color using the weather intensity.
     * The weather intensity indicates how dangerous is the weather.
     * <p>
     * Ex:
     * - for grade=0, return a friendly color, like GREEN;
     * - for grade=1, return a maybe-friends color, like ORANGE;
     * - for grade=2, return a enemy / I-will-kill-you color, like RED.
     * <p>
     * Ex:
     * - for grade=0, the weather intensity is the lowest (ex: Sunny, Soft rain, Soft wind);
     * - for grade=1, the weather intensity is in the middle of danger zone (ex: Sun, Moderate rain);
     * - for grade=2, the weather intensity is high, can be a dangerous weather (ex: Heat, Torrential rain).
     *
     * @param grade is the weather grade/intensity.
     * @return the weather text color.
     */
    public static int getWeatherTextColor(int grade) {
        if (grade == 0) {
            return Color.argb(255, 0, 255, 0);
        } else if (grade == 1) {
            return Color.argb(255, 255, 180, 20);
        } else if (grade == 2) {
            return Color.argb(255, 255, 0, 0);
        } else
            return Color.argb(255, 0, 0, 0);
    }

    /**
     * Get the region area color using the color code received.
     * The region area is represented by a polygon.
     * The fill color and the stroke color of the polygon are the same, but with different opacity.
     * <p>
     * AVAILABLE COLORS:
     * - COLOR_REGION_RED - for a red color;
     * - COLOR_REGION_GREEN - for a green color;
     * - COLOR_REGION_ORANGE - for an orange color;
     *
     * @param color specifies which color to choose.
     * @return the region color.
     */
    public static int[] getRegionColor(int color) {
        int[] result = new int[2];
        if (color == COLOR_REGION_RED) {
            result[0] = Color.argb(40, 255, 0, 0);
            result[1] = Color.argb(32, 255, 0, 0);
            return result;
        } else if (color == COLOR_REGION_GREEN) {
            result[0] = Color.argb(40, 0, 255, 0);
            result[1] = Color.argb(32, 0, 255, 0);
            return result;
        } else if (color == COLOR_REGION_ORANGE) {
            result[0] = Color.argb(40, 255, 180, 20);
            result[1] = Color.argb(32, 255, 180, 20);
            return result;
        } else
            return null;
    }

}
