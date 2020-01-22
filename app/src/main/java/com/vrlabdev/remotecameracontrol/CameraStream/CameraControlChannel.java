package com.vrlabdev.remotecameracontrol.CameraStream;

public class CameraControlChannel {
    private static final CameraControlChannel ourInstance = new CameraControlChannel();

    public VideoStream stream;

    public static CameraControlChannel getControl() {
        return ourInstance;
    }
    private CameraControlChannel() {
    }
}
