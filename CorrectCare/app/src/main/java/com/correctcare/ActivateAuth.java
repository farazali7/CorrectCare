package com.correctcare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bioconnect.id.sdk.BCIDSdk;
import com.bioconnect.id.sdk.SdkConstants;
import com.bioconnect.id.sdk.api.models.ServerInfo;
import com.bioconnect.id.sdk.interfaces.ActivationResponse;
import com.bioconnect.id.sdk.interfaces.ActivationStringParseCompletion;
import com.bioconnect.id.sdk.interfaces.SyncModalityAvailabilitiesResponse;
import com.bioconnect.id.sdk.utils.AuthenticatorStatus;
import com.bioconnect.id.sdk.utils.BCSDKError;

import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ActivateAuth extends AppCompatActivity {
    private static final String SECRET_TOKEN = "AnySecretStringCreatedAndProtectedBySdkUserApp";
    private String mUrl;
    private String mAccessKey;
    private String mActivationKey;
    public String mActivationString;


    final String consoleLogin = "https://auth.bioconnectid.com/bioid/api/v2/console/login";
    final String createAuthnUrl = "https://auth.bioconnectid.com/bioid/api/v2/users/1Q59WWTCEGD5TT5MSX584JKWEG/authenticators/create";
    final String activationStringURL = "https://auth.bioconnectid.com/bioid/api/v2/users/1Q59WWTCEGD5TT5MSX584JKWEG/authenticators/";


    String authenticatorUUID;

    public String bcAccessKey;
    public String bcEntityKey;
    public String bcToken;

    RequestQueue queue;

    public boolean activated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            activated = savedInstanceState.getBoolean("activation_state");
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        activated = sp.getBoolean("activation_state", false);
        authenticatorUUID = sp.getString("auth_uuid", "");
        bcAccessKey = sp.getString("bcaccesskey", "");
        bcEntityKey = sp.getString("bcentitykey", "");
        bcToken = sp.getString("bctoken", "");
        setContentView(R.layout.activity_activate_auth);

        getSupportActionBar().setTitle("Biometrics");

        //activated = false;

        //Set up a queue for https requests to backend
        queue = Volley.newRequestQueue(this);

        LocalBroadcastManager.getInstance(this).registerReceiver(mEnrollCountMessageReceiver,
                new IntentFilter(SdkConstants.BCID_ENROLL_COUNT_EVENT));

        LocalBroadcastManager.getInstance(this).registerReceiver(mEnrollSetupBackPressEvent,
                new IntentFilter(SdkConstants.BCID_ENROLL_SETUP_BACK_EVENT));

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
        editor.putBoolean("activation_state", activated);
        editor.putString("auth_uuid", authenticatorUUID);
        editor.putString("bcentitykey", bcEntityKey);
        editor.putString("bcaccesskey", bcAccessKey);
        editor.putString("bctoken", bcToken);
        editor.putString("activation_key",mActivationKey);
        editor.commit();

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("activation_state", activated);
        super.onSaveInstanceState(savedInstanceState);
    }

    public StringRequest console_login() {
        JSONParser parser = new JSONParser();
        StringRequest console_login = new StringRequest(Request.Method.POST, consoleLogin,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        try {
                            org.jose4j.json.internal.json_simple.JSONObject midResponse = (org.jose4j.json.internal.json_simple.JSONObject) parser.parse(response);
                            midResponse.toJSONString();
                            JSONObject responseJ = new JSONObject(midResponse);
                            bcAccessKey = responseJ.getString("bcaccesskey");
                            bcEntityKey = responseJ.getString("bcentitykey");
                            bcToken = responseJ.getString("bctoken");
                        } catch (ParseException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.d("Response", response);

                        queue.add(create_authenticator());
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
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("username", "admin");
                params.put("password", "password");

                return params;
            }
        };

        return console_login;
    }

    public StringRequest create_authenticator() {
        JSONParser parser = new JSONParser();
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("name", "Android Phone");
            jsonBody.put("description", "Correct Care app Phone");
            jsonBody.put("authenticator_type", "mobile");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String requestBody = jsonBody.toString();

        StringRequest create_authenticator = new StringRequest(Request.Method.POST, createAuthnUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        try {
                            org.jose4j.json.internal.json_simple.JSONObject midResponse = (org.jose4j.json.internal.json_simple.JSONObject) parser.parse(response);
                            midResponse.toJSONString();
                            JSONObject responseJ = new JSONObject(midResponse);
                            authenticatorUUID = responseJ.getString("uuid");
                        } catch (ParseException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.d("Response", response);

                        queue.add(get_activate_string());
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

        return create_authenticator;
    }

    public StringRequest delete_authenticator() {
        String deleteAuthnUrl = activationStringURL + authenticatorUUID + "/delete";
        StringRequest delete_authenticator = new StringRequest(Request.Method.POST, deleteAuthnUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
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
        };

        return delete_authenticator;
    }


    public StringRequest get_activate_string() {
        JSONParser parser = new JSONParser();
        String url = activationStringURL + authenticatorUUID + "/activation_string.txt";
        StringRequest get_activate_string = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        try {
                            org.jose4j.json.internal.json_simple.JSONObject midResponse = (org.jose4j.json.internal.json_simple.JSONObject) parser.parse(response);
                            midResponse.toJSONString();
                            JSONObject responseJ = new JSONObject(midResponse);
                            mActivationString = responseJ.getString("activation_string");
                        } catch (ParseException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.d("Response", response);
                        Log.d("String is",mActivationString);

                        decodeActivationString();
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
        };

        return get_activate_string;
    }

    public void activateAuthenticatorPress(View view) {

        if (!activated) {
            queue.add(console_login());
        } else {
            showMessage("This authenticator is already activated.");
        }
    }

    public void openQRScannerPage(View view) {
        if (!activated) {
            Intent intent = new Intent(this, QRCodeScan.class);
            startActivity(intent);
        } else {
            showMessage("This authenticator is already activated.");
        }
    }

    public void decodeActivationString() {
        String activationString = mActivationString;
        Log.d("CUSTOM MESSAGE: ", activationString);
        if (activationString.isEmpty()) {
            showMessage(R.string.activation_string_empty);
            return;
        }
        // Sdk function to decode the activation string.
        BCSDKError error = BCIDSdk.getInstance().decodeActivationString(activationString, new ActivationStringParseCompletion() {
            @Override
            public void onCompletion(ServerInfo serverInfo) {
                if (serverInfo == null) {
                    showMessage(R.string.cannot_decode_activation_string);
                    return;
                }
                mAccessKey = serverInfo.getAccessKey().trim();
                mActivationKey = serverInfo.getActivationKey().trim();
                mUrl = serverInfo.getUrl();

                if (mAccessKey == null || mActivationKey == null || mUrl == null ||
                        mAccessKey.isEmpty() || mActivationKey.isEmpty() || mUrl.isEmpty()) {
                    showMessage(R.string.cannot_decode_activation_string);
                }

                activateAuthenticator();
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

    private void activateAuthenticator() {
        // Sdk function to activate an authenticator.
        BCIDSdk.getInstance()
                .activateAuthenticator(mActivationString, SECRET_TOKEN, new ActivationResponse() {
                    @Override
                    public void onSuccess() {
                        showMessage(R.string.authenticator_is_activated);
                        activated = true;
                        syncActiveModalities();
                    }

                    @Override
                    public void onError(BCSDKError error) {
                        showErrorMessage(error);
                    }
                });
    }

    public void syncActiveModalities() {
        // Sdk function to sync modality availability status.
        BCIDSdk.getInstance().syncModalityAvailabilities(new SyncModalityAvailabilitiesResponse() {

            @Override
            public void onSuccess() {
                showMessage(R.string.synced_active_modalities);
                startModalityEngines();
            }

            @Override
            public void onError(BCSDKError error) {
                showErrorMessage(error);
            }
        });
    }

    private void startModalityEngines() {
        // Sdk function to start modality/biometrics engines.
        BCSDKError error = BCIDSdk.getInstance().startModalityEngines();
        switch (error) {
            case NO_ERROR:
                showMessage(R.string.modality_engines_started);
                showEnrollmentSetupActivity();
                break;
            default:
                showErrorMessage(error);
                break;
        }
    }

    public void showEnrollmentSetupActivity() {
        // Sdk function to show enrollment setup activity
        BCSDKError error = BCIDSdk.getInstance().showSetupEnrollmentActivity();
        showErrorMessage(error);
    }

    public void deleteAllData(View view) {
        //startModalityEngines();
        // Sdk function to delete all data.
        if (!activated){
            showMessage("This authenticator is not activated yet.");
        } else {
            BCSDKError error = BCIDSdk.getInstance().deleteAllData();
            switch(error) {
                case NO_ERROR:
                    activated = false;
                    queue.add(delete_authenticator());
                    showMessage(R.string.userdata_deleted);
                    break;
                case SDK_NOT_INITIALIZED:
                    showMessage(R.string.cannot_delete_userdata_sdk_initialization);
                    break;
                case MODALITY_NOT_STARTED:
                    showMessage(R.string.cannot_delete_userdata_modality_engine);
                default:
                    break;
            }
        }
    }

    private BroadcastReceiver mEnrollCountMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Integer count = intent.getIntExtra(SdkConstants.ENROLL_COUNT_KEY, -1);
            Toast.makeText(ActivateAuth.this, "Number of enrolled modality: " + count, Toast.LENGTH_SHORT).show();
        }
    };

    private BroadcastReceiver mEnrollSetupBackPressEvent = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    };

    public void openModManagePage(View view) {
        Log.d("status", Boolean.toString(activated));
        if (!activated) {
            showMessage("This authenticator is not activated yet.");
        } else {
            Intent intent = new Intent(this, ModalityManagement.class);
            startActivity(intent);
        }
    }
}
