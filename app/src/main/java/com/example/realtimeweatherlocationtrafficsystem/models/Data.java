package com.example.realtimeweatherlocationtrafficsystem.models;

import androidx.annotation.NonNull;

public class Data {
    private int code;
    private int temperature;
    private int humidity;
    private int air;

    public Data() {} //need this for firebase assignation

    public Data(int code, int temperature, int humidity, int air) {
        this.code = code;
        this.temperature = temperature;
        this.humidity = humidity;
        this.air = air;
    }

    public Data(Data data) { //need this for firebase assignation
        this.code = data.getCode();
        this.temperature = data.getTemperature();
        this.humidity = data.getHumidity();
        this.air = data.getAir();
    }

    @NonNull
    public String toString(){
        return code + " " + temperature + " " + humidity + " " + air;
    }

    public String toStringFormatDataBase(){
        String tabs = "\n\t\t\t\t\t\t";
        return tabs + "air: " + air + tabs + "code: " + code
                + tabs + "humidity: " + humidity + tabs + "temperature: " + temperature;
    }

    public int getCode() {
        return code;
    }


    public int getTemperature() {
        return temperature;
    }


    public int getHumidity() {
        return humidity;
    }

    public int getAir() {
        return air;
    }

}