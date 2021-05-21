package com.example.realtimeweatherlocationtrafficsystem.models;

import com.example.realtimeweatherlocationtrafficsystem.R;

import java.util.List;

/**
 * Define static functions used in Firebase procedures.
 */
public class UtilsFireBase {

    /**
     * Convert the list of regions to a String formatted to be displayed in a view.
     *
     * @param regions is the list of regions that will converted to a String and formatted.
     */
    public static String regionListToString(List<Region> regions) {
        // Check if the list exists and have at least one element
        if (regions == null || regions.size() == 0) {
            // There is no region in database
            return Resources.resources.getString(R.string.no_data);
        }
        // Create a StringBuilder to control the String format
        StringBuilder result = new StringBuilder();
        // Loop trough all regions in list
        for (int i = 0; i < regions.size(); i++) {
            // Get the danger of the region, if exists
            String danger = regions.get(i).getWeather().getDanger();
            // Append all weather values of the region to the String and separate them using a format
            result.append(regions.get(i).getName())
                    .append("\n\t\t").append(regions.get(i).getWeather().getWeather())
                    .append("\n\t\t").append(Resources.resources.getString(R.string.danger_dots)).append(danger == null ? Resources.resources.getString(R.string.none) : danger)
                    .append("\n\t\t").append(Resources.resources.getString(R.string.temp_dots)).append(regions.get(i).getWeather().getTemperature())
                    .append("\n\t\t").append(Resources.resources.getString(R.string._humidity_dots)).append(regions.get(i).getWeather().getHumidity())
                    .append("\n\t\t").append(Resources.resources.getString(R.string.air_dots)).append(regions.get(i).getWeather().getAir())
                    .append('\n');
        }
        return result.toString();
    }

}
