package com.grf.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.grf.smarttagmanager.R;

import java.util.ArrayList;
import java.util.List;

public class CycleTagAdapter extends RecyclerView.Adapter<CycleTagAdapter.TagViewHolder> {

    public static class CycleTagItem {
        public final String epc;
        public int rssi;

        public CycleTagItem(String epc, int rssi) {
            this.epc = epc;
            this.rssi = rssi;
        }
    }

    private final List<CycleTagItem> items = new ArrayList<>();

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cycle_tag, parent, false);
        return new TagViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        CycleTagItem item = items.get(position);
        holder.tvTagValue.setText(item.epc);
        holder.tvDbmValue.setText(item.rssi + " dBm");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void upsertTag(String epc, int rssi) {
        for (int i = 0; i < items.size(); i++) {
            CycleTagItem existing = items.get(i);
            if (existing.epc.equals(epc)) {
                existing.rssi = rssi;
                notifyItemChanged(i);
                return;
            }
        }

        items.add(0, new CycleTagItem(epc, rssi));
        notifyItemInserted(0);
    }

    public void clearAll() {
        items.clear();
        notifyDataSetChanged();
    }

    static class TagViewHolder extends RecyclerView.ViewHolder {
        TextView tvTagValue;
        TextView tvDbmValue;

        TagViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTagValue = itemView.findViewById(R.id.tvTagValue);
            tvDbmValue = itemView.findViewById(R.id.tvDbmValue);
        }
    }
}
