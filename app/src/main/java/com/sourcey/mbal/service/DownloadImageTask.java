package com.sourcey.mbal.service;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.sourcey.mbal.CircleTransform;
import com.sourcey.mbal.R;
import com.squareup.picasso.Picasso;
import com.stfalcon.multiimageview.MultiImageView;

import java.io.InputStream;
import java.net.URL;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

    private String path;
    private MultiImageView multiImageView;
    private Context context;

    public DownloadImageTask(Context context, String path, MultiImageView multiImageView) {
        this.path = path;
        this.multiImageView = multiImageView;
        this.context = context;
    }

    protected Bitmap doInBackground(String... urls) {
        Bitmap mIcon11 = null;
        try {
            if(this.path != null && !this.path.isEmpty() && !this.path.equals("null")){
                URL url = new URL(PropertiesReader.properties.getProperty("url") +
                        "public/files/"+path);
                mIcon11 = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            }else{
                return null;
            }
        } catch (Exception e) {
            Log.e("Error", "image download error");
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        if(result != null){
            multiImageView.addImage(result);
        }else{
            Bitmap icon = BitmapFactory.decodeResource(this.context.getResources(),
                    R.drawable.user);
            multiImageView.addImage(icon);
        }
    }
}