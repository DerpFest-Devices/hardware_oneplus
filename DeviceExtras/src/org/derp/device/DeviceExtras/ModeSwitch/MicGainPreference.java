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

import java.util.List;

public class MicGainPreference extends CustomSeekBarPreference {

    private static int mDefVal;

    private static final int NODE_LEVEL = R.string.node_mic_gain_preference;

    public MicGainPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mInterval = context.getResources().getInteger(R.integer.mic_gain_preference_interval);
        mShowSign = false;
        mUnits = "";
        mContinuousUpdates = false;
        int[] mAllValues = context.getResources().getIntArray(R.array.mic_gain_preference_array);
        mMinValue = mAllValues[1];
        mMaxValue = mAllValues[2];
        mDefaultValueExists = true;
        mDefVal = mAllValues[0];
        mDefaultValue = mDefVal;
        mValue = Integer.parseInt(loadValue(context));

        setPersistent(false);
    }

    private static String getFile(Context context) {
        String file = context.getResources().getString(NODE_LEVEL);
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

        String storedValue = PreferenceManager.getDefaultSharedPreferences(context).getString(DeviceExtras.KEY_MIC_GAIN, String.valueOf(mDefVal));
        FileUtils.writeValue(getFile(context), storedValue);
    }

    public static String loadValue(Context context) {
        return FileUtils.getFileValue(getFile(context), String.valueOf(mDefVal));
    }

    private void saveValue(String newValue) {
        FileUtils.writeValue(getFile(getContext()), newValue);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editor.putString(DeviceExtras.KEY_MIC_GAIN, newValue);
        editor.apply();
    }

    @Override
    protected void changeValue(int newValue) {
        saveValue(String.valueOf(newValue));
    }
}
