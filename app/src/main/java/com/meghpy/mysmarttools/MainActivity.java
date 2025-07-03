package com.meghpy.mysmarttools;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    CardView cardOCR, cardPDF, cardQR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cardOCR = findViewById(R.id.cardOCR);
        cardPDF = findViewById(R.id.cardPDF);
        cardQR = findViewById(R.id.cardQR);

        cardOCR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, OCRActivity.class));
            }
        });

        cardPDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PDFActivity.class));
            }
        });

        cardQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, QRScannerActivity.class));
            }
        });
    }
}