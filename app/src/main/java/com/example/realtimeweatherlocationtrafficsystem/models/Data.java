package com.example.realtimeweatherlocationtrafficsystem.models;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * Class that contains general weather data as temperature, humidity, air quality and a code
 * indicating the weather type.
 * <p>
 * This class is mostly used for receiving database records with the same fields.
 * The fields that don't exists will be null.
 * <p>
 * The class implements the default constructor without parameters necessary for auto assignation.
 */
public class Data implements Serializable {

    private int code;           // the weather code (from 100-sunny to 499-massive snow fall)
    private int temperature;    // the temperature value (from -50 to 50 Celsius degrees)
    private int humidity;       // the humidity value (from 0 to 100%)
    private int air;            // the air quality / pollution (from 0 to 100%)

    /**
     * Constructor
     * This will allow auto assignation from a dictionary.
     * The values that don't exists in dictionary will be null.
     */
    public Data() {
    }

    /**
     * Constructor
     * Initialize all variables from parameters.
     */
    public Data(int code, int temperature, int humidity, int air) {
        this.code = code;
        this.temperature = temperature;
        this.humidity = humidity;
        this.air = air;
    }

    /**
     * Convert the Data object to a String object, using its variables values and a space separator.
     */
    @NonNull
    public String toString() {
        return code + " " + temperature + " " + humidity + " " + air;
    }

    /**
     * GETTERS and SETTERS
     * DON'T REMOVE THEM, EVENT IF ARE NOT USED !!!
     * They are necessary for auto assignation from dictionary.
     */

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getAir() {
        return air;
    }

    public void setAir(int air) {
        this.air = air;
    }
}