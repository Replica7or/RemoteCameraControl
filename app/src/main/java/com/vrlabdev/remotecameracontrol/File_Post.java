package com.vrlabdev.remotecameracontrol;

import android.util.Log;

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
    HttpURLConnection con = null;
    public void TransieveFile(File file, String DestiationFolderName)
    {
        try
        {

           m_Url = new URL("http://10.128.33.90/api/post/save_ctk.php");
            //m_Url = new URL("https://untwisted-trailers.000webhostapp.com/api/post/save_ctk.php");
            final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
            String path = "Photos_"+"photos"+"_"+"recognition"+"_"+(new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())).toString()+".png";


            //RequestBody requestImageFile = RequestBody.create(file,MEDIA_TYPE_PNG);
            //MultipartBody.Part image = MultipartBody.Part.createFormData(path, file.getAbsolutePath(), requestImageFile);
            MultipartBody multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("userfile", path, RequestBody.Companion.create(file,MEDIA_TYPE_PNG)).build();
            //RequestBody req = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("userfile", path, RequestBody.create(file,MEDIA_TYPE_PNG)).build();



            Request request = new Request.Builder().url(m_Url).post(multipartBody).build();

            OkHttpClient client;
            client = new OkHttpClient().newBuilder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            Response response = client.newCall(request).execute();

            Log.d("response", "uploadImage:"+response.body().string());






           // HttpResponse response1 = client.execute(post);



           /* con = (HttpURLConnection) m_Url.openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestProperty("content-length", "24616");
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=--------------------------290299153002865238385305");

            DataOutputStream os = new DataOutputStream(con.getOutputStream());

            //entity.writeTo(os);

            InputStream IS;

            int status = con.getResponseCode();
            Log.d("STATUS", String.valueOf(status));

            if (status != HttpURLConnection.HTTP_OK)
            {
                IS = con.getErrorStream();
                con.getErrorStream();
                InputStreamReader inputStream = new InputStreamReader(IS);
                BufferedReader reader = new BufferedReader(inputStream);
                for (String line; (line = reader.readLine()) != null; )
                {
                    System.out.println(line);
                }
                reader.close();
            } else
            {
                IS = con.getInputStream();
                InputStream in = new BufferedInputStream(IS);
                Log.d("RESPONSE", convertStreamToString(in));
                in.close();
            }

            os.flush();
            os.close();
            con.disconnect();
*/

        } catch (Exception exception)
        {
            exception.getMessage();
            Log.d("ERROR",exception.getMessage());
        }


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
