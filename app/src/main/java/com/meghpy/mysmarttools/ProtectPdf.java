package com.meghpy.mysmarttools;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission;
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ProtectPdf extends AppCompatActivity {

    private EditText passwordInput;
    private LinearLayout pdfPageContainer;
    private View emptyState;
    private Uri selectedPdfUri;
    private LinearProgressIndicator progressBar;
    private MaterialButton btnProtect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protect_pdf);

        passwordInput = findViewById(R.id.passwordInput);
        pdfPageContainer = findViewById(R.id.pdfPageContainer);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.passwordStrengthBar);
        btnProtect = findViewById(R.id.btnProtectPDF);

        TextInputEditText passwordInput = findViewById(R.id.passwordInput);
        TextView strengthText = findViewById(R.id.passwordStrength);
        LinearProgressIndicator strengthBar = findViewById(R.id.passwordStrengthBar);

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                int score = calculateStrength(password);
                updateStrengthUI(score, strengthText, strengthBar);
            }
        });


        MaterialButton btnSelectPDF = findViewById(R.id.btnSelectPDF);
        btnSelectPDF.setOnClickListener(v -> selectPdf());

        btnProtect.setOnClickListener(v -> {
            String password = passwordInput.getText().toString().trim();
            if (selectedPdfUri == null) {
                Toast.makeText(this, "Select a PDF first", Toast.LENGTH_SHORT).show();
            } else if (password.isEmpty()) {
                Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();
            } else {
                protectPdf(selectedPdfUri, password);
            }
        });
    }
    //================================ On Create END =========================================


    //================================ Method Start Here =========================================

    private int calculateStrength(String password) {
        if (password.length() == 0) return 0;
        int score = 0;

        if (password.length() >= 8) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score++;

        return score; // max 5
    }

    private void updateStrengthUI(int score, TextView label, LinearProgressIndicator bar) {
        switch (score) {
            case 0:
            case 1:
                label.setText("Very Weak");
                label.setTextColor(Color.RED);
                bar.setIndicatorColor(Color.RED);
                bar.setProgressCompat(20, true);
                break;
            case 2:
                label.setText("Weak");
                label.setTextColor(Color.parseColor("#FF9800")); // Orange
                bar.setIndicatorColor(Color.parseColor("#FF9800"));
                bar.setProgressCompat(40, true);
                break;
            case 3:
                label.setText("Medium");
                label.setTextColor(Color.YELLOW);
                bar.setIndicatorColor(Color.YELLOW);
                bar.setProgressCompat(60, true);
                break;
            case 4:
                label.setText("Strong");
                label.setTextColor(Color.parseColor("#4CAF50")); // Green
                bar.setIndicatorColor(Color.parseColor("#4CAF50"));
                bar.setProgressCompat(80, true);
                break;
            case 5:
                label.setText("Very Strong");
                label.setTextColor(Color.parseColor("#388E3C")); // Dark Green
                bar.setIndicatorColor(Color.parseColor("#388E3C"));
                bar.setProgressCompat(100, true);
                break;
        }
    }


    private void selectPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pdfPickerLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> pdfPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        selectedPdfUri = uri;
                        displayPdfPreview(uri);
                    }
                }
            });

    private void displayPdfPreview(Uri uri) {
        try {
            ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(uri, "r");
            if (descriptor == null) return;

            PdfRenderer renderer = new PdfRenderer(descriptor);
            pdfPageContainer.removeAllViews();
            emptyState.setVisibility(View.GONE);

            for (int i = 0; i < renderer.getPageCount(); i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                ZoomableImageView imageView = new ZoomableImageView(this);
                imageView.setImageBitmap(bitmap);
                imageView.setPadding(8, 8, 8, 8);
                pdfPageContainer.addView(imageView);
            }

            renderer.close();
            descriptor.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error displaying PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRenameAndSaveDialog(byte[] pdfBytes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save As");

        final EditText input = new EditText(this);
        input.setHint("Enter file name (without .pdf)");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText("protected_pdf_" + System.currentTimeMillis());

        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String filename = input.getText().toString().trim();
            if (filename.isEmpty()) {
                Toast.makeText(this, "File name can't be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            saveToDownloads(pdfBytes, filename + ".pdf");
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveToDownloads(byte[] pdfBytes, String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/MySmartTools");

        ContentResolver resolver = getContentResolver();
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }

        if (uri != null) {
            try (OutputStream out = resolver.openOutputStream(uri)) {
                out.write(pdfBytes);
                out.flush();

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(uri, values, null, null);

                Toast.makeText(this, "Saved to Downloads/MySmartTools", Toast.LENGTH_SHORT).show();

                showOpenDialog(uri); // ✅ Show "Open PDF" option

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to write file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showOpenDialog(Uri fileUri) {
        new AlertDialog.Builder(this)
                .setTitle("PDF Saved")
                .setMessage("Do you want to open the PDF now?")
                .setPositiveButton("Open", (dialog, which) -> openPdfFile(fileUri))
                .setNegativeButton("Close", null)
                .show();
    }

    private void openPdfFile(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open PDF", Toast.LENGTH_SHORT).show();
        }
    }



    private void protectPdf(Uri pdfUri, String password) {
        try {
            ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(pdfUri, "r");
            if (descriptor == null) {
                Toast.makeText(this, "Error opening PDF", Toast.LENGTH_SHORT).show();
                return;
            }

            PDDocument document = PDDocument.load(new FileInputStream(descriptor.getFileDescriptor()));

            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy(password, password, ap);
            spp.setEncryptionKeyLength(128);
            spp.setPermissions(ap);

            document.protect(spp);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            document.close();

            byte[] pdfBytes = outputStream.toByteArray();

            showRenameAndSaveDialog(pdfBytes); // ✅ Prompt rename before saving

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to protect PDF", Toast.LENGTH_SHORT).show();
        }
    }

}
