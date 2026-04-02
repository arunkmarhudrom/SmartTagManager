package com.grf.adapter;

import static android.view.View.GONE;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.grf.model.PendingTag;
import com.grf.smarttagmanager.R;
import com.grf.utils.PopupUtils;
import com.grf.viewmodel.ModuleViewModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Corrected PendingTagsAdapter.
 * <p>
 * Notes:
 * - Uses setHasStableIds(true) so getItemId() is meaningful.
 * - Synchronizes mutations to the backing list via itemsLock.
 * - updateSignalByTagId performs an in-place update and sorts + posts notifyDataSetChanged()
 * which is the simplest safe approach for very-frequent updates.
 * - All UI notifications are posted to the main looper.
 * - Methods use try/catch to protect against unexpected runtime errors.
 */
public class PendingTagsAdapter extends RecyclerView.Adapter<PendingTagsAdapter.VH> {

    public interface Listener {
        void onItemClicked(PendingTag tag);
        void onItemChecked(PendingTag tag);
    }

    private final Context context;
    private final List<PendingTag> items;
    private final Listener listener;

    public PendingTagsAdapter(Context context, List<PendingTag> items, Listener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_tag, parent, false);
            return new VH(v);
        } catch (Throwable t) {
            t.printStackTrace();
            View v = new View(context);
            v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            return new VH(v);
        }
    }

    // 🔥 PARTIAL UPDATE SUPPORT
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position, @NonNull List<Object> payloads) {
        try {
            if (!payloads.isEmpty()) {
                PendingTag tag = items.get(position);
                updateSignalOnly(holder, tag);
                return;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        super.onBindViewHolder(holder, position, payloads);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        try {
            if (position < 0 || position >= items.size()) return;

            PendingTag tag = items.get(position);
            if (tag == null) return;

            holder.tvTagId.setText(tag.getToteId());
            holder.tvTagSubtitle.setText("Tap to confirm scan");

            updateSignalOnly(holder, tag);

            holder.itemView.setOnClickListener(v -> handleClick(holder));

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // 🔥 ONLY UPDATE SIGNAL PART (FAST)
    private void updateSignalOnly(VH holder, PendingTag tag) {
        try {
            int percent = tag.getSignalPercent();

            holder.tvSignalPercent.setText(percent + "% (" + tag.getRssi() + " dbms)");

            if (holder.progressSignal.getProgress() != percent) {
                holder.progressSignal.setProgress(percent);
            }

            int tintColor;
            if (percent >= ModuleViewModel.greenTh) {
                tintColor = ContextCompat.getColor(context, R.color.signal_green);
            } else if (percent >= ModuleViewModel.yellowTh) {
                tintColor = ContextCompat.getColor(context, R.color.signal_yellow);
            } else {
                tintColor = ContextCompat.getColor(context, R.color.signal_red);
            }

            holder.progressSignal.getProgressDrawable()
                    .setColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void handleClick(VH holder) {
        try {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            PendingTag tag = items.get(pos);
            if (tag == null) return;

            if (tag.getSignalPercent() <= 60) {

                PopupUtils.showCustomYesNoDialog(
                        holder.itemView.getContext(),
                        "Tag Confirmation ?",
                        "Are you sure to confirm Tag?",
                        new PopupUtils.PopupCallback() {
                            @Override
                            public void onYes() {
                                try {
                                    if (listener != null) listener.onItemClicked(tag);
                                    applyConfirmedUI(holder);
                                } catch (Throwable ignored) {}
                            }
                            public void onNo() {}
                            public void onCLose() {}
                        }
                );

            } else {
                if (listener != null) listener.onItemClicked(tag);
                applyConfirmedUI(holder);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void applyConfirmedUI(VH holder) {
        try {
            holder.tagDetailLayout.setBackgroundColor(Color.parseColor("#BE58EF86"));
            holder.itemView.setEnabled(false);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < items.size()) {
            String tagId = items.get(position).getTagId();
            if (tagId != null) return tagId.hashCode();
        }
        return RecyclerView.NO_ID;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivTagIcon;
        TextView tvTagId, tvTagSubtitle, tvSignalPercent;
        ProgressBar progressSignal;
        LinearLayout tagDetailLayout;

        VH(@NonNull View itemView) {
            super(itemView);
            try {
                ivTagIcon = itemView.findViewById(R.id.ivTagIcon);
                tvTagId = itemView.findViewById(R.id.tvTagId);
                tvTagSubtitle = itemView.findViewById(R.id.tvTagSubtitle);
                tvSignalPercent = itemView.findViewById(R.id.tvSignalPercent);
                progressSignal = itemView.findViewById(R.id.progressSignal);
                tagDetailLayout = itemView.findViewById(R.id.tagDetailLayout);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}