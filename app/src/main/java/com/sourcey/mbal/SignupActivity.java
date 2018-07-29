package com.sourcey.mbal;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sourcey.mbal.service.HttpRequestAsyncTask;
import com.sourcey.mbal.service.HttpRequestAsyncTaskJson;
import com.sourcey.mbal.service.HttpRequestGetAsyncTask;
import com.sourcey.mbal.service.HttpRequestGetAsyncTaskJson;
import com.sourcey.mbal.service.HttpRequestGetAsyncTaskJsonArray;
import com.sourcey.mbal.service.PropertiesReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import butterknife.ButterKnife;
import butterknife.Bind;

public class SignupActivity extends FragmentActivity {
    private static final String TAG = "SignupActivity";

    @Bind(R.id.input_name)
    EditText _nameText;
    @Bind(R.id.input_prenom)
    EditText _prenomText;
    @Bind(R.id.input_email)
    EditText _emailText;
    @Bind(R.id.input_mobile)
    EditText _mobileText;
    @Bind(R.id.input_password)
    EditText _passwordText;
    @Bind(R.id.input_reEnterPassword)
    EditText _reEnterPasswordText;
    @Bind(R.id.btn_signup)
    Button _signupButton;
    @Bind(R.id.link_login)
    TextView _loginLink;
    @Bind(R.id.autoCompleteTextView)
    AutoCompleteTextView autoCompleteTextView;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    private String response;
    private String responseCode;

    private List<String> namesFamily;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);

        sharedPref = getSharedPreferences("mbal", Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        _signupButton.setOnClickListener(v -> signup());

        _loginLink.setOnClickListener(v -> {
            // Finish the registration screen and return to the Login activity
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        });

        List<String> requestParametersFamily = new ArrayList<>();
        requestParametersFamily.add("Authorization");
        requestParametersFamily.add("Bearer " + sharedPref.getString("token", ""));

        HttpRequestGetAsyncTaskJsonArray httpRequestAsyncTaskUser = new HttpRequestGetAsyncTaskJsonArray(requestParametersFamily);
        httpRequestAsyncTaskUser.execute(PropertiesReader.properties.getProperty("url") +
                "api/family/getAllFamilies");

        try {
            JSONArray jsonArray = httpRequestAsyncTaskUser.get();

            this.namesFamily = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                this.namesFamily.add(jsonArray.getJSONObject(i).getString("name"));
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>
                    (this, android.R.layout.select_dialog_item, this.namesFamily);
            autoCompleteTextView.setThreshold(1);
            autoCompleteTextView.setAdapter(adapter);
            autoCompleteTextView.setTextColor(Color.WHITE);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed();
            return;
        }

        _signupButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(SignupActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        String name = _nameText.getText().toString();
        String prenom = _prenomText.getText().toString();
        String email = _emailText.getText().toString();
        String mobile = _mobileText.getText().toString();
        String password = _passwordText.getText().toString();
        String reEnterPassword = _reEnterPasswordText.getText().toString();

        if (name.endsWith(" ")) {
            name = name.substring(0, name.length() - 1);
        }

        if (prenom.endsWith(" ")) {
            prenom = prenom.substring(0, prenom.length() - 1);
        }

        if (email.endsWith(" ")) {
            email = email.substring(0, email.length() - 1);
        }

        if (mobile.endsWith(" ")) {
            mobile = mobile.substring(0, mobile.length() - 1);
        }


        List<String> requestParameters = new ArrayList<>();
        requestParameters.add("Authorization");
        requestParameters.add("Bearer " + sharedPref.getString("token", ""));
        HttpRequestAsyncTask httpRequestAsyncTask = new HttpRequestAsyncTask(requestParameters, "POST");
        httpRequestAsyncTask.execute(PropertiesReader.properties.getProperty("url") +
                "api/user/createByMobileApp?name=" + name + "&prenom=" + prenom + "&mail=" + email + "&password=" + password + "&" +
                "num_tel=" + mobile + "&role=USER");

        try {
            response = httpRequestAsyncTask.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        new android.os.Handler().postDelayed(
                () -> {
                    // On complete call either onSignupSuccess or onSignupFailed
                    // depending on success
                    if (response.equals("OK")) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            onSignupSuccess();
                        }
                    } else {
                        onSignupFailed();
                    }
                    progressDialog.dismiss();
                }, 3000);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onSignupSuccess() {
        _signupButton.setEnabled(true);
        setResult(RESULT_OK, null);

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

            final ProgressDialog progressDialog = new ProgressDialog(SignupActivity.this,
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

                            HttpRequestAsyncTaskJson httpRequestAsyncTaskJson = new HttpRequestAsyncTaskJson(requestParameters);
                            httpRequestAsyncTaskJson.execute(PropertiesReader.properties.getProperty("url") +
                                    "api/user/login?username=" + _emailText.getText().toString() + "&password=" + _passwordText.getText().toString());

                            try {
                                response = (String) httpRequestAsyncTaskJson.get().get("response");

                                if (response.contains("-")) {
                                    editor.putString("sessionId", response);
                                    editor.commit();
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            this.errorAfterCode();
                        } else {
                            this.errorAfterCode();
                            //TODO : Delete account + creation again and code
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

    public void onSignupFailed() {
        Toast.makeText(getBaseContext(), "Signup failed : " + response, Toast.LENGTH_LONG).show();

        _signupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String name = _nameText.getText().toString();
        String email = _emailText.getText().toString();
        String mobile = _mobileText.getText().toString();
        String password = _passwordText.getText().toString();
        String reEnterPassword = _reEnterPasswordText.getText().toString();

        if (name.isEmpty() || name.length() < 3) {
            _nameText.setError("at least 3 characters");
            valid = false;
        } else {
            _nameText.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (mobile.isEmpty() || mobile.length() != 10) {
            _mobileText.setError("Enter Valid Mobile Number");
            valid = false;
        } else {
            _mobileText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        if (reEnterPassword.isEmpty() || reEnterPassword.length() < 4 || reEnterPassword.length() > 10 || !(reEnterPassword.equals(password))) {
            _reEnterPasswordText.setError("Password Do not match");
            valid = false;
        } else {
            _reEnterPasswordText.setError(null);
        }

        return valid;
    }

    private void errorAfterCode() {
        Toast.makeText(SignupActivity.this, "Erreur durant la vérification !", Toast.LENGTH_LONG).show();

        List<String> requestParameters = new ArrayList<>();
        requestParameters.add("Authorization");
        requestParameters.add("Bearer " + sharedPref.getString("token", ""));
        HttpRequestAsyncTaskJson httpRequestAsyncTaskJson = new HttpRequestAsyncTaskJson(requestParameters);
        httpRequestAsyncTaskJson.execute(PropertiesReader.properties.getProperty("url") +
                "api/user/deleteUser?username=" + _emailText.getText().toString());

        try {
            response = (String) httpRequestAsyncTaskJson.get().get("response");

            if(response.equals("OK")){
                signup();
            }else{
                Toast.makeText(this, "Erreur durant le renvoi du code !", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}