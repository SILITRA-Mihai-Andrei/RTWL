package com.example.realtimeweatherlocationtrafficsystem.models;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Data implements Serializable {
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

    @NonNull
    public String toString(){
        return code + " " + temperature + " " + humidity + " " + air;
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