package com.example.terminalbluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class VerifyTheLocalStorage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_the_local_storage);

        Button back = findViewById(R.id.goBack);
        Button insert = findViewById(R.id.getLocalStorage);
        final TextView show = findViewById(R.id.showLocalStoarage);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(VerifyTheLocalStorage.this, MainActivity.class));
            }
        });
        String text = "De aici puteti sa va descarcati datele din localStorage, cele nu au putut fi trimise!";
        show.setText(text);

        insert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File internalStorageDir = getFilesDir();
                File test = new File(internalStorageDir, "test.csv");

                try {
                    BufferedReader br = new BufferedReader(new FileReader(test));
                    String st;
                    String textToShow = "";
                    while ((st = br.readLine()) != null) {
                        textToShow = textToShow + " " + st;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }
}
