package com.vrlabdev.remotecameracontrol;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.widget.Toast;

import com.vrlabdev.remotecameracontrol.CameraStream.CameraControlChannel;
import com.vrlabdev.remotecameracontrol.CameraStream.CameraMode;
import com.vrlabdev.remotecameracontrol.CameraStream.VideoStream;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class MyHTTPD extends NanoHTTPD {
    public static final int PORT = 8765;

    Context mContext=null;
    Activity mActivity = null;

    boolean recognition=false;

    private Handler mUiHandler = new Handler();

    public MyHTTPD() throws IOException
    {
        super(PORT);
    }

    public void setmContext(Context context)
    {
        mContext=context;
    }
    public void setmActivity(Activity activity){mActivity=activity;}
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if (uri.equals("/TakePhoto")) {
            String response = "HelloWorld";
            CameraControlChannel.getControl().isBusy=true;
            Thread myThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            CameraControlChannel.getControl().stream = new VideoStream(mContext, mActivity);
                            recognition=false;
                            CameraControlChannel.getControl().stream.takePicture();
                            //CameraControlChannel.getControl().stream.CameraStart(CameraMode.STREAM_DRAWING_SURFACE_MODE);
                        }
                    });
                }
            }
            );
            myThread.start();


            while(CameraControlChannel.getControl().isBusy){}



           /* try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
           showToast("method is TakePhoto");
            return newFixedLengthResponse(response);
        }

        if (uri.equals("/StartVideo")) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraControlChannel.getControl().stream = new VideoStream(mContext,mActivity);
                    CameraControlChannel.getControl().stream.startVideoRecord();
                }
            });
            String response = "Start record";
            showToast("method is StartVideo");
            return newFixedLengthResponse(response);
        }

        if (uri.equals("/StopVideo")) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraControlChannel.getControl().stream.stopVideoRecord();
                }
            });
            String response = "stop record";

            showToast("method is StopVideo");
            return newFixedLengthResponse(response);
        }

        if(uri.equals("/GetRecogResult"))
        {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraControlChannel.getControl().stream = new VideoStream(mContext,mActivity);
                    recognition = true;
                    CameraControlChannel.getControl().stream.takePicture();
                }
            });
        }
        String response = "recognition";
        showToast("method is GetRecogResult");
        return newFixedLengthResponse(response);
    }
    private void showToast(String text) {
        final String ftext = text;
        Thread myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(mContext, ftext, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });
            }
        }
        );
        myThread.start();
    }
}
