package com.example.realtimeweatherlocationtrafficsystem.models;

import android.content.Context;
import android.graphics.Color;
import com.example.realtimeweatherlocationtrafficsystem.R;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

public class UtilsGoogleMaps {

    public static final String[] DIRECTIONS = {"N", "NE", "EN",
                                                "E", "ES", "SE",
                                                "S", "SW", "WS",
                                                "WN", "NW", "N"};

    public static final int COLOR_REGION_RED = 1;
    public static final int COLOR_REGION_GREEN = 2;
    public static final int COLOR_REGION_ORANGE = 3;
    public static final double REGION_AREA = 0.005;

    public static final int MIN_VALUE_FIRST_GRADE = 0;
    public static final int MAX_VALUE_FIRST_GRADE = 33;
    public static final int MIN_VALUE_SECOND_GRADE = 34;
    public static final int MAX_VALUE_SECOND_GRADE = 66;
    public static final int MIN_VALUE_THIRD_GRADE = 67;
    public static final int MAX_VALUE_THIRD_GRADE = 99;

    public static PolygonOptions getPolygonOptions(LatLng coordinate, double distance, int color) {
        int[] colors = UtilsGoogleMaps.getRegionColor(color);
        if (colors == null) {
            colors = new int[2];
            colors[0] = Color.argb(0, 0, 0, 0);
            colors[1] = Color.argb(0, 0, 0, 0);
        }
        return new PolygonOptions()
                .add(new LatLng(coordinate.latitude - distance, coordinate.longitude - distance),
                        new LatLng(coordinate.latitude + distance, coordinate.longitude - distance),
                        new LatLng(coordinate.latitude + distance, coordinate.longitude + distance),
                        new LatLng(coordinate.latitude - distance, coordinate.longitude + distance))
                .strokeColor(colors[0])
                .fillColor(colors[1])
                .strokeWidth(5f);
    }

    public static LatLng getCoordinates(String coordinate, int decimals) {
        String[] splitedCoordinates = Utils.getCoordinatesSplited(coordinate);
        if (splitedCoordinates == null || !Utils.isCoordinatesValid(coordinate)) return null;
        String result1 = splitedCoordinates[1];
        String result3 = splitedCoordinates[3];
        if (result1.length() >= decimals) result1 = result1.substring(0, decimals) + "00000000000";
        if (result3.length() >= decimals) result3 = result3.substring(0, decimals) + "00000000000";
        return new LatLng(Double.parseDouble(splitedCoordinates[0] + "." + result1),
                Double.parseDouble(splitedCoordinates[2] + "." + result3));
    }

    public static MarkerOptions getMarkerOptions(LatLng coordinate, String title, String description, int icon) {
        if(coordinate==null || icon == -1) return null;
        return new MarkerOptions()
                .position(coordinate)
                .title(title)
                .snippet(description)
                .icon(BitmapDescriptorFactory.fromResource(icon))
                .anchor(0.5f, 0.5f);
    }

    public static int getWeatherStringIndex(int weatherCode){
        for(int i=1; i<=4; i++){
            if(Utils.isInRange(weatherCode, MIN_VALUE_FIRST_GRADE+(i*100), MAX_VALUE_FIRST_GRADE+(i*100))) return (i-1)+((i-1)*2); //0, 3, 6, 9
            else if(Utils.isInRange(weatherCode, MIN_VALUE_SECOND_GRADE+(i*100), MAX_VALUE_SECOND_GRADE+(i*100))) return i+((i-1)*2); //1, 4, 7, 10
            else if(Utils.isInRange(weatherCode, MIN_VALUE_THIRD_GRADE+(i*100), MAX_VALUE_THIRD_GRADE+(i*100))) return i+1+((i-1)*2); //2, 5, 7, 11
        }
        return -1;
    }

    public static boolean isPointInRegion(String region, String point, double areaDistance){
        region = Utils.getCoordinatesWithPoint(region);
        if(region==null) return false;
        LatLng regionCoords = getCoordinates(region, 3);
        if(regionCoords==null) return false;
        point = Utils.getCoordinatesWithPoint(point);
        if (point==null) return false;
        LatLng pointCoords = getCoordinates(point, 3);
        if (pointCoords==null) return false;
        return  //left-top point
                pointCoords.latitude <= regionCoords.latitude+areaDistance
                &&  pointCoords.longitude >= regionCoords.longitude-areaDistance
                //right-top point
                && pointCoords.latitude <= regionCoords.latitude+areaDistance
                &&  pointCoords.longitude <= regionCoords.longitude+areaDistance
                //left-top point
                && pointCoords.latitude >= regionCoords.latitude-areaDistance
                &&  pointCoords.longitude >= regionCoords.longitude-areaDistance
                //left-top point
                && pointCoords.latitude >= regionCoords.latitude-areaDistance
                &&  pointCoords.longitude <= regionCoords.longitude+areaDistance;
    }

    public static String getDirection(int degrees){
        if(degrees >= 0 && degrees <= 360){
            return DIRECTIONS[(degrees - 15) / 30];
        }
        return DIRECTIONS[0];
    }

    public static int getWeatherStringIndex(String weather, Context context){
        if(weather.equals(context.getString(R.string.weather_sunny))) return 0;
        else if(weather.equals(context.getString(R.string.weather_sun))) return 1;
        else if(weather.equals(context.getString(R.string.weather_heat))) return 2;
        else if(weather.equals(context.getString(R.string.weather_soft_rain))) return 3;
        else if(weather.equals(context.getString(R.string.weather_moderate_rain))) return 4;
        else if(weather.equals(context.getString(R.string.weather_torrential_rain))) return 5;
        else if(weather.equals(context.getString(R.string.weather_soft_wind))) return 6;
        else if(weather.equals(context.getString(R.string.weather_moderate_wind))) return 7;
        else if(weather.equals(context.getString(R.string.weather_torrential_wind))) return 8;
        else if(weather.equals(context.getString(R.string.weather_soft_snow_fall))) return 9;
        else if(weather.equals(context.getString(R.string.weather_moderate_snow_fall))) return 10;
        else if(weather.equals(context.getString(R.string.weather_massive_snow_fall))) return 11;
        else return -1;
    }

    public static String getWeatherString(int weatherCode, Context context){
        if(weatherCode==0) return context.getString(R.string.weather_sunny);
        else if(weatherCode==1) return context.getString(R.string.weather_sun);
        else if(weatherCode==2) return context.getString(R.string.weather_heat);
        else if(weatherCode==3) return context.getString(R.string.weather_soft_rain);
        else if(weatherCode==4) return context.getString(R.string.weather_moderate_rain);
        else if(weatherCode==5) return context.getString(R.string.weather_torrential_rain);
        else if(weatherCode==6) return context.getString(R.string.weather_soft_wind);
        else if(weatherCode==7) return context.getString(R.string.weather_moderate_wind);
        else if(weatherCode==8) return context.getString(R.string.weather_torrential_wind);
        else if(weatherCode==9) return context.getString(R.string.weather_soft_snow_fall);
        else if(weatherCode==10) return context.getString(R.string.weather_moderate_snow_fall);
        else if(weatherCode==11) return context.getString(R.string.weather_massive_snow_fall);
        else return null;
    }

    public static int[] getRegionColor(int color) {
        int[] result = new int[2];
        if (color == COLOR_REGION_RED) {
            result[0] = Color.argb(40, 255, 0, 0);
            result[1] = Color.argb(32, 255, 0, 0);
            return result;
        } else if (color == COLOR_REGION_GREEN) {
            result[0] = Color.argb(40, 0, 255, 0);
            result[1] = Color.argb(32, 0, 255, 0);
            return result;
        } else if (color == COLOR_REGION_ORANGE) {
            result[0] = Color.argb(40, 255, 180, 20);
            result[1] = Color.argb(32, 255, 180, 20);
            return result;
        } else
            return null;
    }

}
