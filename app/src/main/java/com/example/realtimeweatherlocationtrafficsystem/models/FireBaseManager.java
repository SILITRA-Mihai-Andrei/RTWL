package com.example.realtimeweatherlocationtrafficsystem.models;

import androidx.annotation.NonNull;
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
    private onFireBaseDataNew onFireBaseDataNew;

    public interface onFireBaseDataNew{
        void onDataNewFireBase(List<Region> regions);
    }

    public FireBaseManager(onFireBaseDataNew onFireBaseDataNew){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(weatherPath);
        this.onFireBaseDataNew = onFireBaseDataNew;
        databaseReference.addValueEventListener(getValueEventListener());
    }

    public ValueEventListener getValueEventListener(){
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Region region = new Region(ds.getKey(), ds.getValue(Weather.class));
                    found = false;
                    for(int i=0; i<regions.size(); i++){
                        if(regions.get(i).getName().equals(region.getName())){
                            regions.set(i, region);
                            found = true;
                            break;
                        }
                    }
                    if(!found){
                        regions.add(region);
                    }
                }
                onFireBaseDataNew.onDataNewFireBase(regions);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
    }

    public void setValue(String region, String time, Data data){
        if(region==null || time==null || data==null) return;
        FirebaseDatabase.getInstance().getReference().child(dataPath).child(region).child(time).setValue(data);
    }
}
