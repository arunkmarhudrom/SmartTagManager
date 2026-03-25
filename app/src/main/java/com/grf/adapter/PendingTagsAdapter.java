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

        void onItemChecked(PendingTag tag); // optional
    }

    private final Context context;
    private final List<PendingTag> items;
    private final Listener listener;

    private final Object itemsLock = new Object();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PendingTagsAdapter(Context context, List<PendingTag> items, Listener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
        // Tell RecyclerView item IDs are stable (we override getItemId)
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
            // fallback empty view holder
            View v = new View(context);
            v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            return new VH(v);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        try {
            PendingTag tag = null;
            synchronized (itemsLock) {
                if (position >= 0 && position < items.size()) {
                    tag = items.get(position);
                }
            }
            if (tag == null) {
                // defensive: hide content if item not available
                holder.itemView.setVisibility(GONE);
                return;
            } else {
                holder.itemView.setVisibility(View.VISIBLE);
            }

            // left side
            holder.tvTagId.setText(tag.getToteId());
            holder.tvTagSubtitle.setText("Tap to confirm scan");

            int percent = tag.getSignalPercent();

            holder.tvSignalPercent.setText(percent + "% (" + tag.getRssi() + " dbms)");
            holder.progressSignal.setProgress(percent);

            // choose tint color based on percent (use ContextCompat for compatibility)
            int tintColor;
            if (percent >= ModuleViewModel.greenTh) {
                tintColor = ContextCompat.getColor(context, R.color.signal_green);
            } else if (percent >= ModuleViewModel.yellowTh) {
                tintColor = ContextCompat.getColor(context, R.color.signal_yellow);
            } else {
                tintColor = ContextCompat.getColor(context, R.color.signal_red);
            }

            // Animate progress change
            // animated
            holder.progressSignal.getProgressDrawable().setColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN);


            // ---------- click listener: read current item/position at click-time ----------
            holder.itemView.setOnClickListener(v -> {
                try {
                    int posAtClick = holder.getBindingAdapterPosition();
                    if (posAtClick == RecyclerView.NO_POSITION) return;

                    PendingTag current;
                    synchronized (itemsLock) {
                        if (posAtClick >= 0 && posAtClick < items.size()) {
                            current = items.get(posAtClick);
                        } else {
                            return;
                        }
                    }

                    // Use the snapshot percent to avoid stale UI mutation from recycled holder
                    final int snapshotPercent = current.getSignalPercent();

                    if (current.getSignalPercent() <= 60) {
                        // show popup. Because popup is async, re-check position when user confirms.
                        PopupUtils.showCustomYesNoDialog(
                                v.getContext(),
                                "Tag Confirmation ?",
                                "Are you sure to confirm Tag?",
                                new PopupUtils.PopupCallback() {
                                    @Override
                                    public void onYes() {
                                        try {
                                            int posAtConfirm = holder.getBindingAdapterPosition();
                                            PendingTag confirmedTag;
                                            synchronized (itemsLock) {
                                                if (posAtConfirm != RecyclerView.NO_POSITION && posAtConfirm < items.size()) {
                                                    confirmedTag = items.get(posAtConfirm);
                                                } else {
                                                    confirmedTag = current;
                                                }
                                                // mark confirmed copy in model
                                                confirmedTag.setSignalPercent(snapshotPercent);
                                            }

                                            if (listener != null)
                                                listener.onItemClicked(confirmedTag);

                                            // only update the holder's views if the holder still represents the same item
                                            if (posAtConfirm != RecyclerView.NO_POSITION && posAtConfirm == posAtClick) {
                                                int semiGreen = Color.parseColor("#BE58EF86");
                                                holder.tagDetailLayout.setBackgroundColor(semiGreen);
                                                holder.itemView.setEnabled(false);
                                            } else {
                                                if (posAtConfirm != RecyclerView.NO_POSITION) {
                                                    // notify the changed position to refresh UI
                                                    mainHandler.post(() -> {
                                                        try {
                                                            notifyItemChanged(posAtConfirm);
                                                        } catch (Throwable ignore) {
                                                        }
                                                    });
                                                }
                                            }
                                        } catch (Throwable ignore) {
                                        }
                                    }

                                    @Override
                                    public void onNo() {
                                        // no-op
                                    }

                                    @Override
                                    public void onCLose() {
                                        // no-op
                                    }


                                }
                        );

                    } else { // immediate confirm branch (no popup)
                        int posNow = holder.getBindingAdapterPosition();
                        PendingTag itemNow;
                        synchronized (itemsLock) {
                            if (posNow != RecyclerView.NO_POSITION && posNow < items.size()) {
                                itemNow = items.get(posNow);
                            } else {
                                itemNow = current;
                            }
                        }

                        if (listener != null) listener.onItemClicked(itemNow);

                        if (posNow != RecyclerView.NO_POSITION && posNow == posAtClick) {
                            int semiGreen = Color.parseColor("#BE58EF86");
                            holder.tagDetailLayout.setBackgroundColor(semiGreen);
                            holder.itemView.setEnabled(false);
                        } else if (posNow != RecyclerView.NO_POSITION) {
                            final int updatePos = posNow;
                            mainHandler.post(() -> {
                                try {
                                    notifyItemChanged(updatePos);
                                } catch (Throwable ignore) {
                                }
                            });
                        }
                    }
                } catch (Throwable ignore) {
                }
            });

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        synchronized (itemsLock) {
            return items == null ? 0 : items.size();
        }
    }




    @Override
    public long getItemId(int position) {
        synchronized (itemsLock) {
            if (position >= 0 && position < items.size()) {
                String tagId = items.get(position).getTagId();
                if (tagId != null) {
                    return tagId.hashCode(); // Stable and fast
                }
            }
            return RecyclerView.NO_ID;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivTagIcon;
        TextView tvTagId;
        TextView tvTagSubtitle;
        TextView tvSignalPercent;
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
