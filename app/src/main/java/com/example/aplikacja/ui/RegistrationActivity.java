package com.example.aplikacja.ui;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aplikacja.R;
import com.example.aplikacja.databinding.ActivityRegistrationBinding;
import com.example.aplikacja.repository.MainRepository;
import com.example.aplikacja.repository.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RegistrationActivity extends AppCompatActivity {

    private ActivityRegistrationBinding views;
    private MainRepository mainRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        views = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(views.getRoot());
        init();
    }

    public String inputText1;

    private void init() {
        views.enterBtn2.setOnClickListener(v -> {
            TextInputLayout til1 = findViewById(R.id.til1);
            TextInputLayout til2 = findViewById(R.id.til2);
            inputText1 = ((TextInputEditText) til1.findViewById(R.id.til1Text)).getText().toString();
            String inputText2 = ((TextInputEditText) til2.findViewById(R.id.til2Text)).getText().toString();
            FirebaseDatabase.getInstance().getReference().child("volunteers/" + inputText1).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                    if (dataSnapshot.exists()) {
                        Object passwordFromDatabaseObject = dataSnapshot.child("password").getValue();
                        if (passwordFromDatabaseObject instanceof Long) {
                            long passwordFromDatabase = (Long) passwordFromDatabaseObject;
                            if (String.valueOf(passwordFromDatabase).equals(inputText2)) {
                                FirebaseDatabase.getInstance().getReference().child(Build.DEVICE).setValue(new User(true,1, 0, 0, inputText1,""));
                                startActivity(new Intent(RegistrationActivity.this, CallActivity.class));
                            } else {
                                til1.setError(null);
                                til2.setError(" ");
                                til2.setErrorTextColor(ColorStateList.valueOf(Color.RED));
                            }
                        }
                    } else {
                        til1.setError(" ");
                        til1.setErrorTextColor(ColorStateList.valueOf(Color.RED));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        });
        views.enterBtn.setOnClickListener(v -> {
            startActivity(new Intent(RegistrationActivity.this, SelectgroupActivity.class));
            finish();
        });
    }
}