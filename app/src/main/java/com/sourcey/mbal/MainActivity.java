package com.sourcey.mbal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;
import com.sourcey.mbal.service.DownloadImageTask;
import com.sourcey.mbal.service.HttpRequestAsyncTaskJson;
import com.sourcey.mbal.service.HttpRequestGetAsyncTask;
import com.sourcey.mbal.service.HttpRequestGetAsyncTaskJson;
import com.sourcey.mbal.service.HttpRequestGetAsyncTaskJsonArray;
import com.sourcey.mbal.service.MultiPartSendAsyncTask;
import com.sourcey.mbal.service.PropertiesReader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.stfalcon.multiimageview.MultiImageView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import butterknife.Bind;
import butterknife.ButterKnife;
import okhttp3.WebSocket;
import tarek360.animated.icons.AnimatedIconView;
import tarek360.animated.icons.IconFactory;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.client.StompClient;
import xdroid.toaster.Toaster;

public class MainActivity extends AppCompatActivity {

    public static final int PICK_IMAGE = 1;
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL = 2;

    @Bind(R.id.photoProfil) ImageView photoProfil;
    @Bind(R.id.familyName) TextView familyName;
    @Bind(R.id.numberFamily) TextView numberFamily;
    @Bind(R.id.name) TextView name;
    @Bind(R.id.lastEvent) TextView lastEvent;
    @Bind(R.id.clickImage) LinearLayout clickImage;
    @Bind(R.id.logout) ImageView logout;
    @Bind(R.id.animatedIconView) AnimatedIconView animatedIconView;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    private String sessionId;
    private String token;
    private String username;

    private JSONObject jsonUser;
    private JSONArray jsonUsersForFamily;

    private MultiImageView imagesPopup;
    private Dialog settingsDialog;

    private StompClient mStompClient;
    private Long hasNewCourriel;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref= getSharedPreferences("mbal", Context.MODE_PRIVATE);
        editor=sharedPref.edit();

        ButterKnife.bind(this);

        animatedIconView.setAnimatedIcon(IconFactory.iconNotificationAlert());

        if(sharedPref.getString("username","").equals("") && sharedPref.getString("password","").equals("")){
            startActivity(new Intent(MainActivity.this, LaunchActivity.class));
            finish();
        }else{
            PropertiesReader.initProperties(MainActivity.this);
            PrefManager prefManager = new PrefManager(MainActivity.this);
            prefManager.setToken();

            if(isNetworkConnected() && !sharedPref.getString("token","").equals("")){
                mStompClient = Stomp.over(WebSocket.class, PropertiesReader.properties.getProperty("url") + "ws");
                mStompClient.connect();

                mStompClient.lifecycle().subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {

                        case OPENED:
                            Log.d("INIT WEBSOCKET", "Stomp connection opened");
                            break;

                        case ERROR:
                            Log.e("INIT WEBSOCKET", "Error", lifecycleEvent.getException());
                            break;

                        case CLOSED:
                            Log.d("INIT WEBSOCKET", "Stomp connection closed");
                            break;
                    }
                });

                this.token = sharedPref.getString("token", "");
                this.sessionId = sharedPref.getString("sessionId", "");
                this.username = sharedPref.getString("username", "");

                if(!this.isSessionIdCorrect(this.username,this.sessionId)){
                    editor.clear().apply();
                    mStompClient.disconnect();
                    Toast.makeText(this,"Votre session a expiré ! Veuillez vous reconnecter !", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(MainActivity.this, LaunchActivity.class));
                    finish();
                }

                List<String> requestParameters = new ArrayList<>();
                requestParameters.add("Authorization");
                requestParameters.add("Bearer " + this.token);

                HttpRequestAsyncTaskJson httpRequestAsyncTaskUser = new HttpRequestAsyncTaskJson(requestParameters);
                httpRequestAsyncTaskUser.execute(PropertiesReader.properties.getProperty("url") +
                        "api/user/getUserByName?username="+this.username);

                try {
                    this.jsonUser = httpRequestAsyncTaskUser.get();

                    if (this.jsonUser != null && !this.jsonUser.getJSONObject("family").getString("name").equals("")) {
                        String nameFinal = this.jsonUser.getString("prenom") + " " + this.jsonUser.getString("nom");
                        name.setText(nameFinal);
                        familyName.setText(this.jsonUser.getJSONObject("family").getString("name"));

                        FirebaseMessaging.getInstance().subscribeToTopic("/topics/"+familyName.getText().toString());

                        mStompClient.topic("/alert/event/" + this.jsonUser.getJSONObject("family").getInt("id")).subscribe(topicMessage -> {
                            runOnUiThread(() -> animatedIconView.startAnimation());
                            Thread.sleep(2000);
                            Intent i = new Intent(MainActivity.this, NewEventActivity.class);
                            i.putExtra("date",new Date().getTime());
                            startActivity(i);
                            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                            runOnUiThread(() -> animatedIconView.setAnimatedIcon(IconFactory.iconNotificationAlert().setNotificationCount(0)));
                        });

                        HttpRequestGetAsyncTaskJsonArray httpRequestAsyncTaskUserFamily = new HttpRequestGetAsyncTaskJsonArray(requestParameters);
                        httpRequestAsyncTaskUserFamily.execute(PropertiesReader.properties.getProperty("url") +
                                "api/user/getUsersByFamilyName/" + this.jsonUser.getJSONObject("family").getString("name"));

                        this.jsonUsersForFamily = httpRequestAsyncTaskUserFamily.get();

                        if (this.jsonUsersForFamily != null) {
                            numberFamily.setText(String.valueOf(this.jsonUsersForFamily.length()));
                            settingsDialog = new Dialog(MainActivity.this);
                            settingsDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                            settingsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                            settingsDialog.setContentView(getLayoutInflater().inflate(R.layout.image_layout, null));
                            imagesPopup = (MultiImageView) settingsDialog.findViewById(R.id.multipleImageFamily);
                            getPathProfileFamily();
                        } else {
                            numberFamily.setText("0");
                        }

                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                this.setLastEvent();

                if(this.hasNewCourriel != null){
                    animatedIconView.startAnimation();
                    animatedIconView.setOnClickListener(v -> {
                        Intent i = new Intent(MainActivity.this, NewEventActivity.class);
                        i.putExtra("date",this.hasNewCourriel);
                        startActivity(i);
                        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                        runOnUiThread(() -> animatedIconView.setAnimatedIcon(IconFactory.iconNotificationAlert().setNotificationCount(0)));
                    });
                }

                this.getProfilePicture();

                photoProfil.setOnClickListener(v -> {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                        // Permission is not granted
                        // Should we show an explanation?
                        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                Manifest.permission.READ_EXTERNAL_STORAGE)) {

                        } else {
                            // No explanation needed; request the permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL);
                        }
                    }
                    Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    getIntent.setType("image/*");

                    Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    pickIntent.setType("image/*");

                    Intent chooserIntent = Intent.createChooser(getIntent, "Nouvelle photo de profil");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});

                    startActivityForResult(chooserIntent, PICK_IMAGE);
                });

                logout.setOnClickListener(v -> logout());

                clickImage.setOnClickListener(v -> {

                    settingsDialog.setOnShowListener(dialog -> {
                        RelativeLayout layout = (RelativeLayout) settingsDialog.findViewById(R.id.dialogImages);
                        imagesPopup.setShape(MultiImageView.Shape.CIRCLE);
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        int height = displayMetrics.heightPixels;
                        int width = displayMetrics.widthPixels;
                        layout.getLayoutParams().height = height / 2;
                        layout.getLayoutParams().width = width - 100;

                        imagesPopup.getLayoutParams().height = height / 2;
                        imagesPopup.getLayoutParams().width = width - 100;
                    });
                    settingsDialog.show();
                });
            }else{
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Nous ne parvenons pas à joindre le serveur ! Réessayez plus tard !")
                        .setPositiveButton("Quitter", (dialog, which) -> System.exit(0))
                        .setIcon(R.drawable.logo);
                builder.create();
                builder.show();
            }
        }
    }

    private void getPathProfileFamily(){
        try {
            if(jsonUsersForFamily.length()>0){
                for(int i = 0; i < jsonUsersForFamily.length(); i++){
                    String path = jsonUsersForFamily.getJSONObject(i).getString("path_profile_picture");
                    new DownloadImageTask(MainActivity.this,path,imagesPopup).execute();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void logout(){
        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Logout...");
        progressDialog.show();

        List<String> requestParameters = new ArrayList<>();
        requestParameters.add("Authorization");
        requestParameters.add("Bearer "+token);
        HttpRequestGetAsyncTaskJson httpRequestAsyncTask = new HttpRequestGetAsyncTaskJson(requestParameters);
        httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                "api/user/logout/"+sessionId);

        try {
            final String response = (String) httpRequestAsyncTask.get().get("response");

            new android.os.Handler().postDelayed(
                    () -> {
                        if(response.toUpperCase().equals("OK")){
                            editor.clear().commit();
                            progressDialog.dismiss();
                            mStompClient.disconnect();
                            startActivity(new Intent(MainActivity.this, LaunchActivity.class));
                            finish();
                        }else{
                            //TODO : erreur logout
                        }
                    }, 3000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == PICK_IMAGE) {
            if(data != null){
                sendImage(data.getData());
            }
        }
    }

    private void sendImage(Uri uri){
        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Sending Image...");
        progressDialog.show();
        List<Uri> nameValuePairs = new ArrayList<>();
        if(uri != null){
            List<String> requestParameters = new ArrayList<>();
            requestParameters.add("Authorization");
            requestParameters.add("Bearer "+token);
            nameValuePairs.add(uri);
            MultiPartSendAsyncTask multiPartSendAsyncTask = new MultiPartSendAsyncTask(requestParameters,MainActivity.this,nameValuePairs);
            multiPartSendAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                    "api/user/setProfilePicture", sessionId);

            try {
                String response = (String) multiPartSendAsyncTask.get().get("response");
                if(response.equals("OK")){
                    progressDialog.dismiss();
                    this.getProfilePicture();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mStompClient != null){
            mStompClient.disconnect();
        }
    }

    private void getProfilePicture(){
        List<String> requestParameters = new ArrayList<>();
        requestParameters.add("Authorization");
        requestParameters.add("Bearer "+this.token);

        HttpRequestGetAsyncTaskJson httpRequestAsyncTask = new HttpRequestGetAsyncTaskJson(requestParameters);
        httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                "api/user/getPathProfilePicture/"+this.sessionId);
        String path = null;
        try {
            if(httpRequestAsyncTask.get() != null)
                path = (String) httpRequestAsyncTask.get().get("response");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(path != null && !path.toUpperCase().equals("NO CONTENT")){
            final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this,
                    R.style.AppTheme_Dark_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Fetching Image...");
            progressDialog.show();
            final AtomicBoolean playAnimation = new AtomicBoolean(true);
            Picasso.with(this).load(PropertiesReader.properties.getProperty("url") +
                    "public/files/"+path).transform(new CircleTransform()).into(photoProfil, new Callback() {
                @Override
                public void onSuccess() {
                    if(playAnimation.get()){
                        progressDialog.dismiss();
                        Animation fadeOut = new AlphaAnimation(0, 1);
                        fadeOut.setInterpolator(new AccelerateInterpolator());
                        fadeOut.setDuration(1000);
                        photoProfil.startAnimation(fadeOut);
                    }
                }

                @Override
                public void onError() {
                    progressDialog.dismiss();
                    Picasso.with(MainActivity.this).load(R.drawable.user).transform(new CircleTransform()).into(photoProfil);
                }
            });
        }else{
            Picasso.with(MainActivity.this).load(R.drawable.user).transform(new CircleTransform()).into(photoProfil);
        }
    }

    private void setLastEvent(){
        List<String> requestParameters = new ArrayList<>();
        requestParameters.add("Authorization");
        requestParameters.add("Bearer "+this.token);

        HttpRequestGetAsyncTaskJson httpRequestAsyncTask = new HttpRequestGetAsyncTaskJson(requestParameters);
        try {
            httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                    "api/event/getLastEventByFamily/"+this.jsonUser.getJSONObject("family").getString("name"));

            JSONObject lastEvent = httpRequestAsyncTask.get();

            if(lastEvent != null && !lastEvent.isNull("timestamp")) {
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.FRANCE);
                String dateString = formatter.format(new Date(lastEvent.getLong("timestamp")));
                this.lastEvent.setText(dateString);

                if(lastEvent.getString("message").equals("NEW_COURRIEL")){
                    this.hasNewCourriel = lastEvent.getLong("timestamp");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isSessionIdCorrect(String username, String sessionIdToTest){
        boolean isCorrect = false;

        List<String> requestParameters = new ArrayList<>();
        requestParameters.add("Authorization");
        requestParameters.add("Bearer "+this.token);

        HttpRequestGetAsyncTaskJson httpRequestAsyncTask = new HttpRequestGetAsyncTaskJson(requestParameters);
        try {
            httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                    "api/user/getSessionIdByUsername/" + username + "/ANDROID");

            JSONObject sessionId = httpRequestAsyncTask.get();

            if(sessionId != null && !sessionId.isNull("response")){
                if(sessionId.getString("response").equals(sessionIdToTest)){
                    isCorrect = true;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return isCorrect;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting()) {

            return true;

        } else {

            return false;

        }
    }
}
