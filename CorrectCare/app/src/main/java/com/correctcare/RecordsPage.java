package com.correctcare;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class RecordsPage extends AppCompatActivity {

    public String patientName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records_page);
        getSupportActionBar().hide();

        Intent intent = getIntent();
        patientName = intent.getExtras().getString("current_patient");
        TextView patientHeader = (TextView) findViewById(R.id.patientName);
        patientHeader.setText(patientName);
        TextView information = (TextView) findViewById(R.id.recordInfo);
        setInformation(information);
    }

    private void setInformation(TextView text) {

        HashMap<String, String> patientGenders = new HashMap<>();
        patientGenders.put("Faraz Ali", "Male");
        patientGenders.put("Jermaine Jackson", "Male");
        patientGenders.put("Stanley Andersen", "Male");
        patientGenders.put("Ash Leope", "Female");
        patientGenders.put("Naomie Krine", "Female");
        patientGenders.put("Emma Pope", "Female");
        patientGenders.put("Advika Sarath", "Female");
        patientGenders.put("Jeff Collier", "Male");

        ImageView patientImage = (ImageView) findViewById(R.id.patientImage);
        String gender = patientGenders.get(patientName);
        if (gender == "Male") {
            patientImage.setImageResource(R.drawable.man);
        } else {
            patientImage.setImageResource(R.drawable.woman);
        }

        InputStream is = getResources().openRawResource(R.raw.record);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        String entireFile = "";
        try {
            while((line = br.readLine()) != null) {
                if (line.contains("Gender")) {
                    line = "Gender: " + gender + " <br />";
                }
                entireFile += (line + "\n"); // <---------- add each line to entireFile
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        text.setText(Html.fromHtml(entireFile)); // <------- assign entireFile to TextView
    }
}
