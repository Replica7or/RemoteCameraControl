package com.vrlabdev.remotecameracontrol.CameraStream;

import org.json.JSONObject;

public class CameraControlChannel {
    private static final CameraControlChannel ourInstance = new CameraControlChannel();

    public VideoStream stream;
    public boolean isBusy = false;

    public JSONObject recognitionResult = null;

    public String filename=null;

    public static CameraControlChannel getControl() {
        return ourInstance;
    }
    private CameraControlChannel() {
    }
}
