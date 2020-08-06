package com.example.realtimeweatherlocationtrafficsystem.models;

import android.content.Context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UtilsFireBase {

    public static List<Record> getFireBaseRecord(Map<String, Data> records) {
        if (records == null) return null;
        List<Record> recordsList = new ArrayList<>();
        Iterator<Map.Entry<String, Data>> it = records.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Data> item = (Map.Entry<String, Data>)it.next();
            recordsList.add(new Record(item.getKey(), item.getValue()));
            it.remove(); // avoids a ConcurrentModificationException
        }
        return recordsList;
    }

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
            result.append("> ").append(regions.get(i).getName()).append("\n");
            for (int j = 0; j < regions.get(i).getRecords().size(); j++) {
                result.append("\t\t# ").append(regions.get(i).getRecords().get(j).getTime())
                        .append(regions.get(i).getRecords().get(j).getData().toStringFormatDataBase()).append("\n");
            }
        }
        return result.toString();
    }

}
