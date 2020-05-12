package com.example.terminalbluetooth;

import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class SendServerAsyncT {
    public static String result;

    public void sendPost(final String urlSend, final String[] message, final String id_stup, final String id_stupina,
                         final String nr_rame, final boolean antifurt) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlSend);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("precipitatii", message[0]);
                    jsonParam.put("temperatura", message[1]);
                    jsonParam.put("presiune", message[2]);
                    jsonParam.put("id_stup", id_stup);
                    jsonParam.put("nr_rame", nr_rame);
                    jsonParam.put("id_stupina", id_stupina);
                    jsonParam.put("lumina", message[3].replace(";", ""));
                    jsonParam.put("antifurt", antifurt);

                    Log.i("JSON", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG", conn.getResponseMessage());
                    result = conn.getResponseMessage();
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    result = e.toString();
                }
            }
        });

        thread.start();
    }
}
