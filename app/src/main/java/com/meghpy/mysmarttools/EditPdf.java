package com.meghpy.mysmarttools;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class EditPdf extends AppCompatActivity {

    Button btnFinish;
    MaterialButton btnTextIncrease, btnTextDecrease;
    private ViewPager2 pdfViewPager;
    private PdfPageAdapter pageAdapter;
    private List<Bitmap> pageBitmaps = new ArrayList<>();
    private PDDocument currentDocument;
    private final PdfRepository pdfRepository = new PdfRepository();

    private ActivityResultLauncher<Intent> pdfPickerLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private DraggableTextView selectedTextView = null;
    private enum ToolMode {
        NONE,
        DRAW,
        ERASE
    }

    private ToolMode currentToolMode = ToolMode.NONE;
    private boolean isEditing = false;

    private Stack<View> undoStack = new Stack<>();
    private Stack<View> redoStack = new Stack<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pdf);
        PDFBoxResourceLoader.init(getApplicationContext());

        pdfViewPager = findViewById(R.id.pdfViewPager);
        btnFinish = findViewById(R.id.btnFinish);
        btnTextIncrease = findViewById(R.id.btnTextIncrease);
        btnTextDecrease = findViewById(R.id.btnTextDecrease);


        btnFinish.setVisibility(View.GONE);

        pdfPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        openPdf(uri);
                    }
                });

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        addImageOverlay(imageUri);
                    }
                });

        btnFinish.setOnClickListener(v -> {
            // Save or finish editing logic here

            setEditingMode(false);
        });



        findViewById(R.id.btnOpenPdf).setOnClickListener(v -> pickPdf());
        findViewById(R.id.btnSavePdf).setOnClickListener(v -> saveEditedPdf());

        btnTextIncrease.setOnClickListener(v -> {
            if (selectedTextView != null) {
                selectedTextView.increaseTextSize();
            } else {
                Toast.makeText(this, "No text selected", Toast.LENGTH_SHORT).show();
            }
        });

        btnTextDecrease.setOnClickListener(v -> {
            if (selectedTextView != null) {
                selectedTextView.decreaseTextSize();
            } else {
                Toast.makeText(this, "No text selected", Toast.LENGTH_SHORT).show();
            }
        });


        findViewById(R.id.btnDraw).setOnClickListener(v -> {
            setEditingMode(true);
            enableDrawing();
        });

        findViewById(R.id.btnEraser).setOnClickListener(v -> {
            setEditingMode(true);
            enableEraser();
        });

        findViewById(R.id.btnText).setOnClickListener(v -> {
            setEditingMode(true);
            addTextOverlay();
        });

        findViewById(R.id.btnAddPhoto).setOnClickListener(v -> {
            setEditingMode(true);
            pickImage();
        });


        findViewById(R.id.btnAddPage).setOnClickListener(v -> addBlankPage());
        findViewById(R.id.btnRemovePage).setOnClickListener(v -> removeCurrentPage());
        findViewById(R.id.btnMoreTools).setOnClickListener(v -> openReorderDialog());

        setupViewPager();


        pdfViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                applyToolModeToCurrentPage();
                TextView tvPageInfo = findViewById(R.id.tvPageInfo);
                tvPageInfo.setText((position + 1) + "/" + pageBitmaps.size());
            }
        });


        findViewById(R.id.btnUndo).setOnClickListener(v -> performUndo());
        findViewById(R.id.btnRedo).setOnClickListener(v -> performRedo());


    }
//======================================== On Create END ==============================================

    private void setEditingMode(boolean editing) {
        isEditing = editing;
        pdfViewPager.setUserInputEnabled(!editing);

        if (editing) {
            btnFinish.setVisibility(View.VISIBLE);
        } else {
            btnFinish.setVisibility(View.GONE);
        }
    }

    private void setupViewPager() {
        pageAdapter = new PdfPageAdapter(this, pageBitmaps);
        pdfViewPager.setAdapter(pageAdapter);
    }

    private void pickPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"application/pdf"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pdfPickerLauncher.launch(intent);
    }



    private void openPdf(Uri uri) {
        try {
            Log.d("DEBUG", "Uri: " + uri);
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(this, "Could not open PDF", Toast.LENGTH_SHORT).show();
                return;
            }


            int available = inputStream.available();
            Log.d("DEBUG", "InputStream available bytes: " + available);

            currentDocument = PDDocument.load(inputStream);
            int pageCount = currentDocument.getNumberOfPages();
            Log.d("DEBUG", "Number of pages: " + pageCount);

            pageBitmaps.clear();
            List<Bitmap> renderedPages = pdfRepository.renderAllPages(currentDocument);
            pageBitmaps.addAll(renderedPages);
            pageAdapter.notifyDataSetChanged();
            Log.d("DEBUG", "Rendered pages: " + renderedPages.size());
            Toast.makeText(this, "PDF Loaded: " + renderedPages.size() + " pages", Toast.LENGTH_SHORT).show();

            // Reset tool mode to NONE when new PDF loads
            currentToolMode = ToolMode.NONE;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to open PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addTextOverlay() {
        RecyclerView recyclerView = (RecyclerView) pdfViewPager.getChildAt(0);
        PdfPageAdapter.PageViewHolder holder = (PdfPageAdapter.PageViewHolder)
                recyclerView.findViewHolderForAdapterPosition(pdfViewPager.getCurrentItem());
        if (holder != null) {
            FrameLayout container = holder.itemView.findViewById(R.id.pageContainer);
            DraggableTextView textView = new DraggableTextView(this);
            textView.setText("New Text");
            textView.setX(100);
            textView.setY(100);
            container.addView(textView);

            // Set the listener to track selection
            textView.setOnSelectedListener(tv -> {
                selectedTextView = tv;
                Toast.makeText(this, "Text selected", Toast.LENGTH_SHORT).show();
            });

            undoStack.push(textView); // or imageView
            redoStack.clear(); // Clear redo history on new action


            // Set this newly added text as selected immediately
            selectedTextView = textView;
        }
    }

    private void performUndo() {
        if (!undoStack.isEmpty()) {
            View lastView = undoStack.pop();
            ((ViewGroup) lastView.getParent()).removeView(lastView);
            redoStack.push(lastView);
        } else {
            Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
        }
    }

    private void performRedo() {
        if (!redoStack.isEmpty()) {
            View view = redoStack.pop();
            RecyclerView recyclerView = (RecyclerView) pdfViewPager.getChildAt(0);
            PdfPageAdapter.PageViewHolder holder = (PdfPageAdapter.PageViewHolder)
                    recyclerView.findViewHolderForAdapterPosition(pdfViewPager.getCurrentItem());

            if (holder != null) {
                FrameLayout container = holder.itemView.findViewById(R.id.pageContainer);
                container.addView(view);
                undoStack.push(view);
            }
        } else {
            Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show();
        }
    }



    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(intent);
    }

    private void addImageOverlay(Uri uri) {
        RecyclerView recyclerView = (RecyclerView) pdfViewPager.getChildAt(0);
        PdfPageAdapter.PageViewHolder holder = (PdfPageAdapter.PageViewHolder)
                recyclerView.findViewHolderForAdapterPosition(pdfViewPager.getCurrentItem());
        if (holder != null) {
            FrameLayout container = holder.itemView.findViewById(R.id.pageContainer);
            DraggableImageView imageView = new DraggableImageView(this);
            imageView.setImageURI(uri);
            imageView.setX(100);
            imageView.setY(100);
            int size = 300;
            container.addView(imageView, new FrameLayout.LayoutParams(size, size));

            undoStack.push(imageView); // or imageView
            redoStack.clear();
        }

    }

    private void enableDrawing() {
        currentToolMode = ToolMode.DRAW;
        applyToolModeToCurrentPage();
    }

    private void enableEraser() {
        currentToolMode = ToolMode.ERASE;
        applyToolModeToCurrentPage();
    }

    private void applyToolModeToCurrentPage() {
        RecyclerView recyclerView = (RecyclerView) pdfViewPager.getChildAt(0);
        PdfPageAdapter.PageViewHolder holder = (PdfPageAdapter.PageViewHolder)
                recyclerView.findViewHolderForAdapterPosition(pdfViewPager.getCurrentItem());

        if (holder != null) {
            DrawingOverlayView overlay = holder.itemView.findViewById(R.id.drawingOverlay);
            switch (currentToolMode) {
                case DRAW:
                    overlay.setMode(DrawingOverlayView.Mode.DRAW);
                    break;
                case ERASE:
                    overlay.setMode(DrawingOverlayView.Mode.ERASE);
                    break;
                case NONE:
                default:
                    overlay.setMode(DrawingOverlayView.Mode.NONE);
                    break;
            }
        }
    }


    private void addBlankPage() {
        try {
            PDPage newPage = new PDPage();
            int index = pdfViewPager.getCurrentItem() + 1;
            currentDocument.getPages().insertAfter(newPage, currentDocument.getPage(index - 1));
            Bitmap blank = Bitmap.createBitmap(800, 1200, Bitmap.Config.ARGB_8888);
            blank.eraseColor(android.graphics.Color.WHITE);
            pageBitmaps.add(index, blank);
            pageAdapter.notifyItemInserted(index);
            pdfViewPager.setCurrentItem(index, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeCurrentPage() {
        try {
            int index = pdfViewPager.getCurrentItem();
            if (currentDocument.getNumberOfPages() <= 1) {
                Toast.makeText(this, "Cannot remove last page", Toast.LENGTH_SHORT).show();
                return;
            }
            currentDocument.removePage(index);
            pageBitmaps.remove(index);
            pageAdapter.notifyItemRemoved(index);
            pdfViewPager.setCurrentItem(Math.max(0, index - 1), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openReorderDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_reorder_pages, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();

        RecyclerView recycler = view.findViewById(R.id.reorderRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        ReorderAdapter reorderAdapter = new ReorderAdapter(this, new ArrayList<>(pageBitmaps));
        recycler.setAdapter(reorderAdapter);

        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        reorderAdapter.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                        return true;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) { }
                });
        helper.attachToRecyclerView(recycler);

        view.findViewById(R.id.btnApplyReorder).setOnClickListener(v -> {
            applyReorder(reorderAdapter.getReorderedPages());
            dialog.dismiss();
        });

        dialog.show();
    }

    private void applyReorder(List<Bitmap> newOrder) {
        try {
            List<PDPage> newPages = new ArrayList<>();
            for (Bitmap bmp : newOrder) {
                int oldIndex = pageBitmaps.indexOf(bmp);
                newPages.add(currentDocument.getPage(oldIndex));
            }

            PDDocument newDoc = new PDDocument();
            for (PDPage p : newPages) newDoc.addPage(p);

            currentDocument.close();
            currentDocument = newDoc;

            pageBitmaps.clear();
            pageBitmaps.addAll(newOrder);
            pageAdapter.notifyDataSetChanged();

            Toast.makeText(this, "Reorder done", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void saveEditedPdf() {
        try {
            int pageCount = pageBitmaps.size();

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                PDPage page = currentDocument.getPage(pageIndex);
                PDRectangle pageSize = page.getMediaBox();
                float pageWidthPt = pageSize.getWidth();
                float pageHeightPt = pageSize.getHeight();

                // Get the visible page holder
                RecyclerView recyclerView = (RecyclerView) pdfViewPager.getChildAt(0);
                PdfPageAdapter.PageViewHolder holder = (PdfPageAdapter.PageViewHolder)
                        recyclerView.findViewHolderForAdapterPosition(pageIndex);

                if (holder == null) continue;

                FrameLayout container = holder.itemView.findViewById(R.id.pageContainer);
                int containerWidthPx = container.getWidth();
                int containerHeightPx = container.getHeight();

                // Calculate scale between PDF page points and screen pixels
                float scaleX = (float) containerWidthPx / pageWidthPt;
                float scaleY = (float) containerHeightPx / pageHeightPt;

                // Merge drawing if you have it
                DrawingOverlayView overlay = holder.itemView.findViewById(R.id.drawingOverlay);
                Bitmap drawing = overlay.getDrawingBitmap();
                if (drawing != null) {
                    pdfRepository.mergeDrawingOnPage(currentDocument, pageIndex, drawing);
                }

                // Merge text and images
                for (int i = 0; i < container.getChildCount(); i++) {
                    View child = container.getChildAt(i);

                    if (child instanceof DraggableTextView) {
                        DraggableTextView tv = (DraggableTextView) child;

                        float viewX = tv.getX();
                        float viewY = tv.getY();
                        float scale = tv.getScaleX(); // Assuming uniform scale

                        float pdfX = tv.getX() / scaleX;
                        float pdfY = pageHeightPt - (tv.getY() / scaleY);
                        float pdfSize = (tv.getTextSize() * tv.getScaleX()) / scaleY;

                        pdfRepository.addTextToPage(
                                currentDocument,
                                pageIndex,
                                tv.getText().toString(),
                                pdfX,
                                pdfY,
                                pdfSize,
                                1.0f
                        );

                    }

                    if (child instanceof DraggableImageView) {
                        DraggableImageView iv = (DraggableImageView) child;
                        iv.setDrawingCacheEnabled(true);
                        iv.buildDrawingCache(true);

                        Bitmap bmp = Bitmap.createBitmap(iv.getWidth(), iv.getHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bmp);
                        iv.draw(canvas);

                        iv.setDrawingCacheEnabled(false);

                        float viewX = iv.getX();
                        float viewY = iv.getY();
                        float viewWidth = iv.getWidth() * iv.getScaleX();
                        float viewHeight = iv.getHeight() * iv.getScaleY();

                        float pdfX = viewX / scaleX;
                        float pdfY = pageHeightPt - ((viewY + viewHeight) / scaleY); // Align top-left
                        float pdfWidth = viewWidth / scaleX;
                        float pdfHeight = viewHeight / scaleY;

                        pdfRepository.addImageToPage(
                                currentDocument,
                                pageIndex,
                                bmp,
                                pdfX,
                                pdfY,
                                pdfWidth,
                                pdfHeight
                        );
                    }
                }
            }

            // Write file
            String fileName = "edited_" + System.currentTimeMillis() + ".pdf";

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream out = getContentResolver().openOutputStream(uri);
                    currentDocument.save(out);
                    out.close();
                    Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);
                OutputStream out = new FileOutputStream(file);
                currentDocument.save(out);
                out.close();
                Toast.makeText(this, "Saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }

            currentDocument.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }






}
