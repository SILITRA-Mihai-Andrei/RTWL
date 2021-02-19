package com.example.realtimeweatherlocationtrafficsystem.models;

import androidx.annotation.NonNull;

/**
 * Class that contains general weather data as temperature, humidity, air quality and a code
 * indicating the weather type.
 * <p>
 * It also contains a string that is the name of the region.
 * The name of the region is made from GPS coordinates of the region (ex: 46.23 26.20).
 * <p>
 * This class is mostly used for receiving database records with the same fields.
 */
public class Region {

    // The name of the region
    // The name of the region is made from GPS coordinates of the region (ex: 46.23 26.20)
    private String name;
    // The object contains all weather data for the region (ex: temperature, humidity, air quality)
    private Weather weather;

    /**
     * Constructor
     * Initialize the variables and objects of the object using the parameters.
     *
     * @param name    is the region name.
     * @param weather is the object containing all weather that for region.
     */
    public Region(String name, Weather weather) {
        this.name = name;
        this.weather = weather;
    }

    /**
     * Convert the Data object to a String object, using its variables values and a space separator.
     */
    @NonNull
    public String toString() {
        return name + ":\n\t" + weather.toStringFormatDataBase();
    }

    /**
     * GETTERS
     */

    public String getName() {
        return name;
    }

    public Weather getWeather() {
        return weather;
    }
}
