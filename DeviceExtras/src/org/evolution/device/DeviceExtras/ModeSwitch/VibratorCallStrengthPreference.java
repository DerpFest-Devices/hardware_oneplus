/*
* Copyright (C) 2016 The OmniROM Project
* Copyright (C) 2021 The Evolution X Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.com/licenses/>.
*
*/
package org.evolution.device.DeviceExtras;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;

import java.util.List;

public class VibratorCallStrengthPreference extends CustomSeekBarPreference {

    private static int mMinVal = 116;
    private static int mMaxVal = 2088;
    private static int mDefVal = 500;
    private Vibrator mVibrator;

    private static final String FILE_LEVEL = "/sys/devices/platform/soc/c440000.qcom,spmi/spmi-0/spmi0-03/c440000.qcom,spmi:qcom,pmi8998@3:qcom,haptics@c000/leds/vibrator/vmax_mv_call";
    private static final long testVibrationPattern[] = {0,250};

    public VibratorCallStrengthPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mInterval = 100;
        mShowSign = false;
        mUnits = "";
        mContinuousUpdates = false;
        mMinValue = mMinVal;
        mMaxValue = mMaxVal;
        mDefaultValueExists = true;
        mDefaultValue = mDefVal;
        mValue = Integer.parseInt(loadValue());

        setPersistent(false);

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static boolean isSupported() {
        return FileUtils.fileWritable(FILE_LEVEL);
    }

    public static void restore(Context context) {
        if (!isSupported()) {
            return;
        }

        String storedValue = PreferenceManager.getDefaultSharedPreferences(context).getString(DeviceExtras.KEY_CALL_VIBSTRENGTH , String.valueOf(mDefVal));
        FileUtils.writeValue(FILE_LEVEL, storedValue);
    }

    public static String loadValue() {
        return FileUtils.getFileValue(FILE_LEVEL, String.valueOf(mDefVal));
    }

    private void saveValue(String newValue) {
        FileUtils.writeValue(FILE_LEVEL, newValue);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editor.putString(DeviceExtras.KEY_CALL_VIBSTRENGTH, newValue);
        editor.apply();
        mVibrator.vibrate(testVibrationPattern, -1);
    }

    @Override
    protected void changeValue(int newValue) {
        saveValue(String.valueOf(newValue));
    }
}
