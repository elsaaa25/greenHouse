package com.example.greenhouse.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.greenhouse.R;
import com.example.greenhouse.model.Plant;

import java.util.ArrayList;
import java.util.List;

public class PlantAdapter extends RecyclerView.Adapter<PlantAdapter.PlantViewHolder> {

    private List<Plant> plantList = new ArrayList<>();
    private List<Plant> plantListFull = new ArrayList<>();
    private OnPlantClickListener listener;
    private String selectedId = "";

    public interface OnPlantClickListener {
        void onPlantClick(Plant plant);
    }

    public void setOnPlantClickListener(OnPlantClickListener listener) {
        this.listener = listener;
    }

    public void setPlants(List<Plant> plants) {
        this.plantList = plants;
        this.plantListFull = new ArrayList<>(plants);
        notifyDataSetChanged();
    }

    public void setSelectedId(String id) {
        this.selectedId = id;
        notifyDataSetChanged();
    }

    public void filter(String query) {
        plantList.clear();
        if (query.isEmpty()) {
            plantList.addAll(plantListFull);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (Plant plant : plantListFull) {
                if (plant.getPlantName().toLowerCase().contains(filterPattern) ||
                    plant.getCategory().toLowerCase().contains(filterPattern)) {
                    plantList.add(plant);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant, parent, false);
        return new PlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        Plant plant = plantList.get(position);
        holder.tvPlantName.setText(plant.getPlantName());
        holder.tvCategory.setText(plant.getCategory());

        // Load image using Glide
        if (plant.getImageUrl() != null && !plant.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(plant.getImageUrl())
                    .placeholder(R.drawable.ic_vegetable_pakcoy) // fallback
                    .into(holder.ivPlant);
        }

        // Handle selection effect
        if (plant.getId().equals(selectedId)) {
            holder.containerPlant.setAlpha(0.65f);
        } else {
            holder.containerPlant.setAlpha(1.0f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlantClick(plant);
            }
        });
    }

    @Override
    public int getItemCount() {
        return plantList.size();
    }

    static class PlantViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlant;
        TextView tvPlantName, tvCategory;
        LinearLayout containerPlant;

        public PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlant = itemView.findViewById(R.id.ivPlant);
            tvPlantName = itemView.findViewById(R.id.tvPlantName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            containerPlant = itemView.findViewById(R.id.containerPlant);
        }
    }
}
