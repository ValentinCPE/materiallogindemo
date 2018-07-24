package com.sourcey.mbal.service.firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.sourcey.mbal.MainActivity;
import com.sourcey.mbal.PrefManager;
import com.sourcey.mbal.service.HttpRequestAsyncTaskJson;
import com.sourcey.mbal.service.PropertiesReader;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static android.content.ContentValues.TAG;

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        SharedPreferences sharedPref = getSharedPreferences("mbal", Context.MODE_PRIVATE);
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        sendRegistrationToServer(refreshedToken, sharedPref);
    }

    public static void sendRegistrationToServer(String tokenPhone, SharedPreferences sharedPref){
        String session_id = sharedPref.getString("sessionId",null);

        if(session_id != null){

            List<String> requestParameters = new ArrayList<>();
            requestParameters.add("Authorization");
            requestParameters.add("Basic bXktdHJ1c3RlZC1jbGllbnQ6c2VjcmV0");
            HttpRequestAsyncTaskJson httpRequestAsyncTaskJson = new HttpRequestAsyncTaskJson(requestParameters);
            httpRequestAsyncTaskJson.execute(PropertiesReader.properties.getProperty("url")+"oauth/token?grant_type=password&username=admin&password=Valentin34");

            try {
                String token = (String) httpRequestAsyncTaskJson.get().get("access_token");
                List<String> requestParametersSetToken = new ArrayList<>();
                requestParametersSetToken.add("Authorization");
                requestParametersSetToken.add("Bearer "+token);
                HttpRequestAsyncTaskJson httpRequestAsyncTask = new HttpRequestAsyncTaskJson(requestParametersSetToken);
                httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                        "api/user/setTokenForUser?session_id="+session_id+"&token_phone="+tokenPhone);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }



}
