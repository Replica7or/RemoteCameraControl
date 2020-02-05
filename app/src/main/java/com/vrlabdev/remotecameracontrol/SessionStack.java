package com.vrlabdev.remotecameracontrol;

import java.util.ArrayList;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

public class SessionStack {
    List<NanoHTTPD.IHTTPSession> sessionsTakePhoto = new ArrayList<>();
    List<NanoHTTPD.IHTTPSession> sessionsRecognition = new ArrayList<>();
}
