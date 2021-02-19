package com.example.realtimeweatherlocationtrafficsystem.models;

/**
 * General weather data.
 * <p>
 * This class is mostly used for receiving database records with the same fields.
 * The fields that don't exists will be null.
 * <p>
 * The class implements the default constructor without parameters necessary for auto assignation.
 */
public class Weather {

    private String danger;
    private String weather;
    private float air;
    private float humidity;
    private float temperature;

    /**
     * Constructor
     * This will allow auto assignation from a dictionary.
     * The values that don't exists in dictionary will be null.
     */
    public Weather() {
    }

    /**
     * Constructor
     * Initialize the object variables with the parameters.
     */
    public Weather(String danger, String weather, float air, float humidity, float temperature) {
        this.danger = danger;
        this.weather = weather;
        this.air = air;
        this.humidity = humidity;
        this.temperature = temperature;
    }

    /**
     * Constructor
     * This will allow auto assignation from a dictionary.
     * The values that don't exists in dictionary will be null.
     */
    public Weather(Weather weather) {
        this.danger = weather.getDanger();
        this.weather = weather.getWeather();
        this.air = weather.getAir();
        this.humidity = weather.getHumidity();
        this.temperature = weather.getTemperature();
    }

    /**
     * Format the object variables to String objects to display them in a view.
     */
    public String toStringFormatDataBase() {
        String tabs = "\n\t\t\t\t\t\t";
        return tabs + "weather: " + weather + tabs + "danger: " + danger
                + tabs + "air: " + air + tabs + "humidity: " + humidity
                + tabs + "temperature: " + temperature;
    }

    /**
     * GETTERS
     */

    public String getDanger() {
        return danger;
    }

    public String getWeather() {
        return weather;
    }

    public float getAir() {
        return air;
    }

    public float getHumidity() {
        return humidity;
    }

    public float getTemperature() {
        return temperature;
    }
}