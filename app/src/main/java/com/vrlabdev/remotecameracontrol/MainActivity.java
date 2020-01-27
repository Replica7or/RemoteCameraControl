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
import java.util.Collection;
import java.util.Map;
import java.util.Timer;

import com.codevog.android.license_library.MainInteractor;
import com.codevog.android.license_library.MainInteractorImpl;
import com.codevog.android.license_library.client_side_exception.BaseOcrException;
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
            myHTTPD.setmActivity(this);
            myHTTPD.start();
        } 
        catch (IOException e) { e.printStackTrace(); }




       /* final String [] massImages=new String[]{"/sdcard/5USU4_MPTU9274253_20200125_1730594572422499019327193.png"};
        OpenOpen openOpen = new OpenOpen();
        MainInteractorImpl mainInteractor = new MainInteractorImpl(MainActivity.this);

        mainInteractor.importLicense("863f8b0c4824a9d8903d0f71b92959e1b55cef8d926b6ca4463b8be1f9a363b1352f94cfbf9b1a9505887c277a78c24e4a6a52e1301c8405624c8629871a2ee474015451500f8f22d9880c2dd304de447712360d97207bdd7392f3e63f843df48fa76b25e2e6acd982c2fbf5f99ddc7d16d83ed395d61415bc7652b910f027e844dff7f42a6c925a06094ec1cda85fb24360160fa3bb4dd444708a269d26e7737891c28eb96f6003a891eeeeac83bf767c8ae22efcd6224a823f2ea6b61862a38d4d996ed9424dcd5e52d818d2eaa0a6ac86435175c9b82d5d58579ccfa41412f768577eae23b58d64aad8898f52cc229683b05a05a0fb2df169a4fff978e99f707f56d155e52d75e3d72926939218ca07aa0f6bc15c98be9e83c91cc3d86fd0d29c984af6c1bbbe59e692746c339e636ac177d09e02bd2ff8536a3b84327ff46694a3653dd6c83263195fc6640cbf86d58a5b090060c98d7c255a4fb92c4f6274ca889241c44ad2b70d5b11b7bab12c5d175cf85e1671cf2ec77f4fcbfc07cb");

        mainInteractor.doRecognize("b083c358-a424-4833-bfdc-3acf0c2db056", "052a13d0-9048-4f41-8c63-4a8130ee5b3c", massImages, openOpen);*/

        startIpSender();


        //CameraServer camServer = new CameraServer("QQQ",getApplicationContext());

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






    class OpenOpen implements  MainInteractor.Callback{

        @Override
        public void recogOk(Map<String, String> map) {
            Collection<String> str = map.values();
            for (String col : str) {
                String result = getRez(col);
                //поделить результат распознавания наномер контейнера и исо-код
                String [] ResultArray = {"Empty","Empty"};

                //проверка наличия контейнера


            }
        }

        @Override
        public void recogError(BaseOcrException e) {
            Log.d("ERROR ERROR    ", e.getMessage());
        }

        private String getRez(String col) {
            String result = "result";
            if (col.length() < 200) {
// LogWrite(LOG_TYPE_COMMON, "EmptyResult__FileName: " + imageFile.getName());
                return "Result is empty";
            } else {
                col = col.substring(col.indexOf("\"result\""));
                col = col.substring(10);
                result = col.substring(0, col.indexOf("\""));

                col = col.substring(col.indexOf("\"iso_size_type\""));
                col = col.substring(17);

                String subCol = col.substring(0, col.indexOf("\""));
                if (subCol.length() == 4 || subCol.length() == 3) {
                    result = result + " : " + subCol;
                } else {
                    result = result + " : Not recognize";
                }
                return result;
            }
        }
    }
    public void TestFunction(){}
}
