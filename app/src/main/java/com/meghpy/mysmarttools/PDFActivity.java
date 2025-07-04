package com.meghpy.mysmarttools;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PDFActivity extends AppCompatActivity {

    LinearLayout textToPdf,imageToPdf, excelToPdf, wordToPdf, passwordProtect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pdfactivity);

        textToPdf = findViewById(R.id.textToPdf);
        imageToPdf = findViewById(R.id.imageToPdf);
        excelToPdf = findViewById(R.id.excelToPdf);
        wordToPdf = findViewById(R.id.wordToPdf);
        passwordProtect = findViewById(R.id.passwordProtect);

        textToPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PDFActivity.this, TextToPdf.class));
            }
        });

        imageToPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PDFActivity.this, ImageToPdf.class));
            }
        });

        excelToPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PDFActivity.this, ExcelToPdf.class));
            }
        });

        wordToPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PDFActivity.this, WordToPdf.class));
            }
        });

        passwordProtect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PDFActivity.this, ProtectPdf.class));
            }
        });



    }
}