/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.settings.device;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;

import java.lang.Math;
import java.text.DecimalFormat;

/**
 * Special preference type that allows configuration of vibrator intensity settings on Samsung devices
 */
public class VibratorTuningPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "GalaxyS2Settings_Vibrator";

    private static String FILE_PATH = null;
    private static int MAX_VALUE;
    private static int WARNING_THRESHOLD;
    private static int DEFAULT_VALUE;
    private static int MIN_VALUE;

    private Context mContext;
    private SeekBar mSeekBar;
    private TextView mValue;
    private TextView mWarning;
    private String mOriginalValue;
    private Drawable mProgressDrawable;
    private Drawable mProgressThumb;
    private LightingColorFilter mRedFilter;

    public VibratorTuningPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        FILE_PATH = context.getResources().getString(R.string.vibrator_sysfs_file);
        MAX_VALUE = Integer.valueOf(context.getResources().getString(R.string.intensity_max_value));
        WARNING_THRESHOLD = Integer.valueOf(context.getResources().getString(R.string.intensity_warning_threshold));
        DEFAULT_VALUE = Integer.valueOf(context.getResources().getString(R.string.intensity_default_value));
        MIN_VALUE = Integer.valueOf(context.getResources().getString(R.string.intensity_min_value));

        setDialogLayoutResource(R.layout.preference_dialog_vibrator_tuning);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNeutralButton(R.string.defaults_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = (SeekBar) view.findViewById(R.id.vibrator_seekbar);
        mValue = (TextView) view.findViewById(R.id.vibrator_value);
        mWarning = (TextView) view.findViewById(R.id.textWarn);

        String strWarnMsg = getContext().getResources().getString(R.string.vibrator_warning, strengthToPercent(WARNING_THRESHOLD));
        mWarning.setText(strWarnMsg);

        Drawable progressDrawable = mSeekBar.getProgressDrawable();
        if (progressDrawable instanceof LayerDrawable) {
            LayerDrawable ld = (LayerDrawable) progressDrawable;
            mProgressDrawable = ld.findDrawableByLayerId(android.R.id.progress);
        }
        mProgressThumb = mSeekBar.getThumb();
        mRedFilter = new LightingColorFilter(Color.BLACK,
                getContext().getResources().getColor(android.R.color.holo_red_light));

        // Read the current value from sysfs in case user wants to dismiss his changes
        mOriginalValue = Utils.readOneLine(FILE_PATH);

        // Restore percent value from SharedPreferences object
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        int percent = settings.getInt("percent", strengthToPercent(DEFAULT_VALUE));

        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setProgress(Integer.valueOf(percent));
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        AlertDialog d = (AlertDialog) getDialog();
        Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
        defaultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSeekBar.setProgress(strengthToPercent(DEFAULT_VALUE));
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            // Store percent value in SharedPreferences object
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("percent", mSeekBar.getProgress());
            editor.commit();
        } else {
            Utils.writeValue(FILE_PATH, String.valueOf(mOriginalValue));
        }
    }

    public static void restore(Context context) {
        FILE_PATH = context.getResources().getString(R.string.vibrator_sysfs_file);

        if (!isSupported(FILE_PATH)) {
            return;
        }

        MAX_VALUE = Integer.valueOf(context.getResources().getString(R.string.intensity_max_value));
        DEFAULT_VALUE = Integer.valueOf(context.getResources().getString(R.string.intensity_default_value));
        MIN_VALUE = Integer.valueOf(context.getResources().getString(R.string.intensity_min_value));

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int strength = percentToStrength(settings.getInt("percent", strengthToPercent(DEFAULT_VALUE)));

        Log.d(TAG, "Restoring vibration setting: " + strength);
        Utils.writeValue(FILE_PATH, String.valueOf(strength));
    }

    public static boolean isSupported(String filePath) {
        return Utils.fileExists(filePath);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        boolean shouldWarn = progress >= strengthToPercent(WARNING_THRESHOLD);
        if (mProgressDrawable != null) {
            mProgressDrawable.setColorFilter(shouldWarn ? mRedFilter : null);
        }
        if (mProgressThumb != null) {
            mProgressThumb.setColorFilter(shouldWarn ? mRedFilter : null);
        }
        Utils.writeValue(FILE_PATH, String.valueOf(percentToStrength(progress)));
        mValue.setText(String.format("%d%%", progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Do nothing
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vib.vibrate(200);
    }

    /**
    * Convert vibrator strength to percent
    */
    public static int strengthToPercent(int strength) {
        double maxValue = MAX_VALUE;
        double minValue = MIN_VALUE;

        double percent = (strength - minValue) * (100 / (maxValue - minValue));

        if (percent > 100)
            percent = 100;
        else if (percent < 0)
            percent = 0;

        return (int) percent;
    }

    /**
    * Convert percent to vibrator strength
    */
    public static int percentToStrength(int percent) {
        int strength = Math.round((((MAX_VALUE - MIN_VALUE) * percent) / 100) + MIN_VALUE);

        if (strength > MAX_VALUE)
            strength = MAX_VALUE;
        else if (strength < MIN_VALUE)
            strength = MIN_VALUE;

        return strength;
    }
}
