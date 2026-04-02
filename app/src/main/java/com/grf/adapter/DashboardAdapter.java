package com.grf.adapter;

import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.grf.model.ModuleItem;
import com.grf.smarttagmanager.R;

import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.VH> {

    public interface OnTileClick { void onClick(int position); }

    private final List<ModuleItem> items;
    private final OnTileClick listener;

    public DashboardAdapter(List<ModuleItem> items, OnTileClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dashboard_tile, parent, false);
            return new VH(v);
        } catch (Exception e) {
            e.printStackTrace();
            TextView fallback = new TextView(parent.getContext());
            fallback.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 200));
            return new VH(fallback);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        try {
            ModuleItem item = items.get(position);

            // Title
            try {
                holder.title.setText(item.title);
            } catch (Exception e) {
                holder.title.setText("");
            }

            // Subtitle (always visible)
            try {
                holder.subtitle.setText(item.subtitle != null ? item.subtitle : "Tap to view tasks");
            } catch (Exception e) {
                holder.subtitle.setText("Tap to view tasks");
            }

            // Dot gradient
            try {
                if (item.dotColors != null && item.dotColors.length >= 2) {
                    GradientDrawable gd = new GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{item.dotColors[0], item.dotColors[1]});
                    gd.setShape(GradientDrawable.OVAL);
                    holder.dot.setBackground(gd);
                }
            } catch (Exception e) {
                Log.e("DashboardAdapter", "dot color error", e);
            }

            // Click handler
            holder.card.setOnClickListener(v -> {
                try {
                    listener.onClick(position);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        View dot;
        CardView card;

        VH(@NonNull View itemView) {
            super(itemView);
            try {
                card = (CardView) itemView;
            } catch (Exception e) {
                card = null;
            }

            try {
                title = itemView.findViewById(R.id.tvTitle);
            } catch (Exception e) {
                title = new TextView(itemView.getContext());
            }

            try {
                subtitle = itemView.findViewById(R.id.tvSubtitle);
            } catch (Exception e) {
                subtitle = new TextView(itemView.getContext());
            }

            try {
                dot = itemView.findViewById(R.id.vDot);
            } catch (Exception e) {
                dot = new View(itemView.getContext());
            }
        }
    }
}
