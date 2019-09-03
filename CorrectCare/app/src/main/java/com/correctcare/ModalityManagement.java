package com.correctcare;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.bioconnect.id.sdk.BCIDSdk;
import com.bioconnect.id.sdk.utils.BCSDKError;

public class ModalityManagement extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modality_management);

        getSupportActionBar().setTitle("Modality Management");
    }

    public void showModalityManagement(View view) {
        // Sdk function call to show modality management screen.
        BCSDKError error = BCIDSdk.getInstance().showModalityManagementActivity();
        showErrorMessage(error);
    }

    public void selectDefaultModality(View view) {
        // Sdk function to show a dialog to select the default biometrics.
        BCSDKError error = BCIDSdk
                .getInstance()
                .showDefaultModalitySelectionDialog(ModalityManagement.this);
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
}

