package com.meghpy.mysmarttools;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.pdf.PdfDocument;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageToPdf extends AppCompatActivity {

    private Button btnPickImages, btnCreatePDF;
    private RecyclerView recyclerViewImages;
    private EditText editFileName;
    private ProgressBar progressBar;

    private final List<Uri> selectedImageUris = new ArrayList<>();
    private ImageAdapter imageAdapter;

    private final ActivityResultLauncher<Intent> pickImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUris.clear();

                    Intent data = result.getData();

                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            selectedImageUris.add(imageUri);
                        }
                    } else if (data.getData() != null) {
                        selectedImageUris.add(data.getData());
                    }

                    if (!selectedImageUris.isEmpty()) {
                        imageAdapter.setImageUris(selectedImageUris);
                        btnCreatePDF.setEnabled(true);
                    } else {
                        btnCreatePDF.setEnabled(false);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_to_pdf);


        btnPickImages = findViewById(R.id.btn_pick_images);
        btnCreatePDF = findViewById(R.id.btn_create_pdf);
        recyclerViewImages = findViewById(R.id.recycler_view_images);
        editFileName = findViewById(R.id.edit_file_name);
        progressBar = findViewById(R.id.progress_bar);

        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter();
        recyclerViewImages.setAdapter(imageAdapter);

        btnPickImages.setOnClickListener(v -> openImagePicker());
        btnCreatePDF.setOnClickListener(v -> createMultiPagePDF(selectedImageUris));
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImagesLauncher.launch(Intent.createChooser(intent, "Select Pictures"));
    }

    private Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {
        InputStream input = getContentResolver().openInputStream(selectedImage);
        ExifInterface exif = new ExifInterface(input);

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        input.close();

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return img;
        }
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    private Bitmap scaleBitmapIfTooLarge(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            // No scaling needed
            return bitmap;
        }

        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        bitmap.recycle();
        return scaledBitmap;
    }

    private void createMultiPagePDF(List<Uri> imageUris) {
        if (imageUris == null || imageUris.isEmpty()) {
            Toast.makeText(this, "No images selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = editFileName.getText().toString().trim();
        if (fileName.isEmpty()) {
            fileName = "multi_image_pdf_" + System.currentTimeMillis();
        }
        if (!fileName.endsWith(".pdf")) {
            fileName += ".pdf";
        }

        final String finalFileName = fileName;

        new Thread(() -> {
            PdfDocument pdfDocument = new PdfDocument();

            int maxPageWidth = 2000;  // Max size limit to avoid huge pages, adjust as needed
            int maxPageHeight = 2000;

            for (int i = 0; i < imageUris.size(); i++) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUris.get(i));
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (inputStream != null) inputStream.close();

                    if (bitmap != null) {
                        bitmap = rotateImageIfRequired(bitmap, imageUris.get(i));

                        Bitmap finalBitmap = scaleBitmapIfTooLarge(bitmap, maxPageWidth, maxPageHeight);

                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                                finalBitmap.getWidth(), finalBitmap.getHeight(), i + 1).create();

                        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                        page.getCanvas().drawBitmap(finalBitmap, 0, 0, null);
                        pdfDocument.finishPage(page);

                        if (finalBitmap != bitmap) {
                            finalBitmap.recycle();
                        } else {
                            bitmap.recycle();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                File outputDir = new File(getExternalFilesDir(null), "MyPDFs");
                if (!outputDir.exists()) outputDir.mkdirs();
                File outputFile = new File(outputDir, finalFileName);

                pdfDocument.writeTo(new FileOutputStream(outputFile));
                pdfDocument.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(ImageToPdf.this, "PDF saved: " + outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

                    Uri pdfUri = FileProvider.getUriForFile(
                            ImageToPdf.this,
                            getPackageName() + ".provider",
                            outputFile);

                    Intent openPdfIntent = new Intent(Intent.ACTION_VIEW);
                    openPdfIntent.setDataAndType(pdfUri, "application/pdf");
                    openPdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    openPdfIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                    try {
                        startActivity(openPdfIntent);
                    } catch (Exception e) {
                        Toast.makeText(ImageToPdf.this, "No PDF viewer found on device.", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ImageToPdf.this, "Error creating PDF", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}