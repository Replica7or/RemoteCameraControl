package com.vrlabdev.remotecameracontrol.CameraStream;

import com.vrlabdev.remotecameracontrol.R;

import org.json.JSONObject;

public class CameraControlChannel {
    private static final CameraControlChannel ourInstance = new CameraControlChannel();

    public VideoStream stream;
    public boolean isBusy = false;

    public JSONObject jsonImageData = null;

    public String filename=null;

    public String fileUploadPath ="http://10.128.33.90:8080/SmartGlass/File";

    public static CameraControlChannel getControl() {
        return ourInstance;
    }
    private CameraControlChannel() {
    }
}
