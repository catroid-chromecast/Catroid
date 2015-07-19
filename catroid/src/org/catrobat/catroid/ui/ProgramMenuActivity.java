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
package org.catrobat.catroid.ui;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import com.google.android.gms.common.api.Status;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.drone.DroneInitializer;
import org.catrobat.catroid.stage.PreStageActivity;
import org.catrobat.catroid.stage.StageActivity;

import java.util.concurrent.locks.Lock;

public class ProgramMenuActivity extends BaseActivity {

	public static final String FORWARD_TO_SCRIPT_ACTIVITY = "forwardToScriptActivity";
	private static final String TAG = ProgramMenuActivity.class.getSimpleName();
	private Lock viewSwitchLock = new ViewSwitchLock();

	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private CastMediaRouterButtonView mMediaRouterButtonView;
	private MediaRouteButton mMediaRouteButton;
	private MyMediaRouterCallback mMediaRouterCallback = new MyMediaRouterCallback()		;
	private CastDevice mSelectedDevice;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle bundle = getIntent().getExtras();
		if (bundle != null && bundle.containsKey(FORWARD_TO_SCRIPT_ACTIVITY)) {
			Intent intent = new Intent(this, ScriptActivity.class);
			intent.putExtra(ScriptActivity.EXTRA_FRAGMENT_POSITION, bundle.getInt(FORWARD_TO_SCRIPT_ACTIVITY));
			startActivity(intent);
		}

		setContentView(R.layout.activity_program_menu);

		BottomBar.hideAddButton(this);

		mMediaRouter = MediaRouter.getInstance(getApplicationContext());
		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(CastMediaControlIntent.categoryForCast(getString(R.string.REMOTE_DISPLAY_APP_ID)))
						.build();

		mMediaRouterButtonView = (CastMediaRouterButtonView) findViewById(R.id.media_route_button_view);
		if (mMediaRouterButtonView != null) {
			mMediaRouteButton = mMediaRouterButtonView.getMediaRouteButton();
			mMediaRouteButton.setRouteSelector(mMediaRouteSelector);
		}

		// TODO
		//if(Cc project)
		//{
		BottomBar.hidePlayButton(this);
		BottomBar.showCastButton(this);
		//}
		//else
		//BottomBar.showPlayButton(this);
		//BottomBar.hideCastButton(this);

		final ActionBar actionBar = getSupportActionBar();

		//The try-catch block is a fix for this bug: https://github.com/Catrobat/Catroid/issues/618
		try {
			String title = ProjectManager.getInstance().getCurrentSprite().getName();
			actionBar.setTitle(title);
			actionBar.setHomeButtonEnabled(true);
		} catch (NullPointerException nullPointerException) {
			Log.e(TAG, "onCreate: NPE -> finishing", nullPointerException);
			finish();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (isCastServiceRunning(CastService.class))
			CastRemoteDisplayLocalService.stopService();

		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (ProjectManager.getInstance().getCurrentSpritePosition() == 0) {
			((Button) findViewById(R.id.program_menu_button_looks)).setText(R.string.backgrounds);
		} else {
			((Button) findViewById(R.id.program_menu_button_looks)).setText(R.string.looks);
		}
	}

	@Override
	protected void onStop() {
		mMediaRouter.removeCallback(mMediaRouterCallback);
		super.onStop();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PreStageActivity.REQUEST_RESOURCES_INIT && resultCode == RESULT_OK) {
			Intent intent = new Intent(ProgramMenuActivity.this, StageActivity.class);
			DroneInitializer.addDroneSupportExtraToNewIntentIfPresentInOldIntent(data, intent);
			startActivity(intent);
		}
	}

	public void handleScriptsButton(View view) {
		if (!viewSwitchLock.tryLock()) {
			return;
		}
		startScriptActivity(ScriptActivity.FRAGMENT_SCRIPTS);
	}

	public void handleLooksButton(View view) {
		if (!viewSwitchLock.tryLock()) {
			return;
		}
		startScriptActivity(ScriptActivity.FRAGMENT_LOOKS);
	}

	public void handleSoundsButton(View view) {
		if (!viewSwitchLock.tryLock()) {
			return;
		}
		startScriptActivity(ScriptActivity.FRAGMENT_SOUNDS);
	}

	public void handlePlayButton(View view) {
		if (!viewSwitchLock.tryLock()) {
			return;
		}
		ProjectManager.getInstance().getCurrentProject().getDataContainer().resetAllDataObjects();
		Intent intent = new Intent(this, PreStageActivity.class);
		startActivityForResult(intent, PreStageActivity.REQUEST_RESOURCES_INIT);
	}

	private void startScriptActivity(int fragmentPosition) {
		Intent intent = new Intent(this, ScriptActivity.class);
		intent.putExtra(ScriptActivity.EXTRA_FRAGMENT_POSITION, fragmentPosition);
		startActivity(intent);
	}

	private boolean isCastServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void startCastService() {
		Intent intent = new Intent(ProgramMenuActivity.this,ProjectActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent notificationPendingIntent = PendingIntent.getActivity(ProgramMenuActivity.this, 0, intent, 0);

		CastRemoteDisplayLocalService.NotificationSettings settings =
				new CastRemoteDisplayLocalService.NotificationSettings.Builder()
						.setNotificationPendingIntent(notificationPendingIntent).build();
		CastRemoteDisplayLocalService.startService(ProgramMenuActivity.this, CastService.class,
				getString(R.string.REMOTE_DISPLAY_APP_ID), mSelectedDevice, settings,
				new CastRemoteDisplayLocalService.Callbacks() {
					@Override
					public void onRemoteDisplaySessionStarted(
							CastRemoteDisplayLocalService service) {
					}

					@Override
					public void onRemoteDisplaySessionError(Status errorReason) {
						int code = errorReason.getStatusCode();
						//initError();

						mSelectedDevice = null;
						ProgramMenuActivity.this.finish();
					}
				});
	}

	private void startStage() {
		ProjectManager.getInstance().getCurrentProject().getDataContainer().resetAllDataObjects();
		Intent intent = new Intent(this, PreStageActivity.class);
		startActivityForResult(intent, PreStageActivity.REQUEST_RESOURCES_INIT);
	}

	private class MyMediaRouterCallback extends MediaRouter.Callback {

		@Override
		public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
			mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
			String routeId = info.getId();

			if(mSelectedDevice != null){
				startCastService();
				startStage();
			}
		}

		@Override
		public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
			mSelectedDevice = null;
			CastRemoteDisplayLocalService.stopService();
		}
	}
}
