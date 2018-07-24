package com.sourcey.mbal.service;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by valen on 28/03/2018.
 */

public class PropertiesReader {

    private static Context context;
    public static Properties properties;

    public static void initProperties(Context contextClient){
        context=contextClient;
        properties = new Properties();
        getMyProperties("appConfig.properties");
    }

    private static Properties getMyProperties(String file) {
        try {
            AssetManager assetManager = context.getAssets();
            String[] names = assetManager.list( "" );
            InputStream inputStream = assetManager.open(file);
            properties.load(inputStream);

        } catch (Exception e) {
            System.out.print(e.getMessage());
        }

        return properties;
    }

}
