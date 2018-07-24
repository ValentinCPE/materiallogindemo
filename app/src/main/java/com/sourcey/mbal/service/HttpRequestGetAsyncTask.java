package com.sourcey.mbal.service;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by valen on 13/03/2018.
 */

public class HttpRequestGetAsyncTask extends AsyncTask<String , Void, String> {

    public static final int READ_TIMEOUT = 15000;
    public static final int CONNECTION_TIMEOUT = 15000;

    public List<String> requests;

    static InputStream is;
    String TAG = getClass().getSimpleName();

    public HttpRequestGetAsyncTask(List<String> requests){
        this.requests = requests;
    }

    @Override
    protected String doInBackground(String... params){

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
            if(status == 200){
                HttpEntity httpEntity = response.getEntity();
                data = EntityUtils.toString(httpEntity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    protected void onPostExecute(String result){
        super.onPostExecute(result);
    }

}
