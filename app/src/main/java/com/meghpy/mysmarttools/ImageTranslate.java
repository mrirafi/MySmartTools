package com.meghpy.mysmarttools;


import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.Translation;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImageTranslate extends AppCompatActivity {

    private ImageView imagePreview;
    private Button btnSelectImage;
    private Bitmap selectedBitmap;
    private Button btnSaveImage;
    private Bitmap finalTranslatedBitmap;
    private Spinner spinnerSourceLang, spinnerTargetLang;
    private final Map<String, String> languageMap = new HashMap<>();
    private Text recognizedTextBlock = null;
    private Map<String, String> sourceLanguageMap = new HashMap<>();
    private Map<String, String> targetLanguageMap = new HashMap<>();

    private ProgressBar progressBar;
    private TextView textDownloadStatus;
    private Handler handler = new Handler();
    private int progressPercent = 0;
    private Runnable progressRunnable;


    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            selectedBitmap = loadBitmapFromUri(imageUri);
                            imagePreview.setImageBitmap(selectedBitmap);
                            runTextRecognition(selectedBitmap);

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_translate);
        imagePreview = findViewById(R.id.imagePreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        progressBar = findViewById(R.id.progressBar);
        textDownloadStatus = findViewById(R.id.textDownloadStatus);


        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imagePreview.setImageDrawable(null);
                selectedBitmap = null;
                openImagePicker();
            }
        });

        btnSaveImage = findViewById(R.id.btnSaveImage);

        btnSaveImage.setBackgroundColor(Color.GRAY); // Inactive gray

        btnSaveImage.setOnClickListener(v -> {
            if (finalTranslatedBitmap != null) {
                saveImageToDownloads(finalTranslatedBitmap);
            } else {
                Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
            }
        });
        spinnerSourceLang = findViewById(R.id.spinnerSourceLang);
        spinnerTargetLang = findViewById(R.id.spinnerTargetLang);

        setupLanguageSpinners();

        Button btnToggleTranslation = findViewById(R.id.btnToggleTranslation);

        final boolean[] isTranslationVisible = {true}; // track state, start visible after translation

        btnToggleTranslation.setOnClickListener(v -> {
            if (selectedBitmap == null) return;

            if (isTranslationVisible[0]) {
                // Hide translation: show original image
                imagePreview.setImageBitmap(selectedBitmap);
                btnToggleTranslation.setText("Show Translation");
                btnSaveImage.setEnabled(false);
                btnSaveImage.setBackgroundColor(Color.GRAY); // Inactive gray
                isTranslationVisible[0] = false;
            } else {
                // Show translation: show translated image if available
                if (finalTranslatedBitmap != null) {
                    imagePreview.setImageBitmap(finalTranslatedBitmap);
                    btnToggleTranslation.setText("Hide Translation");
                    btnSaveImage.setEnabled(true);
                    btnSaveImage.setBackgroundColor(getResources().getColor(R.color.primaryDark)); // Active color
                    isTranslationVisible[0] = true;
                } else {
                    Toast.makeText(this, "No translated image available", Toast.LENGTH_SHORT).show();
                }
            }
        });



    }

    private void setupLanguageSpinners() {
        languageMap.clear();

        List<String> sourceLangNames = new ArrayList<>();
        Map<String, String> sourceLangMap = new HashMap<>();
        for (String code : MLKIT_ON_DEVICE_DETECTABLE_CODES) {
            String name = getLanguageDisplayName(code);
            sourceLangNames.add(name);
            sourceLangMap.put(name, code);
        }
        Collections.sort(sourceLangNames);

        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, sourceLangNames);
        spinnerSourceLang.setAdapter(sourceAdapter);

        List<String> allTranslatableCodes = new ArrayList<>(TranslateLanguage.getAllLanguages());
        List<String> targetLangNames = new ArrayList<>();
        Map<String, String> targetLangMap = new HashMap<>();
        for (String code : allTranslatableCodes) {
            String name = getLanguageDisplayName(code);
            targetLangNames.add(name);
            targetLangMap.put(name, code);
        }
        Collections.sort(targetLangNames);

        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, targetLangNames);
        spinnerTargetLang.setAdapter(targetAdapter);

        sourceLanguageMap = sourceLangMap;
        targetLanguageMap = targetLangMap;

        spinnerSourceLang.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                triggerTranslation();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerTargetLang.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                triggerTranslation();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }


    private static final List<String> MLKIT_ON_DEVICE_DETECTABLE_CODES = Arrays.asList(
            TranslateLanguage.ENGLISH,    // en
            TranslateLanguage.SPANISH,    // es
            TranslateLanguage.PORTUGUESE, // pt
            TranslateLanguage.FRENCH,     // fr
            TranslateLanguage.GERMAN,     // de
            TranslateLanguage.CHINESE,    // zh
            TranslateLanguage.KOREAN,     // ko
            TranslateLanguage.HINDI       // hi
    );


    private String getLanguageDisplayName(String code) {
        try {
            Locale locale = new Locale(code);
            return locale.getDisplayLanguage(Locale.ENGLISH);
        } catch (Exception e) {
            return code.toUpperCase(); // fallback
        }
    }


    private void triggerTranslation() {
        if (recognizedTextBlock == null || selectedBitmap == null) return;

        String textForDetection = recognizedTextBlock.getText();

        com.google.mlkit.nl.languageid.LanguageIdentifier identifier =
                com.google.mlkit.nl.languageid.LanguageIdentification.getClient();

        identifier.identifyLanguage(textForDetection)
                .addOnSuccessListener(detectedLangCode -> {
                    if (detectedLangCode.equals("und")) {
                        Toast.makeText(this, "Couldn't detect language", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // If detected language is in your ML Kit detectable list, select it in source spinner
                    if (MLKIT_ON_DEVICE_DETECTABLE_CODES.contains(detectedLangCode)) {
                        String detectedLangName = getLanguageDisplayName(detectedLangCode);
                        int pos = ((ArrayAdapter<String>) spinnerSourceLang.getAdapter()).getPosition(detectedLangName);
                        if (pos >= 0) {
                            spinnerSourceLang.setSelection(pos);
                        }
                    } else {
                        Toast.makeText(this, "Detected language not supported for auto selection: " + detectedLangCode, Toast.LENGTH_SHORT).show();
                    }

                    // Translate using current spinner selection and detected or manual target lang
                    String sourceLang = detectedLangCode;
                    String targetLang = targetLanguageMap.get(spinnerTargetLang.getSelectedItem().toString());

                    if (sourceLang == null || targetLang == null) {
                        Toast.makeText(this, "Language selection error", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    translateAndOverlay(recognizedTextBlock, selectedBitmap, sourceLang, targetLang);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(this, "Language detection failed", Toast.LENGTH_SHORT).show();
                });
    }



    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);

    }

    private Bitmap loadBitmapFromUri(Uri uri) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
        } else {
            return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        }
    }


    private void runTextRecognition(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(this::onTextRecognized)
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to recognize text", Toast.LENGTH_SHORT).show();
                });
    }

    private void onTextRecognized(Text visionText) {
        if (visionText.getTextBlocks().isEmpty()) {
            Toast.makeText(this, "No text found in image", Toast.LENGTH_SHORT).show();
            return;
        }

        recognizedTextBlock = visionText; // ✅ Store globally

        // Detect source and translate
        triggerTranslation();
    }


    private void translateAndOverlay(Text visionText, Bitmap bitmap, String sourceLangCode, String targetLangCode) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLangCode)
                .setTargetLanguage(targetLangCode)
                .build();

        Translator translator = Translation.getClient(options);

        progressBar.setVisibility(View.VISIBLE);
        textDownloadStatus.setVisibility(View.VISIBLE);
        progressPercent = 0;

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                progressPercent += 5;
                if (progressPercent <= 95) {
                    textDownloadStatus.setText("Downloading model: " + progressPercent + "%");
                    handler.postDelayed(this, 200);
                }
            }
        };
        handler.post(progressRunnable);


        translator.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> {

                    handler.removeCallbacks(progressRunnable);
                    textDownloadStatus.setText("Model downloaded. Translating...");


                    Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(mutableBitmap);

                    Paint textPaint = new Paint();
                    textPaint.setColor(Color.BLACK);
                    textPaint.setStyle(Paint.Style.FILL);
                    textPaint.setAntiAlias(true);
                    textPaint.setTextAlign(Paint.Align.LEFT);
                    textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                    Paint bgPaint = new Paint();
                    bgPaint.setColor(Color.parseColor("#FFFFFFFF"));

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        Rect rect = block.getBoundingBox();
                        Point[] cornerPoints = block.getCornerPoints();
                        String sourceText = block.getText();

                        if (rect == null || cornerPoints == null) continue;

                        float dx = cornerPoints[1].x - cornerPoints[0].x;
                        float dy = cornerPoints[1].y - cornerPoints[0].y;
                        double angle = Math.toDegrees(Math.atan2(dy, dx));

                        translator.translate(sourceText)
                                .addOnSuccessListener(translatedText -> {

                                    progressBar.setVisibility(View.GONE);
                                    textDownloadStatus.setVisibility(View.GONE);


                                    float maxWidth = rect.width();
                                    float maxHeight = rect.height();

                                    // Try to find best font size that fits
                                    float bestSize = findBestFontSize(translatedText, textPaint, maxWidth, maxHeight);

                                    textPaint.setTextSize(bestSize);

                                    // Re-break lines
                                    List<String> lines = breakTextIntoLines(translatedText, textPaint, maxWidth, angle);

                                    // Draw background box (rotated!)
                                    float pivotX = rect.left + rect.width() / 2f;
                                    float pivotY = rect.top + rect.height() / 2f;

                                    canvas.save();
                                    canvas.rotate((float) angle, pivotX, pivotY);

                                    float totalHeight = lines.size() * bestSize * 1.2f;

                                    float bgLeft = pivotX - maxWidth / 2;
                                    float bgTop = pivotY - totalHeight / 2;
                                    float bgRight = pivotX + maxWidth / 2;
                                    float bgBottom = pivotY + totalHeight / 2;

                                    canvas.drawRect(bgLeft, bgTop, bgRight, bgBottom, bgPaint);

                                    // Draw text lines
                                    float y = bgTop + bestSize;
                                    for (String line : lines) {
                                        float textWidth = textPaint.measureText(line);
                                        float x = pivotX - textWidth / 2;
                                        canvas.drawText(line, x, y, textPaint);
                                        y += bestSize * 1.2f;
                                    }

                                    canvas.restore();
                                    imagePreview.setImageBitmap(mutableBitmap);

                                    finalTranslatedBitmap = mutableBitmap;

                                    // ✅ Enable Save button after successful translation
                                    btnSaveImage.setEnabled(true);
                                    btnSaveImage.setBackgroundColor(getResources().getColor(R.color.primaryDark)); // Active color


                                })
                                .addOnFailureListener(e -> Log.e("Translation", e.getMessage()));
                    }

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to download translation model", Toast.LENGTH_SHORT).show());
    }

    private float findBestFontSize(String text, Paint paint, float maxWidth, float maxHeight) {
        float size = 100f; // start big
        paint.setTextSize(size);
        List<String> lines;

        while (size > 10f) {
            paint.setTextSize(size);
            lines = breakTextIntoLines(text, paint, maxWidth, 0);
            float totalHeight = lines.size() * size * 1.2f;

            boolean fits = true;
            for (String line : lines) {
                if (paint.measureText(line) > maxWidth) {
                    fits = false;
                    break;
                }
            }
            if (fits && totalHeight <= maxHeight) {
                return size;
            }
            size -= 1f;
        }
        return size;
    }


    private List<String> breakTextIntoLines(String text, Paint paint, float maxWidth, double angle) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }


    private void saveImageToDownloads(Bitmap bitmap) {
        String filename = "translated_image_" + System.currentTimeMillis() + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/TranslatedImages");

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();
                    Toast.makeText(this, "Saved to DCIM/TranslatedImages", Toast.LENGTH_LONG).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }



}