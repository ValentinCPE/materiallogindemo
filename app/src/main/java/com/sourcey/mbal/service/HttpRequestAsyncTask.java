package com.sourcey.mbal.service;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
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

public class HttpRequestAsyncTask extends AsyncTask<String , Void, String> {

    public String REQUEST_METHOD;
    public static final int READ_TIMEOUT = 15000;
    public static final int CONNECTION_TIMEOUT = 15000;

    public List<String> requests;

    static InputStream is;
    String TAG = getClass().getSimpleName();

    public HttpRequestAsyncTask(List<String> requests, String requestmethod){
        this.requests = requests;
        this.REQUEST_METHOD = requestmethod;
    }

    @Override
    protected String doInBackground(String... params){

        String data = "";

        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(params[0]);

            int i = 0;
            while(i<requests.size()){
                post.addHeader(requests.get(i),requests.get(i+1));
                i += 2;
            }

            HttpResponse response = client.execute(post);

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
