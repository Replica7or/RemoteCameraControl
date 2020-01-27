package com.vrlabdev.remotecameracontrol.CameraStream;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class CameraServer  extends HandlerThread {

    private Context mContext;
    private ServerSocket mServSocket;
    private String ip_address = "192.168.31.182";//"10.128.33.90";
    private InetAddress address;
    private int port = 5010;
    ServerSocket srvSocket;
    Socket socket;
    String command;



    private Handler mUiHandler = new Handler();

    public CameraServer(String name, Context context) {
        super(name);
        this.mContext = context;
        try {
            mServSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //myThread.start();
    }

   /* Thread myThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while(true) {
                try {
                    socket = mServSocket.accept();

                    DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));     //need close
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));                          //need close
                    command = convertStreamToString(reader);
                    in.close();
                    reader.close();


                    Log.d("TAG", "handleMessage " + command + " in " + Thread.currentThread());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(command.contains("start")) {
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            switch (CameraControlChannel.getControl().stream.getMode()) {
                                case 0:
                                    CameraControlChannel.getControl().stream = new VideoStream(mContext,);
                                    CameraControlChannel.getControl().stream.CameraStart(CameraMode.STREAM_DRAWING_SURFACE_MODE);
                                    break;
                                case 1:
                                    CameraControlChannel.getControl().stream = new VideoStream(mContext);
                                    CameraControlChannel.getControl().stream.CameraStart(CameraMode.STREAM_MODE);
                                    break;
                                case 2:
                                    //CameraControlChannel.getControl().stream = new VideoStream(mContext);
                                   // CameraControlChannel.getControl().stream.CameraStart(CameraMode.STREAM_DRAWING_SURFACE_MODE);
                                    break;
                                default:
                                    CameraControlChannel.getControl().stream = new VideoStream(mContext);
                                    CameraControlChannel.getControl().stream.CameraStart(CameraMode.STREAM_MODE);
                                    break;
                            }
                        }
                    });
                }

                if(command.contains("stop"))
                {
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            switch (CameraControlChannel.getControl().stream.getMode()) {
                                case 0:
                                    break;
                                case 1:
                                    CameraControlChannel.getControl().stream.StopStream();
                                    CameraControlChannel.getControl().stream = new VideoStream(mContext);
                                    break;
                                case 2:
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                }
            }
        }
    });

    private String convertStreamToString(BufferedReader reader)
    {
        StringBuilder sb = new StringBuilder();

        String line;
        try
        {
            while ((line = reader.readLine()) != null)
            {
                sb.append(line).append('\n');
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return sb.toString();
    }*/
}