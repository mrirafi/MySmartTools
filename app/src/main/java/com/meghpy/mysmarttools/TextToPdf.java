package com.meghpy.mysmarttools;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.*;
import android.text.style.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import yuku.ambilwarna.AmbilWarnaDialog;
import java.io.*;

public class TextToPdf extends AppCompatActivity {

    private EditText editText;
    private int selectedColor = Color.BLACK;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private final int PAGE_WIDTH = 595;
    private final int PAGE_HEIGHT = 842;
    private final int MARGIN = 40;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_to_pdf);

        editText = findViewById(R.id.editText);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        insertImageAtCursor(uri);
                    }
                });

        findViewById(R.id.btnBold).setOnClickListener(v -> toggleStyle(Typeface.BOLD));
        findViewById(R.id.btnItalic).setOnClickListener(v -> toggleStyle(Typeface.ITALIC));
        findViewById(R.id.btnUnderline).setOnClickListener(v -> toggleUnderline());
        findViewById(R.id.btnColorPicker).setOnClickListener(v -> openColorPicker());
        findViewById(R.id.btnFontSize).setOnClickListener(v -> changeFontSize());
        findViewById(R.id.btnTextSizeDown).setOnClickListener(v -> decreaseFontSize());
        findViewById(R.id.btnAlignLeft).setOnClickListener(v -> applyAlignment(Layout.Alignment.ALIGN_NORMAL));
        findViewById(R.id.btnAlignCenter).setOnClickListener(v -> applyAlignment(Layout.Alignment.ALIGN_CENTER));
        findViewById(R.id.btnAlignRight).setOnClickListener(v -> applyAlignment(Layout.Alignment.ALIGN_OPPOSITE));
        findViewById(R.id.btnInsertImage).setOnClickListener(v -> openImagePicker());
        findViewById(R.id.btnGeneratePdf).setOnClickListener(v -> generatePdf());
    }

    private void toggleStyle(int style) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start == end) return;
        Spannable str = editText.getText();
        StyleSpan[] spans = str.getSpans(start, end, StyleSpan.class);
        boolean exists = false;
        for (StyleSpan span : spans) {
            if (span.getStyle() == style) {
                str.removeSpan(span);
                exists = true;
            }
        }
        if (!exists) str.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void toggleUnderline() {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start == end) return;
        Spannable str = editText.getText();
        UnderlineSpan[] spans = str.getSpans(start, end, UnderlineSpan.class);
        for (UnderlineSpan span : spans) str.removeSpan(span);
        str.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void openColorPicker() {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start == end) {
            Toast.makeText(this, "Select text first", Toast.LENGTH_SHORT).show();
            return;
        }

        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, selectedColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                selectedColor = color;
                editText.getText().setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog) { }
        });
        dialog.show();
    }

    private void changeFontSize() {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start == end) return;
        Spannable str = editText.getText();
        AbsoluteSizeSpan[] spans = str.getSpans(start, end, AbsoluteSizeSpan.class);
        int newSize = 18;
        if (spans.length > 0) {
            int current = spans[0].getSize();
            newSize = (current == 14) ? 18 : (current == 18) ? 22 : 14;
            for (AbsoluteSizeSpan span : spans) str.removeSpan(span);
        }
        str.setSpan(new AbsoluteSizeSpan(newSize, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void decreaseFontSize() {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start == end) return;
        Spannable str = editText.getText();
        AbsoluteSizeSpan[] spans = str.getSpans(start, end, AbsoluteSizeSpan.class);
        int newSize = 14;
        if (spans.length > 0) {
            int current = spans[0].getSize();
            newSize = (current == 22) ? 18 : (current == 18) ? 14 : 22;
            for (AbsoluteSizeSpan span : spans) str.removeSpan(span);
        }
        str.setSpan(new AbsoluteSizeSpan(newSize, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void applyAlignment(Layout.Alignment alignment) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start == end) return;
        Spannable str = editText.getText();
        AlignmentSpan.Standard[] spans = str.getSpans(start, end, AlignmentSpan.Standard.class);
        for (AlignmentSpan.Standard span : spans) str.removeSpan(span);
        str.setSpan(new AlignmentSpan.Standard(alignment), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void insertImageAtCursor(Uri imageUri) {
        try {
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // Scale only for preview
            int maxW = 400, maxH = 400;
            int w = originalBitmap.getWidth(), h = originalBitmap.getHeight();
            float scale = Math.min((float) maxW / w, (float) maxH / h);
            Bitmap previewBitmap = originalBitmap;
            if (scale < 1) {
                previewBitmap = Bitmap.createScaledBitmap(originalBitmap, (int) (w * scale), (int) (h * scale), true);
            }

            // Use RichImageSpan
            RichImageSpan imageSpan = new RichImageSpan(this, previewBitmap, imageUri);

            SpannableString ss = new SpannableString("\uFFFC");
            ss.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            int pos = editText.getSelectionStart();
            editText.getText().insert(pos, ss);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void generatePdf() {
        new Thread(() -> {
            PdfDocument document = new PdfDocument();
            int y = 0;
            int pageNum = 1;
            PdfDocument.Page page = startNewPage(document, pageNum++);
            Canvas canvas = page.getCanvas();
            canvas.translate(MARGIN, MARGIN);

            TextPaint paint = new TextPaint();
            Spannable content = editText.getText();

            int width = PAGE_WIDTH - 2 * MARGIN;
            int height = PAGE_HEIGHT - 2 * MARGIN;

            for (int i = 0; i < content.length(); ) {
                int nextSpanStart = content.length();
                ImageSpan nextImageSpan = null;

                // Find the next image span after i
                for (ImageSpan span : content.getSpans(i, content.length(), ImageSpan.class)) {
                    int spanStart = content.getSpanStart(span);
                    if (spanStart >= i && spanStart < nextSpanStart) {
                        nextSpanStart = spanStart;
                        nextImageSpan = span;
                    }
                }

                // Handle text before the next image span
                CharSequence sub = content.subSequence(i, nextSpanStart);
                StaticLayout layout = StaticLayout.Builder.obtain(sub, 0, sub.length(), paint, width).build();

                int layoutHeight = layout.getHeight();
                int imageHeight = 0;
                Bitmap bitmap = null;

                // If there's an image right after the text
                if (nextImageSpan != null && content.getSpanStart(nextImageSpan) == nextSpanStart) {
                    bitmap = getOriginalBitmapFromSpan(nextImageSpan);
                    if (bitmap != null) {
                        int targetWidth = (int) (4.0 * 72);
                        float scale = (float) targetWidth / bitmap.getWidth();
                        bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, (int) (bitmap.getHeight() * scale), true);
                        imageHeight = bitmap.getHeight();
                    }
                }

                // Check if text + image fit
                int totalNeededHeight = layoutHeight + imageHeight;
                if (y + totalNeededHeight > height) {
                    document.finishPage(page);
                    page = startNewPage(document, pageNum++);
                    canvas = page.getCanvas();
                    canvas.translate(MARGIN, MARGIN);
                    y = 0;
                }

                // Draw text
                canvas.save();
                canvas.translate(0, y);
                layout.draw(canvas);
                canvas.restore();
                y += layoutHeight;

                // Draw image if any
                if (bitmap != null) {
                    float x = (width - bitmap.getWidth()) / 2f;
                    canvas.drawBitmap(bitmap, x, y, null);

                    y += imageHeight;
                    i = content.getSpanEnd(nextImageSpan); // move past image span
                } else {
                    i = nextSpanStart; // move past text only
                }
            }


            document.finishPage(page);

            // Save to downloads
            String fileName = "RichText_" + System.currentTimeMillis() + ".pdf";
            Uri uri = savePdfToDownloads(document, fileName);
            document.close();

            runOnUiThread(() -> {
                if (uri != null) {
                    Toast.makeText(this, "PDF Saved", Toast.LENGTH_SHORT).show();
                    openPdf(uri);
                } else {
                    Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
                }
            });

        }).start();
    }

    public class RichImageSpan extends ImageSpan {
        private final Uri imageUri;

        public RichImageSpan(Context context, Bitmap b, Uri imageUri) {
            super(context, b);
            this.imageUri = imageUri;
        }

        public Uri getImageUri() {
            return imageUri;
        }
    }


    private PdfDocument.Page startNewPage(PdfDocument doc, int pageNum) {
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create();
        return doc.startPage(info);
    }

    private Bitmap getOriginalBitmapFromSpan(ImageSpan span) {
        if (span instanceof RichImageSpan) {
            Uri uri = ((RichImageSpan) span).getImageUri();
            try {
                return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // fallback
            return getBitmapFromImageSpan(span);
        }
        return null;
    }



    private Bitmap getBitmapFromImageSpan(ImageSpan span) {
        try {
            Drawable drawable = span.getDrawable();
            if (drawable instanceof BitmapDrawable) return ((BitmapDrawable) drawable).getBitmap();
        } catch (Exception ignored) {}
        return null;
    }

    private Uri savePdfToDownloads(PdfDocument doc, String name) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        doc.writeTo(os);
                        return uri;
                    }
                }
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, name);
                FileOutputStream fos = new FileOutputStream(file);
                doc.writeTo(fos);
                fos.close();
                return FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void openPdf(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No PDF viewer app found", Toast.LENGTH_SHORT).show();
        }
    }
}
