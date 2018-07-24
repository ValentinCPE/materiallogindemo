package com.sourcey.mbal;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.sourcey.mbal.service.HttpRequestAsyncTaskJson;
import com.sourcey.mbal.service.HttpRequestGetAsyncTask;
import com.sourcey.mbal.service.PropertiesReader;
import com.sourcey.mbal.service.firebase.MyFirebaseInstanceIDService;

import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import butterknife.ButterKnife;
import butterknife.Bind;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;

    @Bind(R.id.input_email) EditText _emailText;
    @Bind(R.id.input_password) EditText _passwordText;
    @Bind(R.id.btn_login) Button _loginButton;
    @Bind(R.id.link_signup) TextView _signupLink;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    private String response;
    private String responseCode;

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

        sharedPref=getSharedPreferences("mbal", Context.MODE_PRIVATE);
        editor=sharedPref.edit();

        if(!sharedPref.getString("username","").equals("")){
            List<String> requestParameters = new ArrayList<>();
            requestParameters.add("Authorization");
            requestParameters.add("Bearer "+sharedPref.getString("token",""));
            HttpRequestAsyncTaskJson httpRequestAsyncTask = new HttpRequestAsyncTaskJson(requestParameters);
            httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                    "api/user/login?username="+sharedPref.getString("username","")+"&password="+sharedPref.getString("password",""));

            try {
                response = (String) httpRequestAsyncTask.get().get("response");

                if(response.contains("-")){
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
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        final String email = _emailText.getText().toString();
        final String password = _passwordText.getText().toString();

        List<String> requestParameters = new ArrayList<>();
        requestParameters.add("Authorization");
        requestParameters.add("Bearer "+sharedPref.getString("token",""));
        HttpRequestAsyncTaskJson httpRequestAsyncTask = new HttpRequestAsyncTaskJson(requestParameters);
        httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                "api/user/login?username="+email+"&password="+password+"&client=ANDROID");

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
                    if(response.contains("-")){
                        editor.putString("username",email);
                        editor.putString("password",password);
                        editor.putString("sessionId", response);
                        editor.commit();
                        onLoginSuccess();
                    }else if(response.equals("Vous n'avez pas activé votre compte !")){
                        Toast.makeText(LoginActivity.this, "Vous devez tout d'abord activer votre compte !",Toast.LENGTH_LONG).show();
                        showDialogActivation();
                    }else{
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
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        MyFirebaseInstanceIDService.sendRegistrationToServer(refreshedToken, sharedPref);

        _loginButton.setEnabled(true);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Erreur de connexion : "+response, Toast.LENGTH_LONG).show();

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

    private void showDialogActivation(){
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
            String code = mPinFirstDigitEditText.getText().toString()+mPinSecondDigitEditText.getText().toString()+
                    mPinThirdDigitEditText.getText().toString()+mPinForthDigitEditText.getText().toString();

            final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                    R.style.AppTheme_Dark_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Activating...");
            progressDialog.show();

            List<String> requestParameters = new ArrayList<>();
            requestParameters.add("Authorization");
            requestParameters.add("Bearer "+sharedPref.getString("token",""));
            HttpRequestGetAsyncTask httpRequestAsyncTask = new HttpRequestGetAsyncTask(requestParameters);
            httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                    "api/user/activateByPhone/"+code);

            try {
                responseCode = httpRequestAsyncTask.get();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            new android.os.Handler().postDelayed(
                    () -> {

                        if(responseCode.equals("OK")){
                            login();
                        }else {
                            Toast.makeText(LoginActivity.this,"Le code renseigné n'est pas correct !",Toast.LENGTH_SHORT).show();
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
            imm.showSoftInput(mPinFirstDigitEditText,0);

            mPinFirstDigitEditText.setOnKeyListener((v, keyCode, event) -> {
                char pressedKey = (char) event.getUnicodeChar();
                mPinFirstDigitEditText.setText(pressedKey + "");
                mPinSecondDigitEditText.setFocusable(true);
                mPinSecondDigitEditText.setFocusableInTouchMode(true);
                mPinSecondDigitEditText.requestFocus();
                imm.showSoftInput(mPinSecondDigitEditText,0);
                return true;
            });

            mPinSecondDigitEditText.setOnKeyListener((v, keyCode, event) -> {
                char pressedKey = (char) event.getUnicodeChar();
                mPinSecondDigitEditText.setText(pressedKey + "");
                mPinThirdDigitEditText.setFocusable(true);
                mPinThirdDigitEditText.setFocusableInTouchMode(true);
                mPinThirdDigitEditText.requestFocus();
                imm.showSoftInput(mPinThirdDigitEditText,0);
                return true;
            });

            mPinThirdDigitEditText.setOnKeyListener((v, keyCode, event) -> {
                char pressedKey = (char) event.getUnicodeChar();
                mPinThirdDigitEditText.setText(pressedKey + "");
                mPinForthDigitEditText.setFocusable(true);
                mPinForthDigitEditText.setFocusableInTouchMode(true);
                mPinForthDigitEditText.requestFocus();
                imm.showSoftInput(mPinForthDigitEditText,0);
                return true;
            });

            mPinForthDigitEditText.setOnKeyListener((v, keyCode, event) -> {
                char pressedKey = (char) event.getUnicodeChar();
                mPinForthDigitEditText.setText(pressedKey + "");
                InputMethodManager imm1 = (InputMethodManager) getSystemService(Service.INPUT_METHOD_SERVICE);
                imm1.hideSoftInputFromInputMethod(mPinForthDigitEditText.getWindowToken(),0);
                return true;
            });
        });
        dialog.show();
    }
}
