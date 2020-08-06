package com.example.realtimeweatherlocationtrafficsystem.models;

import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.example.realtimeweatherlocationtrafficsystem.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FireBaseManager {

    private DatabaseReference databaseReference;
    private List<Region> regions = new ArrayList<>();
    private Context context;
    private Resources resources;
    private onFireBaseDataNew onFireBaseDataNew;

    public interface onFireBaseDataNew{
        void onDataNewFireBase(List<Region> regions);
    }

    public FireBaseManager(Context context, Resources resources, onFireBaseDataNew onFireBaseDataNew){
        databaseReference = FirebaseDatabase.getInstance().getReference().child("data/");
        this.context = context;
        this.resources = resources;
        this.onFireBaseDataNew = onFireBaseDataNew;
        databaseReference.addValueEventListener(getValueEventListener());
    }

    public ValueEventListener getValueEventListener(){
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Map<String, Data> dictionary = ds.getValue(new GenericTypeIndicator<LinkedHashMap<String, Data>>() {});
                    List<Record> records = UtilsFireBase.getFireBaseRecord(dictionary);
                    Region region = new Region(ds.getKey(), records);
                    int index = UtilsFireBase.indexOfRegionList(regions, region);
                    if(index>=0){
                        regions.get(index).setRecords(records);
                    }
                    else{
                        regions.add(region);
                    }
                }
                onFireBaseDataNew.onDataNewFireBase(regions);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context,
                        String.format(resources.getString(R.string.data_base_error), error.toString()),
                        Toast.LENGTH_SHORT).show();
            }
        };
    }

    public void setValue(String region, String time, Data data){
        if(region==null || time==null || data==null) return;
        databaseReference.child(region).child(time).setValue(data);
    }

    public Region getRegion(int index){
        return regions.get(index);
    }

}
