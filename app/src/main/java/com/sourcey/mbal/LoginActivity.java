package com.sourcey.mbal;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.sourcey.mbal.service.HttpRequestAsyncTaskJson;
import com.sourcey.mbal.service.HttpRequestGetAsyncTask;
import com.sourcey.mbal.service.HttpRequestGetAsyncTaskJsonArray;
import com.sourcey.mbal.service.PropertiesReader;
import com.sourcey.mbal.service.firebase.MyFirebaseInstanceIDService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import butterknife.ButterKnife;
import butterknife.Bind;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;

    @Bind(R.id.input_email)
    EditText _emailText;
    @Bind(R.id.input_password)
    EditText _passwordText;
    @Bind(R.id.btn_login)
    Button _loginButton;
    @Bind(R.id.link_signup)
    TextView _signupLink;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    private String response;
    private String responseCode;

    private String token, username;
    private JSONObject jsonUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        _loginButton.setOnClickListener(v -> login());

        _signupLink.setOnClickListener(v -> {
            // Start the Signup activity
            Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
            startActivityForResult(intent, REQUEST_SIGNUP);
            finish();
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        });

        sharedPref = getSharedPreferences("mbal", Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        //TODO: Mot de passe oublié

        this.token = sharedPref.getString("token", "");
        this.username = sharedPref.getString("username", "");


        if (!this.username.equals("")) {
            List<String> requestParameters = new ArrayList<>();
            requestParameters.add("Authorization");
            requestParameters.add("Bearer " + this.token);
            HttpRequestAsyncTaskJson httpRequestAsyncTask = new HttpRequestAsyncTaskJson(requestParameters);
            httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                    "api/user/login?username=" + this.username + "&password=" + sharedPref.getString("password", ""));

            try {
                response = (String) httpRequestAsyncTask.get().get("response");

                if (response.contains("-")) {
                    editor.putString("sessionId", response);
                    editor.commit();
                    onLoginSuccess();
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

    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Connexion...");
        progressDialog.show();

        final String email = _emailText.getText().toString();
        final String password = _passwordText.getText().toString();

        List<String> requestParameters = new ArrayList<>();
        requestParameters.add("Authorization");
        requestParameters.add("Bearer " + sharedPref.getString("token", ""));
        HttpRequestAsyncTaskJson httpRequestAsyncTask = new HttpRequestAsyncTaskJson(requestParameters);
        httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                "api/user/login?username=" + email + "&password=" + password + "&client=ANDROID");

        try {
            response = (String) httpRequestAsyncTask.get().get("response");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new android.os.Handler().postDelayed(
                () -> {
                    if (response.contains("-")) {
                        editor.putString("username", email);
                        editor.putString("password", password);
                        editor.putString("sessionId", response);
                        editor.commit();
                        onLoginSuccess();
                    } else if (response.equals("Vous n'avez pas activé votre compte !")) {
                        Toast.makeText(LoginActivity.this, "Vous devez tout d'abord activer votre compte !", Toast.LENGTH_LONG).show();
                        showDialogActivation();
                    } else {
                        onLoginFailed();
                    }
                    progressDialog.dismiss();
                }, 3000);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        List<String> requestParameters = new ArrayList<>();
        requestParameters.add("Authorization");
        requestParameters.add("Bearer " + this.token);

        HttpRequestAsyncTaskJson httpRequestAsyncTaskUser = new HttpRequestAsyncTaskJson(requestParameters);
        httpRequestAsyncTaskUser.execute(PropertiesReader.properties.getProperty("url") +
                "api/user/getUserByName?username=" + this.username);

        try {
            this.jsonUser = httpRequestAsyncTaskUser.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        try {
            if (this.jsonUser != null) {
                if(this.jsonUser.getJSONObject("family") == null) {
                    this.noFamily();
                }else{
                    String refreshedToken = FirebaseInstanceId.getInstance().getToken();
                    MyFirebaseInstanceIDService.sendRegistrationToServer(refreshedToken, sharedPref);

                    _loginButton.setEnabled(true);
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Erreur de connexion : " + response, Toast.LENGTH_LONG).show();

        _loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    private void showDialogActivation() {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.activation_signup, null);

        final EditText mPinFirstDigitEditText = (EditText) alertLayout.findViewById(R.id.pin_first_edittext);
        final EditText mPinSecondDigitEditText = (EditText) alertLayout.findViewById(R.id.pin_second_edittext);
        final EditText mPinThirdDigitEditText = (EditText) alertLayout.findViewById(R.id.pin_third_edittext);
        final EditText mPinForthDigitEditText = (EditText) alertLayout.findViewById(R.id.pin_forth_edittext);
        final EditText mPinHiddenEditText = (EditText) alertLayout.findViewById(R.id.pin_hidden_edittext);

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setView(alertLayout);
        alert.setCancelable(false);
        alert.setNegativeButton("Annuler", (dialog, which) -> Toast.makeText(getBaseContext(), "Activation annulée", Toast.LENGTH_SHORT).show());

        alert.setPositiveButton("Activer", (dialog, which) -> {
            String code = mPinFirstDigitEditText.getText().toString() + mPinSecondDigitEditText.getText().toString() +
                    mPinThirdDigitEditText.getText().toString() + mPinForthDigitEditText.getText().toString();

            final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                    R.style.AppTheme_Dark_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Activating...");
            progressDialog.show();

            List<String> requestParameters = new ArrayList<>();
            requestParameters.add("Authorization");
            requestParameters.add("Bearer " + sharedPref.getString("token", ""));
            HttpRequestGetAsyncTask httpRequestAsyncTask = new HttpRequestGetAsyncTask(requestParameters);
            httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                    "api/user/activateByPhone/" + code);

            try {
                responseCode = httpRequestAsyncTask.get();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            new android.os.Handler().postDelayed(
                    () -> {

                        if (responseCode.equals("OK")) {
                            login();
                        } else {
                            Toast.makeText(LoginActivity.this, "Le code renseigné n'est pas correct !", Toast.LENGTH_SHORT).show();
                        }
                        progressDialog.dismiss();
                    }, 3000);
        });
        AlertDialog dialog = alert.create();

        final InputMethodManager imm = (InputMethodManager) getSystemService(Service.INPUT_METHOD_SERVICE);
        dialog.setOnShowListener(dialog1 -> {
            //first chosen
            mPinFirstDigitEditText.setFocusable(true);
            mPinFirstDigitEditText.setFocusableInTouchMode(true);
            mPinFirstDigitEditText.requestFocus();
            imm.showSoftInput(mPinFirstDigitEditText, 0);

            mPinFirstDigitEditText.setOnKeyListener((v, keyCode, event) -> {
                char pressedKey = (char) event.getUnicodeChar();
                mPinFirstDigitEditText.setText(pressedKey + "");
                mPinSecondDigitEditText.setFocusable(true);
                mPinSecondDigitEditText.setFocusableInTouchMode(true);
                mPinSecondDigitEditText.requestFocus();
                imm.showSoftInput(mPinSecondDigitEditText, 0);
                return true;
            });

            mPinSecondDigitEditText.setOnKeyListener((v, keyCode, event) -> {
                char pressedKey = (char) event.getUnicodeChar();
                mPinSecondDigitEditText.setText(pressedKey + "");
                mPinThirdDigitEditText.setFocusable(true);
                mPinThirdDigitEditText.setFocusableInTouchMode(true);
                mPinThirdDigitEditText.requestFocus();
                imm.showSoftInput(mPinThirdDigitEditText, 0);
                return true;
            });

            mPinThirdDigitEditText.setOnKeyListener((v, keyCode, event) -> {
                char pressedKey = (char) event.getUnicodeChar();
                mPinThirdDigitEditText.setText(pressedKey + "");
                mPinForthDigitEditText.setFocusable(true);
                mPinForthDigitEditText.setFocusableInTouchMode(true);
                mPinForthDigitEditText.requestFocus();
                imm.showSoftInput(mPinForthDigitEditText, 0);
                return true;
            });

            mPinForthDigitEditText.setOnKeyListener((v, keyCode, event) -> {
                char pressedKey = (char) event.getUnicodeChar();
                mPinForthDigitEditText.setText(pressedKey + "");
                InputMethodManager imm1 = (InputMethodManager) getSystemService(Service.INPUT_METHOD_SERVICE);
                imm1.hideSoftInputFromInputMethod(mPinForthDigitEditText.getWindowToken(), 0);
                return true;
            });
        });
        dialog.show();
    }

    private void noFamily(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Vous devez faire partie d'une famille pour utiliser les services de MBAL")
                .setTitle("Famille")
                .setPositiveButton("Créer", (dialog, which) -> createFamily())
                .setNegativeButton("Rejoindre", (dialog, which) -> joinFamily());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createFamily(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Créer sa famille");
        alertDialog.setMessage("Entrez les informations relatives à votre nouvelle famille.");

        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setPositiveButton("Créer",
                (dialog, which) -> {
                    String password = input.getText().toString();
                    if(password.compareTo("") == 0) {
                        List<String> requestParametersFamily = new ArrayList<>();
                        requestParametersFamily.add("Authorization");
                        requestParametersFamily.add("Bearer " + sharedPref.getString("token", ""));

                        HttpRequestGetAsyncTaskJsonArray httpRequestAsyncTaskUser = new HttpRequestGetAsyncTaskJsonArray(requestParametersFamily);
                        httpRequestAsyncTaskUser.execute(PropertiesReader.properties.getProperty("url") +
                                "api/family/getAllFamilies");

                        try {
                            JSONArray jsonArray = httpRequestAsyncTaskUser.get();

                            for(int i = 0; i < jsonArray.length(); i++){
                                if(jsonArray.getJSONObject(i).getString("name").equals(/*name family*/)){

                                }
                            }

                        }catch(Exception e){
                            e.printStackTrace();
                        }


                    }
                });
    }

    private void createFamily() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SignupActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.dialog_signup, null);

        final TextView family = (TextView) alertLayout.findViewById(R.id.family);
        final EditText password = (EditText) alertLayout.findViewById(R.id.password);
        final EditText retaperPassword = (EditText) alertLayout.findViewById(R.id.retaperPassword);

        family.setText(autoCompleteTextView.getText().toString());

        builder.setView(alertLayout)
                .setPositiveButton("OK", (DialogInterface dialog, int id) -> {

                    if (retaperPassword.getText().toString().equals(password.getText().toString())) {
                        List<String> requestParameters = new ArrayList<>();
                        requestParameters.add("Authorization");
                        requestParameters.add("Bearer " + sharedPref.getString("token", ""));
                        HttpRequestAsyncTaskJson httpRequestAsyncTask = new HttpRequestAsyncTaskJson(requestParameters);
                        httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                                "api/family/createFamily?familyname=" + autoCompleteTextView.getText().toString() + "&password=" + password.getText().toString() + "&session_id=" + sharedPref.getString("sessionId", ""));

                        try {
                            response = (String) httpRequestAsyncTask.get().get("response");

                            if (response.equals("OK")) {
                                startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, response, Toast.LENGTH_LONG).show();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(this, "Les mots de passe ne sont pas les mêmes ! Impossible de créer cette famille", Toast.LENGTH_LONG).show();
                    }
                });
        builder.create().show();
    }

    private void joinFamily() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SignupActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.dialog_signup, null);

        final TextView family = (TextView) alertLayout.findViewById(R.id.family);
        final EditText password = (EditText) alertLayout.findViewById(R.id.password);
        final EditText retaperPassword = (EditText) alertLayout.findViewById(R.id.retaperPassword);

        family.setText(autoCompleteTextView.getText().toString());
        retaperPassword.setVisibility(View.INVISIBLE);

        builder.setView(alertLayout)
                .setPositiveButton("OK", (dialog, id) -> {

                    List<String> requestParameters = new ArrayList<>();
                    requestParameters.add("Authorization");
                    requestParameters.add("Bearer " + sharedPref.getString("token", ""));
                    HttpRequestAsyncTaskJson httpRequestAsyncTask = new HttpRequestAsyncTaskJson(requestParameters);
                    httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                            "api/user/setFamilyForUser?username=" + _emailText.getText().toString() + "&name_family=" + autoCompleteTextView.getText().toString() + "&password_family=" + password.getText().toString());

                    try {
                        response = (String) httpRequestAsyncTask.get().get("response");

                        if (response.equals("OK")) {
                            startActivity(new Intent(SignupActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Le mot de passe saisi n'est pas le bon ! Réessayez ou revenez en arrière pour créer une famille.", Toast.LENGTH_LONG).show();
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                });
        builder.create().show();

    }
    //TODO : MBAL create user and setfamily
}
