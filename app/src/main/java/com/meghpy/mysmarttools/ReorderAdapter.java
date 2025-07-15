package com.meghpy.mysmarttools;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class ReorderAdapter extends RecyclerView.Adapter<ReorderAdapter.ViewHolder> {

    private Context context;
    private List<Bitmap> pages;

    public ReorderAdapter(Context context, List<Bitmap> pages) {
        this.context = context;
        this.pages = pages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reorder_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.pageThumbnail.setImageBitmap(pages.get(position));
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    public void moveItem(int from, int to) {
        if (from < to) {
            for (int i = from; i < to; i++) {
                Collections.swap(pages, i, i + 1);
            }
        } else {
            for (int i = from; i > to; i--) {
                Collections.swap(pages, i, i - 1);
            }
        }
        notifyItemMoved(from, to);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView pageThumbnail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            pageThumbnail = itemView.findViewById(R.id.pageThumb);
        }
    }

    public List<Bitmap> getReorderedPages() {
        return pages;
    }
}
