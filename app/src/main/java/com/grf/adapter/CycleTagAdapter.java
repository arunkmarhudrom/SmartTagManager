package com.grf.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.grf.smarttagmanager.R;
import com.grf.viewmodel.ModuleViewModel;

import java.util.ArrayList;
import java.util.List;

public class CycleTagAdapter extends RecyclerView.Adapter<CycleTagAdapter.TagViewHolder> {

    public static class CycleTagItem {
        public final String epc;
        public int rssi;
        public int signalPercent;

        public CycleTagItem(String epc, int rssi, int signalPercent) {
            this.epc = epc;
            this.rssi = rssi;
            this.signalPercent = signalPercent;
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
        holder.tvSignalValue.setText(item.signalPercent + "%");

        int tintColor = getSignalColor(holder.itemView.getContext(), item.signalPercent);
        holder.progressSignal.setProgress(item.signalPercent);
        holder.progressSignal.getProgressDrawable().setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
        holder.tvSignalValue.setTextColor(tintColor);
        holder.tvDbmValue.setTextColor(tintColor);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void upsertTag(String epc, int rssi) {
        int signalPercent = ModuleViewModel.rssiToStrength0to100(rssi);

        for (int i = 0; i < items.size(); i++) {
            CycleTagItem existing = items.get(i);
            if (existing.epc.equals(epc)) {
                existing.rssi = rssi;
                existing.signalPercent = signalPercent;
                sortByThreshold();
                notifyDataSetChanged();
                return;
            }
        }

        items.add(new CycleTagItem(epc, rssi, signalPercent));
        sortByThreshold();
        notifyDataSetChanged();
    }

    public void clearAll() {
        items.clear();
        notifyDataSetChanged();
    }

    private int getSignalColor(Context context, int percent) {
        if (percent >= ModuleViewModel.greenTh) {
            return ContextCompat.getColor(context, R.color.signal_green);
        } else if (percent >= ModuleViewModel.yellowTh) {
            return ContextCompat.getColor(context, R.color.signal_yellow);
        } else if (percent >= ModuleViewModel.orangeTh) {
            return ContextCompat.getColor(context, R.color.signal_orange);
        } else {
            return ContextCompat.getColor(context, R.color.signal_red);
        }
    }

    private void sortByThreshold() {
        items.sort((left, right) -> {
            int bucketCompare = Integer.compare(
                    getSignalBucket(left.signalPercent),
                    getSignalBucket(right.signalPercent)
            );
            if (bucketCompare != 0) {
                return bucketCompare;
            }
            return Integer.compare(right.signalPercent, left.signalPercent);
        });
    }

    private int getSignalBucket(int percent) {
        if (percent >= ModuleViewModel.greenTh) {
            return 0;
        } else if (percent >= ModuleViewModel.yellowTh) {
            return 1;
        } else if (percent >= ModuleViewModel.orangeTh) {
            return 2;
        }
        return 3;
    }

    static class TagViewHolder extends RecyclerView.ViewHolder {
        TextView tvTagValue;
        TextView tvDbmValue;
        TextView tvSignalValue;
        ProgressBar progressSignal;

        TagViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTagValue = itemView.findViewById(R.id.tvTagValue);
            tvDbmValue = itemView.findViewById(R.id.tvDbmValue);
            tvSignalValue = itemView.findViewById(R.id.tvSignalValue);
            progressSignal = itemView.findViewById(R.id.progressSignal);
        }
    }
}
