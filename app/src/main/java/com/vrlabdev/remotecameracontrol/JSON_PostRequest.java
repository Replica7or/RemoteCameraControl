package com.vrlabdev.remotecameracontrol;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class JSON_PostRequest {
    private URL m_Url;
    //InputStream is = null;
    HttpURLConnection conn = null;
    int status = 400;

    public int Post(String url, String message)
    {
        try
        {
            m_Url = new URL(url);
            JSONObject json = new JSONObject();
            try
            {
                json.put("CallSign", "");
                json.put("VoyageNumber", "049/050");
            } catch (Exception e)
            {

            }


            Log.d("POST_JSON", message);

            conn = (HttpURLConnection) m_Url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("accept","application/json");
            //conn.setRequestProperty("content-lenght", "300");


            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(message);
            InputStream IS;
            status = conn.getResponseCode();
            Log.d("STATUS", String.valueOf(status));
            if (status != HttpURLConnection.HTTP_OK)
            {
                IS = conn.getErrorStream();
                InputStream in = new BufferedInputStream(IS);
                Log.d("RESPONSE_ERROR_TEXT",convertStreamToString(in));
                in.close();

            }
            else
            {
                IS = conn.getInputStream();
                InputStream in = new BufferedInputStream(IS);
                Log.d("RESPONSE",convertStreamToString(in));
                in.close();
            }

            os.flush();
            os.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Log.d("POST_JSON", e.getMessage());

        }
        finally { conn.disconnect(); }

        return status;
    }


    private String convertStreamToString(InputStream is)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
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
        finally
        {
            try
            {
                is.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
