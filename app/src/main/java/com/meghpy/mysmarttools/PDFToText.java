package com.meghpy.mysmarttools;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.*;

import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.*;

public class PDFToText extends AppCompatActivity {

    private static final int PICK_PDF_FILE = 10;
    TextView tvExtractedText;
    Button btnPickPdf, btnCopyText;
    StringBuilder allText = new StringBuilder();

    ProgressBar progressBar;
    int pageProcessed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pdfto_text);

        tvExtractedText = findViewById(R.id.tvExtractedText);
        btnPickPdf = findViewById(R.id.btnPickPdf);
        btnCopyText = findViewById(R.id.btnCopyText);
        progressBar = findViewById(R.id.progressBar);

        btnPickPdf.setOnClickListener(v -> pickPdfFromDevice());

        btnCopyText.setOnClickListener(v -> {
            String text = tvExtractedText.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("PDF Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Text copied", Toast.LENGTH_SHORT).show();
        });
    }

    private void pickPdfFromDevice() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_PDF_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_PDF_FILE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                readPdfAndExtractText(uri);
            }
        }
    }

    private void readPdfAndExtractText(Uri uri) {
        pageProcessed = 0;
        progressBar.setVisibility(View.VISIBLE);
        tvExtractedText.setText(""); // Clear previous output


        allText.setLength(0); // clear previous
        try {
            ParcelFileDescriptor fileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            if (fileDescriptor != null) {
                PdfRenderer renderer = new PdfRenderer(fileDescriptor);

                for (int i = 0; i < renderer.getPageCount(); i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    int width = getResources().getDisplayMetrics().densityDpi / 72 * page.getWidth();
                    int height = getResources().getDisplayMetrics().densityDpi / 72 * page.getHeight();

                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();

                    extractTextFromBitmap(bitmap, i, renderer.getPageCount());
                }
                renderer.close();
                fileDescriptor.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to read PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void extractTextFromBitmap(Bitmap bitmap, int pageIndex, int totalPages) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener(result -> {
                    allText.append("Page ").append(pageIndex + 1).append(":\n");
                    allText.append(result.getText()).append("\n\n");

                    pageProcessed++;

                    if (pageProcessed == totalPages) {
                        // All pages processed
                        tvExtractedText.setText(allText.toString());
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    pageProcessed++;
                    if (pageProcessed == totalPages) {
                        tvExtractedText.setText(allText.toString());
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "OCR failed on page " + (pageIndex + 1), Toast.LENGTH_SHORT).show();
                });
    }

}