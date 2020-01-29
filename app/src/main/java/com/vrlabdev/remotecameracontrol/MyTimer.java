package com.vrlabdev.remotecameracontrol;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.WIFI_SERVICE;

public class MyTimer extends TimerTask {

    Context mContext=null;
    String ServerResponse;
    JSONObject json;
    String mIp = "10.128.33.90";

    public MyTimer(Context context)
    {
        mContext=context;
    }

    public MyTimer(Context context, String ip)
    {
        mContext=context;
        mIp=ip;
    }
    @Override
    public void run()
    {

        WifiManager manager = (WifiManager) mContext.getSystemService(WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        String IPaddress = Formatter.formatIpAddress(info.getIpAddress());
        String MACaddress =getMacAddr();

        json = new JSONObject();
        try
        {
            json.put("Mac", MACaddress);
            json.put("Ip", IPaddress);
        } catch (JSONException e)
        {
            e.printStackTrace();
        }

        try
        {
            postRequest();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        Log.d("JSON_adresses", String.valueOf(json));

    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            //handle exception
        }
        return "";
    }

    class THREAD implements Runnable {
        public boolean HostIsReached;   //if IsReached Var = 1. If NotIsReached Var=0;

        public final MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

        String URL = "http://"+mIp+":8080/SmartGlass";
        String message = String.valueOf(json);

        @Override
        public void run()
        {
            OkHttpClient client = new OkHttpClient();
            //Log.d("POST_JSON", message);
            RequestBody body = RequestBody.create(mediaType, message);
            Request request = new Request.Builder().url(URL)
                    .post(body)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .build();
            Response response =null;
            Log.d("QWEQR", request.toString());
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    String mMessage = e.getMessage().toString();
                    Log.w("failure Response", mMessage);
                    //call.cancel();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    String mMessage = response.body().string();
                    Log.e("RESPONSE     ", mMessage);
                }
            });


            try {
                ServerResponse = response.body().string();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public boolean HostIsReachable()
        {
            return HostIsReached;
        }
    }



    public void postRequest() throws IOException {

        MediaType MEDIA_TYPE = MediaType.parse("application/json");
        String url = "http://"+mIp+":8080/SmartGlass";

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(MEDIA_TYPE, json.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String mMessage = e.getMessage().toString();
                Log.w("failure Response", mMessage);
                //call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String mMessage = response.body().string();
                Log.e("TAG", mMessage);
            }
        });
    }

}
