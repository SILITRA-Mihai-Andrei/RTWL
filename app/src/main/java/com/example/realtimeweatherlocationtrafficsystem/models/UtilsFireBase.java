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
        if(regions == null){
            return "No data.";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < regions.size(); i++) {
            String danger = regions.get(i).getWeather().getDanger();
            result.append(regions.get(i).getName())
                .append("\n\t\t").append(regions.get(i).getWeather().getWeather())
                .append("\n\t\tdanger: ").append(danger==null ? "none" : danger)
                .append("\n\t\ttemperature: ").append(regions.get(i).getWeather().getTemperature())
                .append("\n\t\thumidity: ").append(regions.get(i).getWeather().getHumidity())
                .append("\n\t\tair: ").append(regions.get(i).getWeather().getAir())
                .append('\n');
        }
        return result.toString();
    }

}
