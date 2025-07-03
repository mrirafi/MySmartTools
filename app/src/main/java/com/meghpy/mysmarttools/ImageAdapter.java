package com.meghpy.mysmarttools;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<Uri> imageUris = new ArrayList<>();

    public void setImageUris(List<Uri> uris) {
        this.imageUris = uris;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_preview, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        holder.image_preview.setImageURI(imageUris.get(position));
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView image_preview;

        public ImageViewHolder(View itemView) {
            super(itemView);
            image_preview = itemView.findViewById(R.id.image_preview);
        }
    }
}
