package com.dimxlp.kfrecalculator.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.dimxlp.kfrecalculator.R;

public class AnimationUtils {

    private static final long DURATION = 250;

    /**
     * A reusable method to set up an expandable/collapsible view group with animations.
     *
     * @param headerView  The clickable header view that contains the title and chevron.
     * @param contentView The LinearLayout that will expand or collapse.
     * @param title       The title to set for the header.
     */
    public static void setupExpandableGroup(View headerView, final LinearLayout contentView, String title) {
        final TextView tvTitle = headerView.findViewById(R.id.tvGroupTitle);
        final ImageView ivChevron = headerView.findViewById(R.id.ivChevron);
        if (tvTitle != null) {
            tvTitle.setText(title);
        }

        headerView.setOnClickListener(v -> {
            boolean isVisible = contentView.getVisibility() == View.VISIBLE;
            toggleSection(contentView, ivChevron, isVisible);
        });
    }

    /**
     * Toggles a section's visibility with a smooth animation.
     *
     * @param contentView The content view to animate.
     * @param chevron     The chevron icon to rotate.
     * @param isVisible   The current visibility state of the content view.
     */
    private static void toggleSection(final LinearLayout contentView, final ImageView chevron, boolean isVisible) {
        // Animate Chevron rotation
        ObjectAnimator chevronAnimator = ObjectAnimator.ofFloat(chevron, "rotation", isVisible ? 180f : 0f, isVisible ? 0f : 180f);
        chevronAnimator.setDuration(DURATION);

        // Animate Height for smooth expand/collapse
        if (isVisible) {
            // Collapse animation
            int initialHeight = contentView.getHeight();
            ValueAnimator heightAnimator = ValueAnimator.ofInt(initialHeight, 0);
            heightAnimator.setDuration(DURATION);
            heightAnimator.addUpdateListener(animation -> {
                ViewGroup.LayoutParams params = contentView.getLayoutParams();
                params.height = (int) animation.getAnimatedValue();
                contentView.setLayoutParams(params);
            });
            heightAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    contentView.setVisibility(View.GONE);
                }
            });
            chevronAnimator.start();
            heightAnimator.start();
        } else {
            // Expand animation
            contentView.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int targetHeight = contentView.getMeasuredHeight();
            contentView.getLayoutParams().height = 0;
            contentView.setVisibility(View.VISIBLE);

            ValueAnimator heightAnimator = ValueAnimator.ofInt(0, targetHeight);
            heightAnimator.setDuration(DURATION);
            heightAnimator.addUpdateListener(animation -> {
                ViewGroup.LayoutParams params = contentView.getLayoutParams();
                params.height = (int) animation.getAnimatedValue();
                contentView.setLayoutParams(params);
            });
            heightAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // Set height to WRAP_CONTENT after animation to allow for dynamic internal content
                    ViewGroup.LayoutParams params = contentView.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    contentView.setLayoutParams(params);
                }
            });
            chevronAnimator.start();
            heightAnimator.start();
        }
    }
}