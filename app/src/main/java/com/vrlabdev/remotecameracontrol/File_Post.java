package com.vrlabdev.remotecameracontrol;

import android.util.Log;

import com.vrlabdev.remotecameracontrol.CameraStream.CameraControlChannel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class File_Post {
    private URL m_Url;
    private Date date = new Date();
    public void TransieveFile(File file)
    {
        try
        {
            //m_Url = new URL("http://192.168.31.142:8080/SmartGlass/File");            //TODO: адрес для лабы
            m_Url = new URL(CameraControlChannel.getControl().fileUploadPath);        //TODO: адрес для порта
            m_Url=new URL(CameraControlChannel.getControl().fileUploadPath+"/"+ date.getDate()+String.valueOf(date.getMonth())+String.valueOf(date.getYear())+"/"+date.getHours()+"/"+CameraControlChannel.getControl().filename+"/"+"Photo");

            final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");


            //TODO: раскомментировать здесь для работы с портом
            MultipartBody multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM).addFormDataPart("userfile",System.currentTimeMillis() / 1000+".jpg",
                    RequestBody.Companion.create(file,MEDIA_TYPE_PNG)).build();

            Request request = new Request.Builder().url(m_Url)
                    .addHeader("Content-type","multipart/form-data")
                    .addHeader("Accept","application/json")
                    .post(multipartBody).build();

            OkHttpClient client;

            client = new OkHttpClient().newBuilder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS).build();
            Response response = client.newCall(request).execute();


            Log.d("response", "uploadImage:"+response.body().string());
        }
        catch (Exception exception)
        {
            exception.getMessage();
            Log.d("ERROR",exception.getMessage());
        }
    }
    public void UploadVideo(File file)
    {
        try
        {
            //m_Url = new URL("http://192.168.31.142:8080/SmartGlass/File");            //TODO: адрес для лабы
            m_Url = new URL(CameraControlChannel.getControl().fileUploadPath);        //TODO: адрес для порта
            m_Url=new URL(CameraControlChannel.getControl().fileUploadPath+"/"+ date.getDate()+String.valueOf(date.getMonth())+String.valueOf(date.getYear())+"/"+date.getHours()+"/"+CameraControlChannel.getControl().videoName+"/"+"Video");
            final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");


            //TODO: раскомментировать здесь для работы с портом
            MultipartBody multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM).addFormDataPart("userfile",System.currentTimeMillis() / 1000+".mp4",
                            RequestBody.Companion.create(file,MEDIA_TYPE_PNG)).build();

            Request request = new Request.Builder().url(m_Url)
                    .addHeader("Content-type","multipart/form-data")
                    .addHeader("Accept","application/json")
                    .post(multipartBody).build();

            OkHttpClient client;

            client = new OkHttpClient().newBuilder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS).build();
            Response response = client.newCall(request).execute();

            Log.d("response", "uploadImage:"+response.body().string());
        }
        catch (Exception exception)
        {
            exception.getMessage();
            Log.d("ERROR",exception.getMessage());
        }
    }
}
