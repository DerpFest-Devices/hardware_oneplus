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
package org.derp.device.DeviceExtras;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;

public class VibratorStrengthPreference extends CustomSeekBarPreference {

    private static int mDefVal;
    private Vibrator mVibrator;

    private static final int NODE_LEVEL = R.string.node_vibrator_strength_preference;
    private static long testVibrationPattern[];

    public VibratorStrengthPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mInterval = context.getResources().getInteger(R.integer.vibrator_strength_preference_interval);
        mShowSign = false;
        mUnits = "";
        mContinuousUpdates = false;

        int[] mAllValues = context.getResources().getIntArray(R.array.vibrator_strength_preference_array);
        mMinValue = mAllValues[1];
        mMaxValue = mAllValues[2];
        mDefaultValueExists = true;
        mDefVal = mAllValues[0];
        mDefaultValue = mDefVal;
        mValue = Integer.parseInt(loadValue(context));

        int[] tempVibrationPattern = context.getResources().getIntArray(R.array.test_vibration_pattern);
        testVibrationPattern = new long[tempVibrationPattern.length];
        for (int i = 0; i < tempVibrationPattern.length; i++) {
            testVibrationPattern[i] = tempVibrationPattern[i];
        }

        setPersistent(false);

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private static String getFile(Context context) {
        String file = context.getString(NODE_LEVEL);
        if (FileUtils.fileWritable(file)) {
            return file;
        }
        return null;
    }

    public static boolean isSupported(Context context) {
        return FileUtils.fileWritable(getFile(context));
    }

    public static void restore(Context context) {
        if (!isSupported(context)) {
            return;
        }

        String storedValue = PreferenceManager.getDefaultSharedPreferences(context).getString(DeviceExtras.KEY_VIBSTRENGTH, String.valueOf(mDefVal));
        FileUtils.writeValue(getFile(context), storedValue);
    }

    public static String loadValue(Context context) {
        return FileUtils.getFileValue(getFile(context), String.valueOf(mDefVal));
    }

    private void saveValue(String newValue) {
        FileUtils.writeValue(getFile(getContext()), newValue);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editor.putString(DeviceExtras.KEY_VIBSTRENGTH, newValue);
        editor.apply();
        mVibrator.vibrate(testVibrationPattern, -1);
    }

    @Override
    protected void changeValue(int newValue) {
        saveValue(String.valueOf(newValue));
    }
}
