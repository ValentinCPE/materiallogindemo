package com.sourcey.mbal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sourcey.mbal.service.HttpRequestGetAsyncTaskJson;
import com.sourcey.mbal.service.PropertiesReader;
import com.transitionseverywhere.ChangeBounds;
import com.transitionseverywhere.ChangeText;
import com.transitionseverywhere.PatternPathMotion;
import com.transitionseverywhere.Recolor;
import com.transitionseverywhere.TransitionManager;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import butterknife.Bind;
import butterknife.ButterKnife;

public class NewEventActivity extends AppCompatActivity {
    private static final String TAG = "NewEventActivity";

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    private String token;
    private String username;

    @Bind(R.id.textNewCourriel) TextView textNewCourriel;
    @Bind(R.id.textIconBack) TextView textIconBack;
    @Bind(R.id.receivedButton) Button receivedButton;
    @Bind(R.id.imageCheck) ImageView imageCheck;
    @Bind(R.id.progressBar) ProgressBar progressBar;
    @Bind(R.id.transitions_container_button) ViewGroup transitions_container_button;
    @Bind(R.id.transitions_container_text) ViewGroup transitions_container_text;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newevent);

        sharedPref= getSharedPreferences("mbal", Context.MODE_PRIVATE);
        editor=sharedPref.edit();

        this.token = sharedPref.getString("token", "");
        this.username = sharedPref.getString("username", "");

        ButterKnife.bind(this);

        this.imageCheck.setVisibility(View.INVISIBLE);
        this.progressBar.setVisibility(View.INVISIBLE);
        this.progressBar.setIndeterminate(true);

        Long date = getIntent().getLongExtra("date", 0);
        SimpleDateFormat formatterHour = new SimpleDateFormat("HH:mm", Locale.FRANCE);
        SimpleDateFormat formatterDate = new SimpleDateFormat("dd MMMM yyyy", Locale.FRANCE);
        String dateString = "Du nouveau courriel a été reçu à " + formatterHour.format(new Date(date))
                + " le " + formatterDate.format(new Date(date)) + " !";
        textNewCourriel.setText(dateString);

        receivedButton.setOnClickListener(v -> {
            this.receivedButtonTriggered();
        });

    }

    @SuppressLint("SetTextI18n")
    private void receivedButtonTriggered(){

        this.progressBar.setVisibility(View.VISIBLE);

        List<String> requestParameters = new ArrayList<>();
        requestParameters.add("Authorization");
        requestParameters.add("Bearer "+this.token);

        HttpRequestGetAsyncTaskJson httpRequestAsyncTask = new HttpRequestGetAsyncTaskJson(requestParameters);
        httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                "api/event/add/COURRIEL_FETCHED?username="+this.username);

        try {
            this.progressBar.setVisibility(View.GONE);
            if(httpRequestAsyncTask.get() != null && httpRequestAsyncTask.get().getString("response").equals("OK")){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    TransitionManager.beginDelayedTransition(this.transitions_container_button, new ChangeBounds().setPathMotion(new PatternPathMotion()).setDuration(1000));
                    TransitionManager.beginDelayedTransition(this.transitions_container_text, new ChangeText().setChangeBehavior(ChangeText.CHANGE_BEHAVIOR_IN));
                }

                this.receivedButton.setVisibility(View.GONE);
                this.textNewCourriel.setText("Notification envoyée !\n");

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) imageCheck.getLayoutParams();
                params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;

                this.imageCheck.setVisibility(View.VISIBLE);
                this.imageCheck.setLayoutParams(params);

                this.imageCheck.setOnClickListener(v -> finish());

                this.textIconBack.setText("Cliquez sur l'icone ci-dessous pour revenir sur la page principale.\n");
            }else{
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Nous ne parvenons pas à joindre le serveur ! Réessayez plus tard !")
                        .setPositiveButton("Retour en arrière", (dialog, which) -> finish())
                        .setIcon(R.drawable.logo);
                builder.create();
                builder.show();
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
