package com.example.realtimeweatherlocationtrafficsystem.models;

import androidx.annotation.NonNull;

public class Record {

    private String time;
    private Data data;

    public Record(){} //need this for firebase assignation

    public Record(String time, Data data) {
        this.time = time;
        this.data = data;
    }

    public Record(Record record) {
        this.time = record.time;
        this.data = record.data;
    }

    @NonNull
    public String toString(){
        return time + " " + data.toString();
    }

    public String getTime() {
        return time;
    }

    public Data getData() {
        return data;
    }
}
