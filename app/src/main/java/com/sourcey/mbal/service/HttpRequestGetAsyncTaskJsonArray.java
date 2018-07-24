package com.sourcey.mbal.service;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by valen on 06/03/2018.
 */

public class HttpRequestGetAsyncTaskJsonArray extends AsyncTask<String , Void, JSONArray> {

    public List<String> requests;

    static InputStream is;
    static JSONArray mJsonObject = null;
    static String json = "";
    String TAG = getClass().getSimpleName();

    public HttpRequestGetAsyncTaskJsonArray(List<String> requests){
        this.requests = requests;
    }

    @Override
    protected JSONArray doInBackground(String... params){

        String data = "";

        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(params[0]);

            int i = 0;
            while(i<requests.size()){
                get.addHeader(requests.get(i),requests.get(i+1));
                i += 2;
            }

            HttpResponse response = client.execute(get);

            int status = response.getStatusLine().getStatusCode();
            System.out.println(status);
            if(status == 200) {
                HttpEntity httpEntity = response.getEntity();
                is = httpEntity.getContent();

                BufferedReader mBufferedReader = null;
                if (is != null) {
                    try {
                        mBufferedReader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
                        StringBuilder builder = new StringBuilder();
                        String line = null;
                        while ((line = mBufferedReader.readLine()) != null) {
                            builder.append(line + "\n");
                        }
                        is.close();
                        json = builder.toString();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        // Convert the JSON String from InputStream to a JSONObject
                        mJsonObject = new JSONArray(json);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception: " + e.getMessage());
                    }
                    return mJsonObject;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void onPostExecute(JSONArray result){
        super.onPostExecute(result);
    }

}
