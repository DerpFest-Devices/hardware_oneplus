/*
 * Copyright (C) 2018-2021 crDroid Android Project
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

package org.derp.device.DeviceExtras;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.FileObserver;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;

import com.android.internal.os.DeviceKeyHandler;

import java.util.Arrays;

import org.derp.device.DeviceExtras.Constants;
import org.derp.device.DeviceExtras.SliderControllerBase;
import org.derp.device.DeviceExtras.slider.NotificationController;
import org.derp.device.DeviceExtras.slider.FlashlightController;
import org.derp.device.DeviceExtras.slider.BrightnessController;
import org.derp.device.DeviceExtras.slider.RotationController;
import org.derp.device.DeviceExtras.slider.RingerController;
import org.derp.device.DeviceExtras.slider.NotificationRingerController;

import vendor.oneplus.hardware.camera.V1_0.IOnePlusCameraProvider;

public class KeyHandler implements DeviceKeyHandler {
    private static final String TAG = KeyHandler.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final String CLIENT_PACKAGE_NAME = "com.oneplus.camera";
    public static final String CLIENT_PACKAGE_PATH = "/data/misc/aosp/client_package_name";

    private final Context mContext;
    private ClientPackageNameObserver mClientObserver;
    private IOnePlusCameraProvider mProvider;
    private final NotificationController mNotificationController;
    private final FlashlightController mFlashlightController;
    private final BrightnessController mBrightnessController;
    private final RotationController mRotationController;
    private final RingerController mRingerController;
    private final NotificationRingerController mNotificationRingerController;

    private SliderControllerBase mSliderController;

    // Vibration effects
    private Vibrator mVibrator;
    private static final VibrationEffect MODE_VIBRATION_EFFECT =
            VibrationEffect.get(VibrationEffect.EFFECT_DOUBLE_CLICK);

    private final BroadcastReceiver mSliderUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int usage = intent.getIntExtra(Constants.EXTRA_SLIDER_USAGE, 0);
            int[] actions = intent.getIntArrayExtra(Constants.EXTRA_SLIDER_ACTIONS);

            Log.d(TAG, "update usage " + usage + " with actions " +
                    Arrays.toString(actions));

            if (mSliderController != null) {
                mSliderController.reset();
            }

            switch (usage) {
                case NotificationController.ID:
                    mSliderController = mNotificationController;
                    mSliderController.update(actions);
                    break;
                case FlashlightController.ID:
                    mSliderController = mFlashlightController;
                    mSliderController.update(actions);
                    break;
                case BrightnessController.ID:
                    mSliderController = mBrightnessController;
                    mSliderController.update(actions);
                    break;
                case RotationController.ID:
                    mSliderController = mRotationController;
                    mSliderController.update(actions);
                    break;
                case RingerController.ID:
                    mSliderController = mRingerController;
                    mSliderController.update(actions);
                    break;
                case NotificationRingerController.ID:
                    mSliderController = mNotificationRingerController;
                    mSliderController.update(actions);
                    break;
            }

            mSliderController.restoreState();
        }
    };

    private BroadcastReceiver mSystemStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                onDisplayOn();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                onDisplayOff();
            }
        }
    };

    public KeyHandler(Context context) {
        mContext = context;

        mNotificationController = new NotificationController(mContext);
        mFlashlightController = new FlashlightController(mContext);
        mBrightnessController = new BrightnessController(mContext);
        mRotationController = new RotationController(mContext);
        mRingerController = new RingerController(mContext);
        mNotificationRingerController = new NotificationRingerController(mContext);

        mContext.registerReceiver(mSliderUpdateReceiver,
                new IntentFilter(Constants.ACTION_UPDATE_SLIDER_SETTINGS));

        mVibrator = mContext.getSystemService(Vibrator.class);

        if (PackageUtils.isAvailableApp(CLIENT_PACKAGE_NAME, mContext)) {
            IntentFilter systemStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            systemStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
            mContext.registerReceiver(mSystemStateReceiver, systemStateFilter);
            mClientObserver = new ClientPackageNameObserver(CLIENT_PACKAGE_PATH);
            mClientObserver.startWatching();
        }
    }

    private boolean hasSetupCompleted() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE, 0) != 0;
    }

    public KeyEvent handleKeyEvent(KeyEvent event) {
        int scanCode = event.getScanCode();
        boolean isSliderControllerSupported = mSliderController != null &&
                mSliderController.isSupported(scanCode);
        if (!isSliderControllerSupported) {
            return event;
        }

        if (!hasSetupCompleted()) {
            return event;
        }

        // We only want ACTION_UP event
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return null;
        }

        mSliderController.processEvent(mContext, scanCode);

        doHapticFeedback(MODE_VIBRATION_EFFECT);

        return null;
    }

    private void doHapticFeedback(VibrationEffect effect) {
        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(effect);
        }
    }

    private void onDisplayOn() {
        if (mClientObserver == null) {
            mClientObserver = new ClientPackageNameObserver(CLIENT_PACKAGE_PATH);
            mClientObserver.startWatching();
        }
    }

    private void onDisplayOff() {
        if (mClientObserver != null) {
            mClientObserver.stopWatching();
            mClientObserver = null;
        }
    }

    private class ClientPackageNameObserver extends FileObserver {

        public ClientPackageNameObserver(String file) {
            super(CLIENT_PACKAGE_PATH, MODIFY);
        }

        @Override
        public void onEvent(int event, String file) {
            String pkgName = FileUtils.getFileValue(CLIENT_PACKAGE_PATH, "0");
            if (event == FileObserver.MODIFY) {
                try {
                    Log.d(TAG, "client_package " + file + " and " + pkgName);
                    mProvider = IOnePlusCameraProvider.getService();
                    mProvider.setPackageName(pkgName);
                } catch (RemoteException e) {
                    Log.e(TAG, "setPackageName error", e);
                }
            }
        }
    }
}
