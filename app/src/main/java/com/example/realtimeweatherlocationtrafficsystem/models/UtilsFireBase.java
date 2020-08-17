package com.example.realtimeweatherlocationtrafficsystem.models;

import java.util.List;

public class UtilsFireBase {

    public static int indexOfRegionList(List<Region> regions, Region region) {
        if(regions==null || regions.size()==0 || region==null) return -1;
        for (int i = 0; i<regions.size(); i++) {
            if (regions.get(i).getName().equals(region.getName())) return i;
        }
        return -1;
    }

    public static String regionListToString(List<Region> regions) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < regions.size(); i++) {
            result.append("> ").append(regions.get(i).getName())
                .append("weather: ").append("\n\t\t- ").append(regions.get(i).getWeather().getWeather())
                .append("danger: ").append("\n\t\t- ").append(regions.get(i).getWeather().getDanger())
                .append("temperature: ").append("\n\t\t- ").append(regions.get(i).getWeather().getTemperature())
                .append("humidity: ").append("\n\t\t- ").append(regions.get(i).getWeather().getHumidity())
                .append("air: ").append("\n\t\t- ").append(regions.get(i).getWeather().getAir())
                .append('\n');
        }
        return result.toString();
    }

}
