/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2015 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.stage;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.cast.CastManager;
import org.catrobat.catroid.common.CatroidService;
import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.common.ServiceProvider;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.drone.DroneInitializer;
import org.catrobat.catroid.facedetection.FaceDetectionHandler;
import org.catrobat.catroid.formulaeditor.SensorHandler;
import org.catrobat.catroid.io.StageAudioFocus;
import org.catrobat.catroid.ui.dialogs.StageDialog;
import org.catrobat.catroid.utils.LedUtil;
import org.catrobat.catroid.utils.ToastUtil;
import org.catrobat.catroid.utils.VibratorUtil;

public class StageActivity extends AndroidApplication {
	public static final String TAG = StageActivity.class.getSimpleName();
	public static StageListener stageListener;
	private boolean resizePossible;
	private StageDialog stageDialog;

	private DroneConnection droneConnection = null;

	public static final int STAGE_ACTIVITY_FINISH = 7777;

	private StageAudioFocus stageAudioFocus;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		CastManager.getInstance().setStageActivity(this);

		int virtualScreenWidth = ProjectManager.getInstance().getCurrentProject().getXmlHeader().virtualScreenWidth;
		int virtualScreenHeight = ProjectManager.getInstance().getCurrentProject().getXmlHeader().virtualScreenHeight;
		if (virtualScreenHeight > virtualScreenWidth) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (getIntent().getBooleanExtra(DroneInitializer.INIT_DRONE_STRING_EXTRA, false)) {
			droneConnection = new DroneConnection(this);
		}
		stageListener = new StageListener();
		stageDialog = new StageDialog(this, stageListener, R.style.stage_dialog);
		calculateScreenSizes();

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

		Project project = ProjectManager.getInstance().getCurrentProject();
		if(project != null && project.isCastProject()) {
			CastManager.getInstance().setView(initializeForView(stageListener, config));

			setFullScreen();
			setContentView(R.layout.activity_stage_gamepad);
		}
		else {
			initialize(stageListener, config);
		}

		if (droneConnection != null) {
			try {
				droneConnection.initialise();
			} catch (RuntimeException runtimeException) {
				Log.e(TAG, "Failure during drone service startup", runtimeException);
				ToastUtil.showError(this, R.string.error_no_drone_connected);
				this.finish();
			}
		}

		ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE).initialise();

		stageAudioFocus = new StageAudioFocus(this);
	}

	private void setFullScreen() {
		// Set the IMMERSIVE flag.
		// Set the content to appear under the system bars so that the content
		// doesn't resize when the system bars hide and show.
		View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
						| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
						| View.SYSTEM_UI_FLAG_IMMERSIVE);
	}

	@Override
	public void onBackPressed() {
		pause();
		stageDialog.show();
	}

	public void manageLoadAndFinish() {
		stageListener.pause();
		stageListener.finish();

		PreStageActivity.shutdownResources();
	}

	@Override
	public void onPause() {
		SensorHandler.stopSensorListeners();
		stageListener.activityPause();
		stageAudioFocus.releaseAudioFocus();
		LedUtil.pauseLed();
		VibratorUtil.pauseVibrator();
		super.onPause();

		if (droneConnection != null) {
			droneConnection.pause();
		}

		ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE).pause();
	}

	@Override
	public void onResume() {
		SensorHandler.startSensorListener(this);
		stageListener.activityResume();
		stageAudioFocus.requestAudioFocus();
		LedUtil.resumeLed();
		VibratorUtil.resumeVibrator();
		super.onResume();

		if (droneConnection != null) {
			droneConnection.start();
		}

		ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE).start();
	}

	public void pause() {
		SensorHandler.stopSensorListeners();
		stageListener.menuPause();
		LedUtil.pauseLed();
		VibratorUtil.pauseVibrator();
		FaceDetectionHandler.pauseFaceDetection();

		ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE).pause();
	}

	public void resume() {
		stageListener.menuResume();
		LedUtil.resumeLed();
		VibratorUtil.resumeVibrator();
		SensorHandler.startSensorListener(this);
		FaceDetectionHandler.startFaceDetection(this);

		ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE).start();
	}

	public boolean getResizePossible() {
		return resizePossible;
	}

	private void calculateScreenSizes() {
		int virtualScreenWidth = ProjectManager.getInstance().getCurrentProject().getXmlHeader().virtualScreenWidth;
		int virtualScreenHeight = ProjectManager.getInstance().getCurrentProject().getXmlHeader().virtualScreenHeight;
		if (virtualScreenHeight > virtualScreenWidth) {
			ifLandscapeSwitchWidthAndHeight();
		} else {
			ifPortraitSwitchWidthAndHeight();
		}
		float aspectRatio = (float) virtualScreenWidth / (float) virtualScreenHeight;
		float screenAspectRatio = ScreenValues.getAspectRatio();

		if ((virtualScreenWidth == ScreenValues.SCREEN_WIDTH && virtualScreenHeight == ScreenValues.SCREEN_HEIGHT)
				|| Float.compare(screenAspectRatio, aspectRatio) == 0) {
			resizePossible = false;
			stageListener.maximizeViewPortWidth = ScreenValues.SCREEN_WIDTH;
			stageListener.maximizeViewPortHeight = ScreenValues.SCREEN_HEIGHT;
			return;
		}

		resizePossible = true;

		float scale = 1f;
		float ratioHeight = (float) ScreenValues.SCREEN_HEIGHT / (float) virtualScreenHeight;
		float ratioWidth = (float) ScreenValues.SCREEN_WIDTH / (float) virtualScreenWidth;

		if (aspectRatio < screenAspectRatio) {
			scale = ratioHeight / ratioWidth;
			stageListener.maximizeViewPortWidth = (int) (ScreenValues.SCREEN_WIDTH * scale);
			stageListener.maximizeViewPortX = (int) ((ScreenValues.SCREEN_WIDTH - stageListener.maximizeViewPortWidth) / 2f);
			stageListener.maximizeViewPortHeight = ScreenValues.SCREEN_HEIGHT;

		} else if (aspectRatio > screenAspectRatio) {
			scale = ratioWidth / ratioHeight;
			stageListener.maximizeViewPortHeight = (int) (ScreenValues.SCREEN_HEIGHT * scale);
			stageListener.maximizeViewPortY = (int) ((ScreenValues.SCREEN_HEIGHT - stageListener.maximizeViewPortHeight) / 2f);
			stageListener.maximizeViewPortWidth = ScreenValues.SCREEN_WIDTH;
		}
	}

	private void ifLandscapeSwitchWidthAndHeight() {
		if (ScreenValues.SCREEN_WIDTH > ScreenValues.SCREEN_HEIGHT) {
			int tmp = ScreenValues.SCREEN_HEIGHT;
			ScreenValues.SCREEN_HEIGHT = ScreenValues.SCREEN_WIDTH;
			ScreenValues.SCREEN_WIDTH = tmp;
		}
	}

	private void ifPortraitSwitchWidthAndHeight() {
		if (ScreenValues.SCREEN_WIDTH < ScreenValues.SCREEN_HEIGHT) {
			int tmp = ScreenValues.SCREEN_HEIGHT;
			ScreenValues.SCREEN_HEIGHT = ScreenValues.SCREEN_WIDTH;
			ScreenValues.SCREEN_WIDTH = tmp;
		}
	}

	@Override
	protected void onDestroy() {
		if (droneConnection != null) {
			droneConnection.destroy();
		}
		CastManager.getInstance().setStageActivity(null);
		ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE).destroy();

		Log.d(TAG, "Destroy");
		LedUtil.destroy();
		VibratorUtil.destroy();
		super.onDestroy();
	}

	@Override
	public ApplicationListener getApplicationListener() {
		return stageListener;
	}

	@Override
	public void log(String tag, String message, Throwable exception) {
		Log.d(tag, message, exception);
	}

	@Override
	public int getLogLevel() {
		return 0;
	}

	public void onPauseButtonPressed(View view) {
		view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
		onBackPressed();
	}

	public void handleGamepadButton(View view) {

		ImageButton button = (ImageButton) view;
		view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

		switch (button.getId())
		{
			case R.id.gamepadButtonA:
				stageListener.gamepadPressed(getString(R.string.cast_gamepad_A));
				break;
			case R.id.gamepadButtonB:
				stageListener.gamepadPressed(getString(R.string.cast_gamepad_B));
				break;
			case R.id.gamepadButtonUp:
				stageListener.gamepadPressed(getString(R.string.cast_gamepad_up));
				break;
			case R.id.gamepadButtonDown:
				stageListener.gamepadPressed(getString(R.string.cast_gamepad_down));
				break;
			case R.id.gamepadButtonLeft:
				stageListener.gamepadPressed(getString(R.string.cast_gamepad_left));
				break;
			case R.id.gamepadButtonRight:
				stageListener.gamepadPressed(getString(R.string.cast_gamepad_right));
				break;
		}
		setFullScreen();
	}
}
