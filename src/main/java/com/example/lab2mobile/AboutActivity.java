package com.example.lab2mobile;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ImageView developerPhoto = findViewById(R.id.developerPhoto);
        TextView developerInfo = findViewById(R.id.developerInfo);

        developerPhoto.setImageResource(R.drawable.my_photo);
        developerInfo.setText("Іванов Микита, студент ТТП-41.\n Телеграм: @Kalips2");
    }
}
