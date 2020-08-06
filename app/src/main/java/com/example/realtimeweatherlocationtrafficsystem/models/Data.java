package com.example.realtimeweatherlocationtrafficsystem.models;

import android.widget.Toast;

import androidx.annotation.IntRange;

public class Data {
    private int code;
    private int temperature;
    private int humidity;
    private int air;

    public Data() {}

    public Data(int code, int temperature, int humidity, int air) {
        this.code = code;
        this.temperature = temperature;
        this.humidity = humidity;
        this.air = air;
    }

    public Data(Data data) {
        this.code = data.getCode();
        this.temperature = getTemperature();
        this.humidity = getHumidity();
        this.air = getAir();
    }

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