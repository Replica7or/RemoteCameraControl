package com.vrlabdev.remotecameracontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.Timer;

import com.vrlabdev.remotecameracontrol.CameraStream.CameraControlChannel;
import com.vrlabdev.remotecameracontrol.CameraStream.CameraMode;
import com.vrlabdev.remotecameracontrol.CameraStream.CameraServer;
import com.vrlabdev.remotecameracontrol.CameraStream.VideoStream;



public class MainActivity extends AppCompatActivity {

    TextureView textureView;
    EditText editText;
    Timer timer=null;
    String serverip="192.168.1.100";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.textureView);
        editText =findViewById(R.id.editText);

        MyHTTPD myHTTPD = null;
        try
        {
            myHTTPD = new MyHTTPD();
            myHTTPD.setmContext(getApplicationContext());
            myHTTPD.start();
        } 
        catch (IOException e) { e.printStackTrace(); }

        startIpSender();


        //CameraServer camServer = new CameraServer("QQQ",getApplicationContext());

        TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

                try {
                    CameraControlChannel.getControl().stream = new VideoStream(getApplicationContext());
                    CameraControlChannel.getControl().stream.SetTargetSurface(textureView);
                    CameraControlChannel.getControl().stream.CameraBuild(CameraMode.DRAWING_SURFACE_MODE);
                    CameraControlChannel.getControl().stream.StartCamera();
                }
                catch(Exception e)
                {
                    try {

                    }
                    catch (Exception e1)
                    {

                        showToast("камера недоступна");
                        Log.e("ERROR", e.getMessage().toString());
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        };
        textureView.setSurfaceTextureListener(surfaceTextureListener);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length()!=0) {
                    timer.cancel();
                    serverip = s.toString();
                    startIpSender();
                }
            }
        });
    }

    private void showToast(String text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }


    private void startIpSender() {
        timer = new Timer();
        timer.schedule(new MyTimer(getApplicationContext(),serverip), 0, 5000);

    }
}
