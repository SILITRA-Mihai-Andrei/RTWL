package com.example.realtimeweatherlocationtrafficsystem.models;

import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.realtimeweatherlocationtrafficsystem.GoogleMapsActivity;
import com.example.realtimeweatherlocationtrafficsystem.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FireBaseManager {

    public static final String dataPath = "data/";
    public static final String weatherPath = "weather/";

    private List<Region> regions = new ArrayList<>();
    private Context context;
    private Resources resources;
    private onFireBaseDataNew onFireBaseDataNew;

    public interface onFireBaseDataNew{
        void onDataNewFireBase(List<Region> regions);
    }

    public FireBaseManager(Context context, Resources resources, onFireBaseDataNew onFireBaseDataNew){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(weatherPath);
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
                    Region region = new Region(ds.getKey(), ds.getValue(Weather.class));
                    for(int i=0; i<regions.size(); i++){
                        if(regions.get(i).getName().equals(region.getName())){
                            regions.add(i, region);
                            break;
                        }
                    }
                    regions.add(region);
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
        FirebaseDatabase.getInstance().getReference().child(dataPath).child(region).child(time).setValue(data);
    }

    public Region getRegion(String name){
        if (name == null) return null;
        for (int i=0; i<regions.size(); i++){
            if(regions.get(i).getName().equals(name))
                return regions.get(i);
        }
        return null;
    }
}
