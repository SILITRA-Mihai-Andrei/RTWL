package com.example.realtimeweatherlocationtrafficsystem.models;

public class Weather {
    private String danger;
    private String weather;
    private float air;
    private float humidity;
    private float temperature;

    public Weather() {} //need this for firebase assignation

    public Weather(String danger, String weather, float air, float humidity, float temperature) {
        this.danger = danger;
        this.weather = weather;
        this.air = air;
        this.humidity = humidity;
        this.temperature = temperature;
    }

    public Weather(Weather weather) { //need this for firebase assignation
        this.danger = weather.getDanger();
        this.weather = weather.getWeather();
        this.air = weather.getAir();
        this.humidity = weather.getHumidity();
        this.temperature = weather.getTemperature();
    }

    public String toStringFormatDataBase(){
        String tabs = "\n\t\t\t\t\t\t";
        return tabs + "weather: " + weather + tabs + "danger: " + danger
                + tabs + "air: " + air + tabs + "humidity: " + humidity
                + tabs + "temperature: " + temperature;
    }

    public String getDanger(){return danger;}
    public String getWeather(){return weather;}
    public float getAir(){return air;}
    public float getHumidity(){return humidity;}
    public float getTemperature(){return temperature;}
}