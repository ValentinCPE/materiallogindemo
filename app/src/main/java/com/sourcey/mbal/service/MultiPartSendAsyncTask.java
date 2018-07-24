package com.sourcey.mbal.service;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by valen on 28/03/2018.
 */

public class MultiPartSendAsyncTask extends AsyncTask<String , Void, JSONObject> {

    public String REQUEST_METHOD;
    public static final int READ_TIMEOUT = 15000;
    public static final int CONNECTION_TIMEOUT = 15000;

    private List<String> requests;
    private List<Uri> nameValuePairs;

    private Context context;

    static InputStream is;
    private InputStream inputStream;
    static JSONObject mJsonObject = null;
    static String json = "";
    String TAG = getClass().getSimpleName();

    public MultiPartSendAsyncTask(List<String> requests, Context context, List<Uri> nameValuePairs){
        this.requests = requests;
        this.context = context;
        this.nameValuePairs = nameValuePairs;
    }

    @Override
    protected JSONObject doInBackground(String... params){

        String data="";
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        HttpPost httpPost = new HttpPost(params[0]);

        try {
            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

            int i = 0;
            while(i<requests.size()){
                httpPost.addHeader(requests.get(i),requests.get(i+1));
                i += 2;
            }

            for(int index=0; index < nameValuePairs.size(); index++) {
                    entity.addPart("session_id",new StringBody(params[1]));
                    entity.addPart("uploadfile", new FileBody(new File(this.getRealPathFromUri(nameValuePairs.get(index),context))));
            }

            httpPost.setEntity(entity);

            HttpResponse response = httpClient.execute(httpPost, localContext);

            int status = response.getStatusLine().getStatusCode();
            System.out.println(status);
            if(status == 200){
                HttpEntity httpEntity = response.getEntity();
                inputStream = httpEntity.getContent();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedReader mBufferedReader = null;
        try {
            mBufferedReader = new BufferedReader(new InputStreamReader(inputStream, "iso-8859-1"), 8);
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = mBufferedReader.readLine()) != null) {
                builder.append(line + "\n");
            }
            inputStream.close();
            json = builder.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // Convert the JSON String from InputStream to a JSONObject
            mJsonObject = new JSONObject(json);
        } catch (JSONException e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }

        return mJsonObject;
    }

    protected void onPostExecute(JSONObject result){
        super.onPostExecute(result);
    }

    public static String getRealPathFromUri(Uri contentUri, Context context) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
