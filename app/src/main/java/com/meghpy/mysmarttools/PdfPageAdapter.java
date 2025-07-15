package com.meghpy.mysmarttools;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageViewHolder> {

    private Context context;
    private List<Bitmap> pages;

    public interface OnPageReadyListener {
        void onPageReady(int position, FrameLayout container, DrawingOverlayView drawingOverlay);
    }

    private OnPageReadyListener listener;

    public PdfPageAdapter(Context context, List<Bitmap> pages) {
        this.context = context;
        this.pages = pages;
    }

    public void setOnPageReadyListener(OnPageReadyListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pdf_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.pageImage.setImageBitmap(pages.get(position));

        if (listener != null) {
            listener.onPageReady(position, holder.pageContainer, holder.drawingOverlay);
        }
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        ImageView pageImage;
        DrawingOverlayView drawingOverlay;
        FrameLayout pageContainer;

        public PageViewHolder(@NonNull View itemView) {
            super(itemView);
            pageImage = itemView.findViewById(R.id.pageImage);
            drawingOverlay = itemView.findViewById(R.id.drawingOverlay);
            pageContainer = itemView.findViewById(R.id.pageContainer);
        }
    }
}
