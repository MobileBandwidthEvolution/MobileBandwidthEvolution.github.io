package com.thucloud;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.thucloud.collectionmodule.R;

import com.thucloud.collectionmodule.Main;

import me.weishu.reflection.Reflection;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION },
                    MY_PERMISSIONS_REQUEST_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { android.Manifest.permission.READ_PHONE_STATE },
                    MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
        }
        Button myButton = findViewById(R.id.button);
        myButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Log.d("onClick", String.valueOf(view.getId()));
        if (view.getId() == R.id.button) {

            Main main = new Main(this);
            main.report("user_id", "Samsung", "s22", "Android", "13", "移动", "浙江", "杭州", "118.21", "29.11", "999", "666", "10", "0", "15");

        }
    }
}
