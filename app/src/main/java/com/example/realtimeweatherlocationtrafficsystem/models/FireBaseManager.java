package com.example.realtimeweatherlocationtrafficsystem.models;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This manager will implement a listener for events to a FireBase service.
 * It will also send data to the Firebase service.
 */
public class FireBaseManager {

    // Define the main nodes in database
    public static final String dataPath = "data/";          // contains all the records sent by users
    public static final String weatherPath = "weather/";    // contains all calculated data from /data/ node

    // Define the list of regions that will received from database
    private List<Region> regions = new ArrayList<>();
    // Interface that will notify when new data from database is received
    private onFireBaseDataNew onFireBaseDataNew;

    /**
     * Interface that will notify when new data from database is received.
     * All regions received will be sent as parameter.
     */
    public interface onFireBaseDataNew {
        void onDataNewFireBase(List<Region> regions);
    }

    /**
     * Constructor
     * Initialize all necessary objects and the interface that will notify changes in database.
     */
    public FireBaseManager(onFireBaseDataNew onFireBaseDataNew) {
        // Create the database reference for /weather/ node
        // This node contains all data calculated by the server using the data from /data/ node
        DatabaseReference databaseReference =
                FirebaseDatabase.getInstance().getReference().child(weatherPath);
        // Initialize the interface
        this.onFireBaseDataNew = onFireBaseDataNew;
        // Add listener on value added to database event
        databaseReference.addValueEventListener(getValueEventListener());
    }

    /**
     * Listener that will be notified when new values appear in database.
     * It will receive all new data from database.
     */
    public ValueEventListener getValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Initialize the variable that indicates if the new value already exists in the list
                boolean found;
                // Loop through all new data received from database
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // Create a new Region object containing the key as the name, and the values as a Weather object
                    // The region name contains the GPS coordinates of the region
                    // The region data contains a Weather object with all weather values
                    Region region = new Region(ds.getKey(), ds.getValue(Weather.class));
                    found = false;
                    // Loop through all regions list to check if already exists
                    for (int i = 0; i < regions.size(); i++) {
                        // Check if the current region name in the loop is equal to the new region name
                        if (regions.get(i).getName().equals(region.getName())) {
                            // Replace the current region in loop with the new region found
                            regions.set(i, region);
                            // Update the variable
                            found = true;
                            // The new region was found
                            break;
                        }
                    }
                    // Check if the new region already existed in the regions list
                    // found = false means the new region is a new region in database
                    // found = true means the new region was updated in database
                    if (!found) {
                        // Add the new region to the regions list
                        regions.add(region);
                    }
                }
                // Notify the interface that the regions list is updated
                onFireBaseDataNew.onDataNewFireBase(regions);
            }

            /** No implementation. */
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
    }

    /**
     * Sent data to database and set it to /data/ node.
     * This node is where all users will send data.
     *
     * @param region is the region name (GPS coordinates).
     * @param time   is the current time.
     * @param data   is the Data object containing all region weather data.
     */
    public void setValue(String region, String time, Data data) {
        // Check if all parameters exists
        // All parameters are necessary
        if (region == null || time == null || data == null) return;
        // Send the data to Firebase database
        FirebaseDatabase.getInstance().getReference()
                .child(dataPath).child(region).child(time).setValue(data);
    }
}
