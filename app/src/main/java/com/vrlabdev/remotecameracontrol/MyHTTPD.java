package com.vrlabdev.remotecameracontrol;

import android.content.Context;
import android.os.Handler;

import com.vrlabdev.remotecameracontrol.CameraStream.CameraControlChannel;
import com.vrlabdev.remotecameracontrol.CameraStream.CameraMode;
import com.vrlabdev.remotecameracontrol.CameraStream.VideoStream;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class MyHTTPD extends NanoHTTPD {
    public static final int PORT = 8765;

    Context mContext=null;
    private Handler mUiHandler = new Handler();

    public MyHTTPD() throws IOException
    {
        super(PORT);
    }

    public void setmContext(Context context)
    {
        mContext=context;
    }
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if (uri.equals("/TakePhoto")) {
            String response = "HelloWorld";
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {

                            CameraControlChannel.getControl().stream = new VideoStream(mContext);
                            CameraControlChannel.getControl().stream.takePicture();
                            //CameraControlChannel.getControl().stream.CameraStart(CameraMode.STREAM_DRAWING_SURFACE_MODE);


                }
            });

            return newFixedLengthResponse(response);
        }

        if (uri.equals("/StartVideo")) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraControlChannel.getControl().stream = new VideoStream(mContext);
                    CameraControlChannel.getControl().stream.startVideoRecord();
                }
            });
            String response = "Start record";
            return newFixedLengthResponse(response);
        }

        if (uri.equals("/StopVideo")) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraControlChannel.getControl().stream.stopVideoRecord();
                    //CameraControlChannel.getControl().stream = new VideoStream(mContext);
                    //CameraControlChannel.getControl().stream.CameraStart(CameraMode.STREAM_DRAWING_SURFACE_MODE);
                }
            });
            String response = "stop record";
            return newFixedLengthResponse(response);
        }

        if(uri.equals("/GetRecogResult"))
        {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraControlChannel.getControl().stream = new VideoStream(mContext);
                    String path = CameraControlChannel.getControl().stream.takePicture();
                }
            });
        }
        String response = "stop record";
        return newFixedLengthResponse(response);
    }
}
