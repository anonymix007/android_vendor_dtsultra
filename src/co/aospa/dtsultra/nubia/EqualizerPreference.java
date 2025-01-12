/*
 * Copyright (C) 2025 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dtsultra.nubia;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;

public class EqualizerPreference extends Preference implements Slider.OnChangeListener {

    public interface OnEqBandChangeListener {
        void onEqBandChange(int band, int gain);
        void onEqChange(int[] gains);
    }

    private final ConcurrentHashMap<Integer, Integer> mSliderIds;
    private Slider[] mSliders;
    private int[] mSliderValues;
    private final int mDefaultValue;

    private OnEqBandChangeListener mListener;

    public EqualizerPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0, 0);
        setLayoutResource(R.layout.preference_eq);
        mSliderIds = new ConcurrentHashMap<>();
        try (TypedArray ids = context.getResources().obtainTypedArray(R.array.dtsultra_eq_slider_ids)) {
            for (int i = 0; i < ids.length(); i++) {
                mSliderIds.put(ids.getResourceId(i, 0), i);
            }
        }
        mDefaultValue = (int) context.getResources().getFloat(R.dimen.dtsultra_eq_slider_default);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mSliders = new Slider[mSliderIds.size()];
        mSliderValues = new int[mSliderIds.size()];
        Arrays.fill(mSliderValues, mDefaultValue);

        MaterialCardView cardView = (MaterialCardView) Objects.requireNonNull(holder.findViewById(0));
        cardView.setOnLongClickListener(view -> {
            Arrays.fill(mSliderValues, mDefaultValue);
            if (persistString(DtsUtils.eqToString(mSliderValues))) {
                updateSliderValues();
                if (mListener != null) {
                    mListener.onEqChange(mSliderValues);
                }
            }
            return true;
        });

        for (Map.Entry<Integer, Integer> entry: mSliderIds.entrySet()) {
            int i = entry.getValue();
            mSliders[i] = (Slider) holder.findViewById(entry.getKey());
            mSliders[i].addOnChangeListener(this);
        }

        int[] eq = DtsUtils.eqFromString(getPersistedString(""));

        if (eq != null) {
            if (eq.length != mSliders.length) {
                throw new IllegalStateException("Saved EQ state has " + eq.length + " bands, expected " + mSliders.length);
            }
            mSliderValues = eq;
            updateSliderValues();
        }
    }

    public void setEqBandChangeListener(OnEqBandChangeListener listener) {
        mListener = listener;
    }

    private void updateSliderValues() {
        for (int i = 0; i < mSliders.length; i++) {
            mSliders[i].setValue(mSliderValues[i]);
        }
    }

    public int[] getEq() {
        if (mSliderValues == null) {
            return DtsUtils.eqFromString(getPersistedString(""));
        }
        return mSliderValues;
    }

    public void setEq(int[] eq) {
        mSliderValues = eq;
        updateSliderValues();
    }

    @Override
    public void onValueChange(@NonNull Slider slider, float v, boolean fromUser) {
        int band = mSliderIds.get(slider.getId());
        int gain = (int) v;
        if (!fromUser || mSliderValues[band] == gain) {
            return;
        }

        if (mListener != null) {
            mListener.onEqBandChange(band, gain);
        }

        mSliderValues[band] = gain;
        persistString(DtsUtils.eqToString(mSliderValues));
    }
}
