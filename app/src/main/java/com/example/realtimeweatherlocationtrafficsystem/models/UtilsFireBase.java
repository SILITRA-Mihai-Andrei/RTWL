package com.example.realtimeweatherlocationtrafficsystem.models;

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
        // Check if the list exists
        if (regions == null) {
            // There is no region in database
            return "No data.";
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
                    .append("\n\t\tdanger: ").append(danger == null ? "none" : danger)
                    .append("\n\t\ttemperature: ").append(regions.get(i).getWeather().getTemperature())
                    .append("\n\t\thumidity: ").append(regions.get(i).getWeather().getHumidity())
                    .append("\n\t\tair: ").append(regions.get(i).getWeather().getAir())
                    .append('\n');
        }
        return result.toString();
    }

}
