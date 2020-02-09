package com.vrlabdev.remotecameracontrol;

import android.app.Activity;
import android.content.Context;
import android.graphics.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.vrlabdev.remotecameracontrol.CameraStream.CameraControlChannel;
import com.vrlabdev.remotecameracontrol.CameraStream.VideoStream;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import  fi.iki.elonen.router.RouterNanoHTTPD;
import okhttp3.MediaType;
import okhttp3.Response;


public class MyHTTPD extends RouterNanoHTTPD {
    private static final int PORT = 1234;//8765

    private Context mContext=null;
    private Activity mActivity = null;

    private boolean serverIsBusy=false;

    private Handler mUiHandler = new Handler();

    private StoreHandler storeHandler;

    private SessionStack sessionStack;

    // ТЕСТОВАЯ ПОЕБЕНЬ \\
    public static class StoreHandler extends GeneralHandler {
        @Override
        public Response get(
                UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return newFixedLengthResponse("Retrieving store for id = "
                    + urlParams.get("storeId"));
        }
    }

    MyHTTPD()
    {
        super(PORT);
        sessionStack = new SessionStack();
        addMappings();
        storeHandler = new StoreHandler();
    }

    void setmContext(Context context)
    {
        mContext=context;
    }
    void setmActivity(Activity activity){mActivity=activity;}

    @Override
    public void addMappings() {
        super.addMappings();
        addRoute("/", MyHTTPD.class);
    }

    @Override
    public Response serve(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();

        //========================  сделать фото   ================================================================================================================================

        if (uri.contains("/TakePhoto")) {
            String randName = System.currentTimeMillis() / 1000 + ".jpg";

            boolean Q = CameraControlChannel.getControl().isRecordingVideo;

            if(Q)
            {
                showToast("method is GetRecogResult");
                return newFixedLengthResponse(Response.Status.CONFLICT,"application/json","отключите запись видео");
            }

            //если камера занята
            if(CameraControlChannel.getControl().isBusy)
            {
                //ждем пока камера не вернет изображение
                while (CameraControlChannel.getControl().isBusy) { }
                CameraControlChannel.getControl().jsonImageData = JSONBuilder("Empty","Empty",randName);
                return newFixedLengthResponse(Response.Status.OK, "application/json", CameraControlChannel.getControl().jsonImageData.toString());
            }
            //если камера свободна
            else {
                if (uri.length() > 10) randName = uri.substring(11);// TODO: исправить
                CameraControlChannel.getControl().filename = randName;

                CameraControlChannel.getControl().isBusy = true;

                Thread takeImage = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                CameraControlChannel.getControl().stream.takePicture(false);
                            }
                        });
                    }
                }
                );
                takeImage.start();

                //ждем пока камера не вернет изображение
                while (CameraControlChannel.getControl().isBusy) { }
                CameraControlChannel.getControl().jsonImageData = JSONBuilder("Empty","Empty",randName);

                showToast("method is TakePhoto");
                return newFixedLengthResponse(Response.Status.OK, "application/json", CameraControlChannel.getControl().jsonImageData.toString());
            }
        }


        //==============  сделать фото с распознаванием ================================================================================================================================

        if(uri.contains("/GetRecogResult"))
        {
            String randName = System.currentTimeMillis() / 1000 + ".jpg";

            if(CameraControlChannel.getControl().isRecordingVideo)
            {
                showToast("method is GetRecogResult");
                return newFixedLengthResponse(Response.Status.CONFLICT,"application/json","отключите запись видео");
            }

            if(CameraControlChannel.getControl().isBusy)
            {
                //ждем пока камера не вернет изображение
                while (CameraControlChannel.getControl().isBusy) { }
                try
                {
                    if(!CameraControlChannel.getControl().jsonImageData.has("Link"))
                        CameraControlChannel.getControl().jsonImageData.put("Link",randName);
                } catch (JSONException e) { e.printStackTrace(); }

                return newFixedLengthResponse(Response.Status.OK, "application/json", CameraControlChannel.getControl().jsonImageData.toString());
            }
            else {
                if(uri.length()>16) randName = uri.substring(16); //TODO: исправить
                CameraControlChannel.getControl().filename = randName;

                CameraControlChannel.getControl().isBusy=true;

                Thread myThread = new Thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run()
                            {
                                //CameraControlChannel.getControl().stream = new VideoStream(mContext,mActivity);
                                CameraControlChannel.getControl().stream.takePicture(true);
                            }
                        });
                    }
                });
                myThread.start();
                while(CameraControlChannel.getControl().isBusy){}
                try { CameraControlChannel.getControl().jsonImageData.put("Link",randName); }
                catch (JSONException e) { e.printStackTrace(); }

                String response = CameraControlChannel.getControl().jsonImageData.toString();

                showToast("method is GetRecogResult");
                return newFixedLengthResponse(Response.Status.OK,"application/json",response);
            }
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
                    try {
                        CameraControlChannel.getControl().stream = new VideoStream(mContext,mActivity);
                        CameraControlChannel.getControl().stream.startVideoRecord();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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

    private JSONObject JSONBuilder(String ContainerNumber,String ISOcode, String Link)
    {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("ContainerNumber",ContainerNumber);
            jsonObject.put("ISOcode",ISOcode);
            jsonObject.put("Link",Link);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
