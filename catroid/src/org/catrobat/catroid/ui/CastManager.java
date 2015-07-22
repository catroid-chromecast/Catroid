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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import com.google.android.gms.common.api.Status;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.stage.PreStageActivity;

public class CastManager {

	private static final CastManager INSTANCE = new CastManager();

	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private CastDevice mSelectedDevice;
	private final MyMediaRouterCallback mMediaRouterCallback = new MyMediaRouterCallback();
	private Activity activity;
	private boolean idleScreen = false;
	private RelativeLayout layout;
	private Application context;
	private View view;

	private CastManager() {
	}

	public static CastManager getInstance() { return INSTANCE; }

	public CastDevice getSelectedDevice() {
		return mSelectedDevice;
	}

	public void initMediaRouter(Activity activity) {

		this.activity = activity;

		mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(CastMediaControlIntent.categoryForCast(activity.getApplicationContext().getString(R.string.REMOTE_DISPLAY_APP_ID)))
				.build();

		if(isCastServiceRunning(activity))
			CastRemoteDisplayLocalService.stopService();

		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
	}

	public void setIdleCastSreen() {

		if(this.layout != null && this.context != null && isCastServiceRunning(this.activity)) {

			this.layout.removeAllViews();
			ImageView imageView = new ImageView(this.context);
			imageView.setImageDrawable(this.context.getDrawable(R.drawable.cast_screensaver));
			this.layout.addView(imageView);
		}
		CastManager.getInstance().setIdleScreen(false);
	}

	public void addCastButtonActionbar(Menu menu) {
		MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
		MediaRouteActionProvider mediaRouteActionProvider =
				(MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
		mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
	}

	public void stopCallbacks(Activity activity){
		if(isCastServiceRunning(activity))
			CastRemoteDisplayLocalService.stopService();

		mMediaRouter.removeCallback(mMediaRouterCallback);
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

	public void setView(View view) {
		this.view = view;

		if(this.layout != null && this.context != null && isCastServiceRunning(this.activity)) {

			this.layout.removeAllViews();
			this.layout.addView(this.view);
		}
	}

	public View getView() {
		return this.view;
	}

	private class MyMediaRouterCallback extends MediaRouter.Callback {

		@Override
		public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
			mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
			String routeId = info.getId();

			if(mSelectedDevice != null){
				startCastService(activity);
				//startStage(activity);
			}
		}

		@Override
		public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
			mSelectedDevice = null;
			CastRemoteDisplayLocalService.stopService();
		}
	}
}
