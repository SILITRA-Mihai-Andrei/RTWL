package com.example.realtimeweatherlocationtrafficsystem.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Record {

    private String time;
    private Data data;

    public Record(){}

    public Record(String time, Data data) {
        this.time = time;
        this.data = data;
    }

    public Record(Record record) {
        this.time = record.time;
        this.data = record.data;
    }

    public String toString(){
        return time + " " + data.toString();
    }

    /*@Override
    public String toString(){
        String result = "";
        Data data = record.get(record.keySet().toArray()[0].toString());
        assert data != null;
        result += Arrays.toString(record.keySet().toArray()) + "=" + data.toString();
        return result;
    }*/

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}
