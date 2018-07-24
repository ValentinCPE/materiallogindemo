package com.sourcey.mbal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.sourcey.mbal.service.HttpRequestAsyncTaskJson;
import com.sourcey.mbal.service.PropertiesReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class PrefManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;
    // shared pref mode
    int PRIVATE_MODE = 0;
    // Shared preferences file name
    private static final String PREF_NAME = "mbal";
    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }
    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }
    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    public void setToken(){
        List<String> requestParameters = new ArrayList<>();
        requestParameters.add("Authorization");
        requestParameters.add("Basic bXktdHJ1c3RlZC1jbGllbnQ6c2VjcmV0");
        HttpRequestAsyncTaskJson httpRequestAsyncTaskJson = new HttpRequestAsyncTaskJson(requestParameters);
        httpRequestAsyncTaskJson.execute(PropertiesReader.properties.getProperty("url")+"oauth/token?grant_type=password&username=admin&password=Valentin34");

        try {
            if(httpRequestAsyncTaskJson.get() != null) {
                String token = (String) httpRequestAsyncTaskJson.get().get("access_token");
                editor.putString("token", token);
            }else{
                editor.remove("token");
            }
            editor.commit();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}
