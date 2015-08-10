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

package org.catrobat.catroid.cast;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.badlogic.gdx.backends.android.surfaceview.GLSurfaceView20;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import com.google.android.gms.common.api.Status;

import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.formulaeditor.Sensors;
import org.catrobat.catroid.stage.PreStageActivity;
import org.catrobat.catroid.stage.StageActivity;

import java.util.EnumMap;

public class CastManager {

	private static final CastManager INSTANCE = new CastManager();

	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private CastDevice mSelectedDevice = null;
	private final MyMediaRouterCallback mMediaRouterCallback = new MyMediaRouterCallback();
	private TextView pausedView;

	public void setIsConnected(Boolean isConnected) {
		this.isConnected = isConnected;
	}

	private Boolean isConnected = false;

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	private Activity activity;

	public void setStageActivity(StageActivity stageActivity) {
		this.stageActivity = stageActivity;
	}

	public Boolean isConnected() {
		return isConnected;
	}

	public StageActivity getStageActivity() {
		return stageActivity;
	}

	private StageActivity stageActivity = null;
	private boolean idleScreen = false;

	public RelativeLayout getLayout() {
		return layout;
	}

	private RelativeLayout layout;
	private Application context;
	private View serviceView;
	private Boolean callbackAdded = false;
	private EnumMap<Sensors, Boolean> isGamepadButtonPressed = new EnumMap<>(Sensors.class);

	private CastManager() {
		isGamepadButtonPressed.put(Sensors.GAMEPAD_A_PRESSED, false);
		isGamepadButtonPressed.put(Sensors.GAMEPAD_B_PRESSED, false);
		isGamepadButtonPressed.put(Sensors.GAMEPAD_LEFT_PRESSED, false);
		isGamepadButtonPressed.put(Sensors.GAMEPAD_RIGHT_PRESSED, false);
		isGamepadButtonPressed.put(Sensors.GAMEPAD_UP_PRESSED, false);
		isGamepadButtonPressed.put(Sensors.GAMEPAD_DOWN_PRESSED, false);

		pausedView = new TextView(CatroidApplication.getAppContext());
		pausedView.setText("PAUSED");
	}

	public boolean isButtonPressed(Sensors btnSensor) {
		return isGamepadButtonPressed.get(btnSensor);
	}

	public void setButtonPress(Sensors btn, boolean b) {
		isGamepadButtonPressed.put(btn, b);
	}

	public static CastManager getInstance() { return INSTANCE; }

	public CastDevice getSelectedDevice() {
		return mSelectedDevice;
	}

	public void initMediaRouter(Activity activity) {

		Activity oldActivity = this.activity;
		this.activity = activity;

		if(oldActivity != null)
			return;

		mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(CastMediaControlIntent.categoryForCast(activity.getApplicationContext().getString(R.string.REMOTE_DISPLAY_APP_ID)))
				.build();

		if(isCastServiceRunning(activity))
			CastRemoteDisplayLocalService.stopService();

	}

	public void addMediaRouterCallback() {
		if (!callbackAdded) {
			mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
		}
		callbackAdded = true;
	}

	public void setIdleCastScreen() {

		if(this.layout != null && this.context != null && isCastServiceRunning(this.activity)) {
			this.layout.removeAllViews();
			Drawable drawable = ContextCompat.getDrawable(context, R.drawable.idle_screen_1);
			this.layout.setBackground(drawable);
		}
		CastManager.getInstance().setIdleScreen(false);
	}

	public void setPausedScreen() {

		if(this.layout != null && this.context != null && isCastServiceRunning(this.activity)) {
			this.layout.addView(pausedView);
		}
	}

	public void removePausedScreen() {

		if(this.layout != null && this.context != null && isCastServiceRunning(this.activity)) {
			this.layout.removeView(pausedView);
		}
	}

	public void addCastButtonActionbar(Menu menu) {

		try {
			MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
			MediaRouteActionProvider mediaRouteActionProvider =
					(MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
			mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
		}catch (Exception e) {
			Log.e("ERROR", "No media router button found", e);
		}
	}

	public boolean isCastServiceRunning(Activity activity) {
		ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (CastService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	public void startCastService(final Activity activity) {

		Intent intent = new Intent(activity, activity.getClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent notificationPendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);

		CastRemoteDisplayLocalService.NotificationSettings settings =
				new CastRemoteDisplayLocalService.NotificationSettings.Builder()
						.setNotificationPendingIntent(notificationPendingIntent).build();
		CastRemoteDisplayLocalService.startService(activity, CastService.class,
				activity.getString(R.string.REMOTE_DISPLAY_APP_ID), mSelectedDevice, settings,
				new CastRemoteDisplayLocalService.Callbacks() {
					@Override
					public void onRemoteDisplaySessionStarted(
							CastRemoteDisplayLocalService service) {

					}

					@Override
					public void onRemoteDisplaySessionError(Status errorReason) {
						int code = errorReason.getStatusCode();
						isConnected = false;
						mSelectedDevice = null;
						activity.finish();
					}
				});
	}

	private void startStage(Activity activity) {
		ProjectManager.getInstance().getCurrentProject().getDataContainer().resetAllDataObjects();
		Intent intent = new Intent(activity, PreStageActivity.class);
		activity.startActivityForResult(intent, PreStageActivity.REQUEST_RESOURCES_INIT);
	}

	public boolean isIdleScreen() {
		return idleScreen;
	}

	public void setIdleScreen(boolean idleScreen) {
		this.idleScreen = idleScreen;
	}


	public void setLayout(RelativeLayout layout) {
		this.layout = layout;
	}

	public void setContext(Application context) {
		this.context = context;
	}

	public void setServiceView(View serviceView) {
		this.serviceView = serviceView;

		if(this.layout != null && this.context != null && isCastServiceRunning(this.activity)) {

			this.layout.removeAllViews();
			this.layout.addView(this.serviceView);

			if (serviceView != null && serviceView.getClass().getName().equals(GLSurfaceView20.class.getName())) {
				GLSurfaceView20 surfaceView = (GLSurfaceView20) serviceView;
				surfaceView.surfaceChanged(surfaceView.getHolder(), 0, 1280, 720);

			}
		}
	}

	public View getServiceView() {
		return this.serviceView;
	}

	private class MyMediaRouterCallback extends MediaRouter.Callback {

		@Override
		public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
			mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
			String routeId = info.getId();

			if(mSelectedDevice != null){
				startCastService(activity);
			}
		}

		@Override
		public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
			mSelectedDevice = null;
			isConnected = false;

			if (stageActivity != null) {
				stageActivity.onBackPressed();
			}

			CastRemoteDisplayLocalService.stopService();
		}
	}
}
