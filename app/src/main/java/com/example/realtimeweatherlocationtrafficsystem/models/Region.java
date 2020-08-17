package com.example.realtimeweatherlocationtrafficsystem.models;

import androidx.annotation.NonNull;

public class Region {

    private String name;
    private Weather weather;

    public Region(String name, Weather weather) {
        this.name = name;
        this.weather = weather;
    }

    @NonNull
    public String toString(){
        return name + ":\n\t" + weather.toStringFormatDataBase();
    }

    public String getName() {
        return name;
    }

    public Weather getWeather() {
        return weather;
    }
}
