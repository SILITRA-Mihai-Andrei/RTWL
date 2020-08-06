package com.example.realtimeweatherlocationtrafficsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.realtimeweatherlocationtrafficsystem.models.Data;
import com.example.realtimeweatherlocationtrafficsystem.models.FireBaseManager;
import com.example.realtimeweatherlocationtrafficsystem.models.Region;
import com.example.realtimeweatherlocationtrafficsystem.models.Utils;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsFireBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TesteActivity extends AppCompatActivity implements FireBaseManager.onFireBaseDataNew{

    private TextView tv1;
    private TextView tv2;
    private Button btn;
    private FireBaseManager fireBaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teste);
        tv1 = findViewById(R.id.tv1);
        tv2 = findViewById(R.id.tv2);
        btn = findViewById(R.id.btn);
        fireBaseManager = new FireBaseManager(getBaseContext(), getResources(), this);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fireBaseManager.setValue("47 64 26 24", Utils.getCurrentDateAndTime(),
                        new Data(new Random().nextInt(500), new Random().nextInt(50), new Random().nextInt(100), new Random().nextInt(100)));
            }
        });
    }

    @Override
    public void onDataNewFireBase(List<Region> regions) {
        tv2.setText(UtilsFireBase.regionListToString(regions));
    }
}