package com.vrlabdev.remotecameracontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
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
import com.vrlabdev.remotecameracontrol.CameraStream.VideoStream;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    TextureView textureView;
    EditText editText;

    Timer timer=null;
    String serverip="192.168.1.100";


    private Handler mUiHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.textureView);
        editText =findViewById(R.id.editText);

        Thread myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        MyHTTPD myHTTPD = null;
                        try {
                            myHTTPD = new MyHTTPD();
                            myHTTPD.setmContext(getApplicationContext());
                            myHTTPD.setmActivity(MainActivity.this);
                            myHTTPD.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
        );

        myThread.start();
        startIpSender();

        /**
            *  Устанавливаем texture слушатель. Открывает камеру и выодит изображение, когда texture прогружается
        */
        TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

                try {
                    CameraControlChannel.getControl().stream = new VideoStream(getApplicationContext(),MainActivity.this);
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

        /**
            * Слушатель на изменение ip в текстовом поле
         */
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

    /**
     * Показать тост с текстом посередине экрана
     * @param text
     */
    private void showToast(String text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    /**
     *   Отправка ip по таймеру
     **/
    private void startIpSender() {
        timer = new Timer();
        timer.schedule(new MyTimer(getApplicationContext(),serverip), 0, 15000);
    }
}
