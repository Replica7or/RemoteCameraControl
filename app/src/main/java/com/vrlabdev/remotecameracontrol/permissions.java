package com.vrlabdev.remotecameracontrol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class permissions extends AppCompatActivity {



    private final int MY_PERMISSIONS_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // разрешение не предоставлено

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                                                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                                        Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST);
        }
        else {
            // разрешение предоставлено
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                if (grantResults.length == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Intent mainIntent = new Intent (this,MainActivity.class);
                    startActivity(mainIntent);
                    permissions.this.finish();
                }
                else {
                    permissions.this.finish();
                }
            }
            return;
        }
    }
}
