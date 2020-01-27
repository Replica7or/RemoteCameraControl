package com.vrlabdev.remotecameracontrol;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileWork {

    //  Метод для записи в файл
    public void writeToFile(File file, String text) {
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(text);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("IOException", "Ошибка записи в файл "+file.getPath());
        }
    }

    public String readFromFile(File file)
    {
        try{
            FileInputStream fis = new FileInputStream(file);
            BufferedReader bfr = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bfr.readLine()) != null)
            {
                sb.append(line).append('\n');
            }
            fis.close();
            bfr.close();
            return sb.toString();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            Log.d("IOException", "Ошибка чтения файла "+file.getPath());
        }
        return null;
    }
}
