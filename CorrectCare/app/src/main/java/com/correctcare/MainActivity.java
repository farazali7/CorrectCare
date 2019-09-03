package com.correctcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bioconnect.id.sdk.BCIDSdk;
import com.bioconnect.id.sdk.SdkConstants;
import com.bioconnect.id.sdk.utils.BCSDKError;
import com.webianks.library.scroll_choice.ScrollChoice;

import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    List<String> patients = new ArrayList<>();
    TextView patientName;
    ScrollChoice scrollChoice;
    BufferedReader reader;
    private static String mFaceLicense;
    private static final int STEP_UP_AUTH_REQUEST_CODE = 1002;
    private static final int PATIENT_IDENTIFICATION_CODE = 1007;
    private final String SECRET_TOKEN = "AnySecretStringCreatedAndProtectedBySdkUserApp";
    private final String createVerificationUrl = "https://auth.bioconnectid.com/bioid/api/v2/clientz/verify";
    final String createClientUrl = "https://auth.bioconnectid.com/bioid/api/v2/clients/create";
    private final String activateClientUrl = "https://auth.bioconnectid.com/bioid/api/v2/clientz/activate";

    public String clientActKey;
    public String clientEntityKey;
    public boolean clientCreated;


    public String bcAccessKey;
    public String bcEntityKey;
    public String bcToken;
    public String ActivationKey;

    public boolean activated;

    RequestQueue queue;

    HashMap<String, String> patientGenders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        readPatientsFromFile();
        initViews();

        scrollChoice.addItems(patients,2);


        //Set up a queue for https requests to backend
        queue = Volley.newRequestQueue(this);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        bcAccessKey = sp.getString("bcaccesskey", "");
        bcEntityKey = sp.getString("bcentitykey", "");
        bcToken = sp.getString("bctoken", "");
        activated = sp.getBoolean("activation_state", false);
        ActivationKey = sp.getString("activation_key","");
        clientCreated = sp.getBoolean("client_created", false);
        clientActKey = sp.getString("client_activation_key", "");
        clientEntityKey = sp.getString("client_entity_key", "");

        mFaceLicense = readRawTextFile(R.raw.facephi_licese);
        initializeBCIDSDK();

        //Start modalities engine on a screen resume case
        BCSDKError error = BCIDSdk.getInstance().startModalityEngines();
        switch (error) {
            case NO_ERROR:
                break;
            default:
                showErrorMessage(error);
                break;
        }

    }

    @Override
    public void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("client_created", clientCreated);
        editor.putString("client_activation_key", clientActKey);
        editor.putString("client_entity_key", clientEntityKey);
        editor.commit();

        super.onPause();
    }

    public void openActivateAuthPage(View view) {
        Intent intent = new Intent(this, ActivateAuth.class);
        startActivity(intent);
    }

    public void openSettingsPage(View view) {
        Intent intent = new Intent(this, SettingsPage.class);
        startActivity(intent);
    }


    public void initializeBCIDSDK() {
        Bundle licenses = new Bundle();

        licenses.putString(SdkConstants.FACEPHI_LICENCE_KEY, mFaceLicense);
        BCIDSdk.getInstance().initialize(MainActivity.this, R.xml.network_security_config, licenses);
    }

    public void openBiometricPage(View view) {
        Intent intent = new Intent(this, BiometricPage.class);
        startActivity(intent);
    }

    private void initViews() {
        patientName = (TextView)findViewById(R.id.selectPatient);
        scrollChoice = (ScrollChoice)findViewById(R.id.scroll_choice);

    }

    private void readPatientsFromFile() {
        try {
            InputStream file = getResources().openRawResource(R.raw.patients);
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line!=null) {
                patients.add(line);
                line =  reader.readLine();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private String readRawTextFile(int resId) {
        InputStream inputStream = this.getResources().openRawResource(resId);
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder total = new StringBuilder();
        String line;
        try {
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return total.toString();
    }

    public void getRecord(View view) {
        if (!activated) {
            showMessage("This authenticator has not been setup, cannot verify user.");
        } else if (!clientCreated){
            queue.add(create_client(STEP_UP_AUTH_REQUEST_CODE));
        } else {
            doStepUpAuthentication(STEP_UP_AUTH_REQUEST_CODE);
        }
    }

    public StringRequest create_verif_request(int requestCode) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("transaction_id", "777");
            jsonBody.put("user_uuid", "1Q59WWTCEGD5TT5MSX584JKWEG");
            jsonBody.put("message", "Please identify yourself - Patient identification via CorrectCare");
            jsonBody.put("server_match","false");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String requestBody = jsonBody.toString();

        StringRequest create_verif_request = new StringRequest(Request.Method.POST, createVerificationUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
                        doStepUpAuthentication(requestCode);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", error.getMessage());
                    }
                })
        {
            @Override
            public Map getHeaders() throws AuthFailureError
            {
                HashMap headers = new HashMap();
                headers.put("Content-Type", "application/json");
                headers.put("bcaccesskey", bcAccessKey);
                headers.put("bcentitykey", clientEntityKey);
                headers.put("bcsecrettoken",SECRET_TOKEN);
                return headers;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException e) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }
        };

        return create_verif_request;
    }

    public StringRequest create_client(int requestCode) {
        JSONParser parser = new JSONParser();
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("name", "CorrectCareClient");
            jsonBody.put("description", "Correct Care App Client");
            jsonBody.put("kind", "application");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String requestBody = jsonBody.toString();

        StringRequest create_client = new StringRequest(Request.Method.POST, createClientUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        try {
                            org.jose4j.json.internal.json_simple.JSONObject midResponse = (org.jose4j.json.internal.json_simple.JSONObject) parser.parse(response);
                            midResponse.toJSONString();
                            JSONObject responseJ = new JSONObject(midResponse);
                            clientActKey = responseJ.getString("activation_key");
                            clientEntityKey = responseJ.getString("entity_key");
                        } catch (ParseException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.d("Response", response);

                        queue.add(activate_client(requestCode));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", error.getMessage());
                    }
                })
        {
            @Override
            public Map getHeaders() throws AuthFailureError
            {
                HashMap headers = new HashMap();
                headers.put("Content-Type", "application/json");
                headers.put("bcaccesskey", bcAccessKey);
                headers.put("bcentitykey", bcEntityKey);
                headers.put("bctoken",bcToken);
                return headers;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException e) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }
        };

        return create_client;
    }

    public StringRequest activate_client(int requestCode) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("activation_key", clientActKey);
            jsonBody.put("secret_token", SECRET_TOKEN);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String requestBody = jsonBody.toString();

        StringRequest activate_client = new StringRequest(Request.Method.POST, activateClientUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
                        clientCreated = true;
                        queue.add(create_verif_request(requestCode));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", error.getMessage());
                    }
                })
        {
            @Override
            public Map getHeaders() throws AuthFailureError
            {
                HashMap headers = new HashMap();
                headers.put("Content-Type", "application/json");
                headers.put("bcaccesskey", bcAccessKey);
                return headers;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException e) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }
        };

        return activate_client;
    }


    public void doStepUpAuthentication(int requestCode) {
        // Sdk function to get the list of pending verifications.
        BCSDKError error = BCIDSdk.getInstance().getPendingVerifications((bcsdkError, list) -> {
            if (list != null && !list.isEmpty()) {
                String verificationId = list.get(0).getUuid();
                if (verificationId != null) {
                    // Sdk function to launch an activity to authenticate for an step up request.
                    BCIDSdk.getInstance()
                            .showStepUpAuthenticationActivity(MainActivity.this,
                                    requestCode, verificationId);
                }
            } else {
                queue.add(create_verif_request(requestCode));
            }
        });
        showErrorMessage(error);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == STEP_UP_AUTH_REQUEST_CODE || requestCode == PATIENT_IDENTIFICATION_CODE) {
            checkVerificationResult(requestCode);
        }
    }

    public void identifyPatient(View view) {
        if (!activated) {
            showMessage("This authenticator has not been setup, cannot identify patient.");
        } else if (!clientCreated){
            queue.add(create_client(PATIENT_IDENTIFICATION_CODE));
        } else {
            doStepUpAuthentication(PATIENT_IDENTIFICATION_CODE);
        }
    }

    public void patientVerificationResult() {
        showIdentityMessage("This patient is Faraz Ali.");
    }

    public void checkVerificationResult(int requestCode) {
        BCSDKError error = BCIDSdk.getInstance().getVerificationHistory((bcsdkError, list) -> {
            if (list!= null && !list.isEmpty()) {
                String result = list.get(0).getStatus();
                Log.d("this is result", result);
                if (result.equals("success")) {
                    switch(requestCode) {
                        case STEP_UP_AUTH_REQUEST_CODE:
                            openRecordsPage();
                            break;
                        case PATIENT_IDENTIFICATION_CODE:
                            patientVerificationResult();
                            break;
                        default:
                            break;
                    }
                } else {
                    switch(requestCode) {
                        case STEP_UP_AUTH_REQUEST_CODE:
                            showMessage("You did not verify successfully, patient record cannot be opened.");
                            break;
                        case PATIENT_IDENTIFICATION_CODE:
                            showMessage("Could not identify the patient. Try again.");
                            break;
                        default:
                            showMessage("Unknown verification error.");
                            break;
                    }
                }
            }
        });
        showErrorMessage(error);
    }

    private void showErrorMessage(BCSDKError error) {
        switch (error) {
            case SDK_NOT_INITIALIZED:
                showMessage(R.string.sdk_not_initialized);
                break;
            case NETWORK_FAILURE:
                showMessage(R.string.network_failure);
                break;
            case MODALITY_NOT_STARTED:
                showMessage(R.string.modality_engines_not_started);
                break;
            case AUTHENTICATOR_NOT_ACTIVATED:
                showMessage(R.string.authenticator_not_activated);
                break;
            case AUTHENTICATOR_NOT_ENROLLED:
                showMessage(R.string.authenticator_not_enrolled);
                break;
            case AUTHENTICATOR_ALREADY_ENROLLED:
                showMessage(R.string.authenticator_already_enrolled);
                break;
            case INTERNAL_ERROR:
                showMessage(R.string.internal_error);
                break;
            case INVALID_PARAMETERS:
                showMessage(R.string.invalid_parameters);
            case NO_ERROR:
            default:
                break;
        }
    }

    private void showMessage(int id) {
        String message = getString(id);
        showMessage(message);
    }

    private void showMessage(String message) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        builder.setTitle("CorrectCare Message")
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                }).show();
    }

    private void showIdentityMessage(String message) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        final View view = factory.inflate(R.layout.patientidpic, null);
        builder.setView(view);
        builder.setTitle("CorrectCare Message")
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                });
        builder.show();
    }

    public void openRecordsPage() {
        Intent intent = new Intent(this, RecordsPage.class);
        intent.putExtra("current_patient", scrollChoice.getCurrentSelection());
        startActivity(intent);
    }

}
