package com.vrlabdev.remotecameracontrol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vrlabdev.remotecameracontrol.CameraStream.CameraControlChannel;
import com.vrlabdev.remotecameracontrol.CameraStream.VideoStream;


public class MainActivity extends AppCompatActivity {

    private HandlerThread mBackgroundThread;
    
    TextureView textureView;
    EditText editText;
    Switch selector;

    Timer timer=null;
    String serverip=null;
    //String serverip="192.168.31.142";     //TODO: для работы в лабе



    private Handler mUiHandler = new Handler();
    private Handler mBackgroundHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        textureView = findViewById(R.id.textureView);
        editText = findViewById(R.id.editText);
        selector = findViewById(R.id.selector);

        serverip=this.getResources().getString(R.string.VMTPip);       //TODO: для работы в порту

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // разрешение не предоставлен
            Intent permissions  =new Intent(this, permissions.class);
            startActivity(permissions);
            finish();
        }

        selector.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    buttonView.setText("VRLAB");
                    editText.setVisibility(View.VISIBLE);
                    editText.setText(R.string.VRip);
                    CameraControlChannel.getControl().fileUploadPath = MainActivity.this.getResources().getString(R.string.VRLABfile);
                }
                else
                {
                    buttonView.setText("VMTP");
                    editText.setVisibility(View.INVISIBLE);
                    editText.setText(R.string.VMTPip);
                    CameraControlChannel.getControl().fileUploadPath= MainActivity.this.getResources().getString(R.string.VMTPfile);
                }
            }
        });

        StartHTTTPserver();
        startIpSender();

        /**
            *  Устанавливаем texture слушатель. Открывает камеру и выодит изображение, когда texture прогружается
        */
        TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

                try {
                    if(!CameraControlChannel.getControl().isBusy) {
                        CameraControlChannel.getControl().stream = new VideoStream(getApplicationContext(), MainActivity.this);
                        CameraControlChannel.getControl().stream.SetTargetSurface(textureView);
                        CameraControlChannel.getControl().stream.CameraBuild();
                        //CameraControlChannel.getControl().stream.StartDrawing();
                        Log.d("INFO", "texture available!");
                    }
                }
                catch(Exception e)
                {
                    Log.e("ERROR", ""+e.getMessage());
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
            public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
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
                if(s.length()>6)
                    {
                        String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                                                    +"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                                                    +"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                                                    +"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
                        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
                        Matcher matcher = pattern.matcher(s);
                        if (matcher.find())
                        {
                            timer.cancel();
                            serverip = s.toString();
                            startIpSender();
                        }
                    }
            }
        });
    }




    void StartHTTTPserver()
    {
        Thread myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        MyHTTPD myHTTPD;
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

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() throws InterruptedException
    {
        mBackgroundThread.quitSafely();
        mBackgroundThread.join();
        mBackgroundThread = null;
        mBackgroundHandler = null;
    }

    public void install(View view)
    {
        install_apk(new File("/sdcard/app-debug.apk"));
    }

    void install_apk(File file) {
        try {
            if (file.exists()) {
                String[] fileNameArray = file.getName().split(Pattern.quote("."));
                if (fileNameArray[fileNameArray.length - 1].equals("apk")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Uri downloaded_apk = getFileUri(MainActivity.this, file);
                        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(downloaded_apk,
                                "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        MainActivity.this.startActivity(intent);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(file),
                                "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        MainActivity.this.startActivity(intent);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    Uri getFileUri(Context context, File file) {
        return FileProvider.getUriForFile(MainActivity.this, context.getApplicationContext().getPackageName() + ".provider", file);
    }
}
