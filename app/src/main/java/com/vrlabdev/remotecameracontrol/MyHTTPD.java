package com.vrlabdev.remotecameracontrol;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.vrlabdev.remotecameracontrol.CameraStream.CameraControlChannel;
import com.vrlabdev.remotecameracontrol.CameraStream.CameraMode;
import com.vrlabdev.remotecameracontrol.CameraStream.VideoStream;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import  fi.iki.elonen.router.RouterNanoHTTPD;
import okhttp3.Response;


public class MyHTTPD extends RouterNanoHTTPD {
    public static final int PORT = 1234;//8765

    Context mContext=null;
    Activity mActivity = null;

    private boolean serverIsBusy=false;

    private Handler mUiHandler = new Handler();

    StoreHandler storeHandler;

    // ТЕСТОВАЯ ПОЕБЕНЬ \\
    public static class StoreHandler extends GeneralHandler {
        @Override
        public Response get(
                UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return newFixedLengthResponse("Retrieving store for id = "
                    + urlParams.get("storeId"));
        }
    }

    public MyHTTPD()
    {
        super(PORT);
        addMappings();
        storeHandler = new StoreHandler();
    }

    public void setmContext(Context context)
    {
        mContext=context;
    }
    public void setmActivity(Activity activity){mActivity=activity;}

    public MyHTTPD(int port)
    {
        super(port);
    }

    @Override
    public void addMappings() {
        super.addMappings();
        addRoute("/", MyHTTPD.class);
    }


    @Override
    public Response serve(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();
        //================================================
        //===========      сделать фото      =============
        //================================================



        if (uri.contains("/TakePhoto")) {

            if(serverIsBusy)
            {
                return newFixedLengthResponse("Please wait. Camera is busy now");
            }
            serverIsBusy=true;
            CameraControlChannel.getControl().isBusy=true;

            String randName = uri.substring(10);//костыль TODO: исправить
            CameraControlChannel.getControl().filename = randName;

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
            //===========================
            CameraControlChannel.getControl().recognitionResult = new JSONObject();
            try
            {
                CameraControlChannel.getControl().recognitionResult.put("Link",randName);
            } catch (JSONException e) { e.printStackTrace(); }
            //===========================

            serverIsBusy = false;
            showToast("method is TakePhoto");
            return newFixedLengthResponse(CameraControlChannel.getControl().recognitionResult.toString());
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

            String randName = uri.substring(15);//костыль TODO: исправить
            CameraControlChannel.getControl().filename = randName;

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

            //=========
            try
            {
                CameraControlChannel.getControl().recognitionResult.put("Link",randName);
            }
            catch (JSONException e) { e.printStackTrace(); }
            //=========

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



    public static class UserHandler extends GeneralHandler {
        @Override
        public String getText() {
            return "UserA, UserB, UserC";
        }

        @Override
        public String getMimeType() {
            return MIME_PLAINTEXT;
        }

        @Override
        public Response.IStatus getStatus() {
            return Response.Status.OK;
        }
    }
}
