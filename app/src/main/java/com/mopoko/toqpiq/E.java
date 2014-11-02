package com.mopoko.toqpiq;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class E {
    public static void ex(Exception e) {
        Log.e("ToqXKCD", e.getMessage(), e);
    }
    public static String inputUTF8StreamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();

        char buf[] = new char[14];

        for(int i;(i = br.read(buf)) > 0; sb.append(buf, 0, i));

        return sb.toString();
    }

}
