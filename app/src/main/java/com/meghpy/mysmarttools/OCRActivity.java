package com.meghpy.mysmarttools;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;


public class OCRActivity extends AppCompatActivity {

    MaterialButton btnImageToText, btnPdfToText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocractivity);

        btnImageToText = findViewById(R.id.btnImageToText);
        btnPdfToText = findViewById(R.id.btnPdfToText);

        btnImageToText.setOnClickListener(v -> {
           startActivity(new Intent(OCRActivity.this, ImageToText.class));
        });
        
        btnPdfToText.setOnClickListener(v -> {
           startActivity(new Intent(OCRActivity.this, PDFToText.class));
        });



    }
}
