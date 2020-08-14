package com.example.realtimeweatherlocationtrafficsystem.models;

import androidx.annotation.NonNull;

import java.util.List;

public class Region {

    private String name;
    private List<Record> records;

    public Region(String name, List<Record> records) {
        this.name = name;
        this.records = records;
    }

    @NonNull
    public String toString(){
        StringBuilder result = new StringBuilder(name + ":\n");
        for(int i=0; i<records.size(); i++){
            result.append("\t").append(records.get(i).getTime()).append(": ").append(records.get(i).getData().toString());
        }
        return result.toString();
    }

    public String getName() {
        return name;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        if(records==null) return;
        this.records = records;
    }
}
