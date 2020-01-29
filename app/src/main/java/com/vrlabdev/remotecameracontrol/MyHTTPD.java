package com.vrlabdev.remotecameracontrol;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.widget.Toast;

import com.vrlabdev.remotecameracontrol.CameraStream.CameraControlChannel;
import com.vrlabdev.remotecameracontrol.CameraStream.VideoStream;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class MyHTTPD extends NanoHTTPD {
    public static final int PORT = 1234;//8765

    Context mContext=null;
    Activity mActivity = null;

    private boolean serverIsBusy=false;

    private Handler mUiHandler = new Handler();


    public MyHTTPD()
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

        //================================================
        //===========      сделать фото      =============
        //================================================

        if (uri.equals("/TakePhoto")) {
            if(serverIsBusy)
            {
                return newFixedLengthResponse("Please wait. Camera is busy now");
            }
            serverIsBusy=true;
            CameraControlChannel.getControl().isBusy=true;

            Thread myThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            CameraControlChannel.getControl().stream = new VideoStream(mContext, mActivity);
                            CameraControlChannel.getControl().stream.takePicture(false);
                            //CameraControlChannel.getControl().stream.CameraStart(CameraMode.STREAM_DRAWING_SURFACE_MODE);
                        }
                    });
                }
            }
            );
            myThread.start();

            while(CameraControlChannel.getControl().isBusy){}

            serverIsBusy = false;
            showToast("method is TakePhoto");
            return newFixedLengthResponse("Photo taken");
        }


        //================================================
        //===========   сделать фото с распознаванием  ===
        //================================================

        if(uri.equals("/GetRecogResult"))
        {
            if(serverIsBusy) {
                JSONObject jsonEmpty = new JSONObject();
                try {
                    jsonEmpty.put("ContainerNumber","Please wait");
                    jsonEmpty.put("ISOcode","Camera is busy now");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return newFixedLengthResponse(jsonEmpty.toString());
            } //вернуть если команда уже обрабатывается
            serverIsBusy=true;
            CameraControlChannel.getControl().isBusy=true;

            Thread myThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            CameraControlChannel.getControl().stream = new VideoStream(mContext,mActivity);
                            CameraControlChannel.getControl().stream.takePicture(true);
                        }
                    });
                }
            });
            myThread.start();

            while(CameraControlChannel.getControl().isBusy){}

            String response = CameraControlChannel.getControl().recognitionResult.toString();

            showToast("method is GetRecogResult");
            serverIsBusy=false;
            return newFixedLengthResponse(response);
        }


        //================================================
        //===========   начать запись видео  =============
        //================================================

        if (uri.equals("/StartVideo")) {
            if(serverIsBusy)
            {
                return newFixedLengthResponse("Please wait. Camera is busy now");
            }
            serverIsBusy=true;

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

        //================================================
        //===========   остановить запись видео  =========
        //================================================

        if (uri.equals("/StopVideo")) {


            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraControlChannel.getControl().stream.stopVideoRecord();
                }
            });
            String response = "stop record";
            showToast("method is StopVideo");

            serverIsBusy=false;
            return newFixedLengthResponse(response);
        }

        //если пришел левый метод

        return newFixedLengthResponse(Response.Status.NOT_FOUND,MIME_PLAINTEXT,"doesnt exist");
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
