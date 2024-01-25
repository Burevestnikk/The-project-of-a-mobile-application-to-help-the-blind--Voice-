package com.example.aplikacja.ui;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aplikacja.databinding.ActivitySelectgroupBinding;
import com.example.aplikacja.repository.User;
import com.google.firebase.database.FirebaseDatabase;

public class SelectgroupActivity extends AppCompatActivity {

    private ActivitySelectgroupBinding views;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        super.onCreate(savedInstanceState);
        views = ActivitySelectgroupBinding.inflate(getLayoutInflater());
        setContentView(views.getRoot());
        init();
    }

    private void init() {
        views.enterBtnBlind.setOnClickListener(v -> {
            startActivity(new Intent(SelectgroupActivity.this, CallActivity.class));
            FirebaseDatabase.getInstance().getReference().child(Build.DEVICE).setValue(new User(false,2, 0, 0, "",""));
            finish();
        });
        views.enterBtnVolunteer.setOnClickListener(v -> {
            startActivity(new Intent(SelectgroupActivity.this, RegistrationActivity.class));
            finish();
        });
        views.enterBackSelect.setOnClickListener(v -> {
            startActivity(new Intent(SelectgroupActivity.this, LoginActivity.class));
            finish();
        });
    }
}
