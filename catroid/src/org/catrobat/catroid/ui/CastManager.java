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
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import com.google.android.gms.common.api.Status;

import org.catrobat.catroid.R;

public class CastManager {

	private MediaRouteButton mMediaRouteButton;
	private CastMediaRouterButtonView mMediaRouterButtonView;
	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private CastDevice mSelectedDevice;
	private final MyMediaRouterCallback mMediaRouterCallback = new MyMediaRouterCallback();

	public void initMediaRouter(Activity activity){
		mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(CastMediaControlIntent.categoryForCast(activity.getApplicationContext().getString(R.string.REMOTE_DISPLAY_APP_ID)))
				.build();

		mMediaRouterButtonView = (CastMediaRouterButtonView) activity.findViewById(R.id.media_route_button_view);
		if (mMediaRouterButtonView != null) {
			mMediaRouteButton = mMediaRouterButtonView.getMediaRouteButton();
			mMediaRouteButton.setRouteSelector(mMediaRouteSelector);
		}

		if(isCastServiceRunning(CastService.class, activity))
			CastRemoteDisplayLocalService.stopService();

		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
	}

	private void stopCallbacks(Activity activity){
		if(isCastServiceRunning(CastService.class, activity))
			CastRemoteDisplayLocalService.stopService();

		mMediaRouter.removeCallback(mMediaRouterCallback);
	}

	private boolean isCastServiceRunning(Class<?> serviceClass, Activity activity) {
		ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void startCastService(final Activity activity) {
		Intent intent = new Intent(activity ,ProjectActivity.class);
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
						//initError();

						mSelectedDevice = null;
						activity.finish();
					}
				});
	}

	private class MyMediaRouterCallback extends MediaRouter.Callback {

		@Override
		public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
			mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
			String routeId = info.getId();
		}

		@Override
		public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
			mSelectedDevice = null;
			CastRemoteDisplayLocalService.stopService();
		}
	}
}
