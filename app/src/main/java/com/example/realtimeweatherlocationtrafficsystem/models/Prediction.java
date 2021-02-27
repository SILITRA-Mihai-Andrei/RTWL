package com.example.realtimeweatherlocationtrafficsystem.models;

public class Prediction {

    // Predicted variables with their percent of accuracy
    // The weather code and its probability
    private int code, code_p;
    // The temperature and its probability
    private int temperature, temperature_p;
    // The humidity and its probability
    private int humidity, humidity_p;

    /**
     * Default constructor required for calls to DataSnapshot.getValue(this.class)
     */
    Prediction() {
    }

    /**
     * GETTERS and SETTERS
     * Default getters and setters required for calls to DataSnapshot.getValue(this.class)
     */

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode_p() {
        return code_p;
    }

    public void setCode_p(int code_p) {
        this.code_p = code_p;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getTemperature_p() {
        return temperature_p;
    }

    public void setTemperature_p(int temperature_p) {
        this.temperature_p = temperature_p;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getHumidity_p() {
        return humidity_p;
    }

    public void setHumidity_p(int humidity_p) {
        this.humidity_p = humidity_p;
    }
}
