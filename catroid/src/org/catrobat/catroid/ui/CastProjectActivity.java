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

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import com.google.android.gms.common.api.Status;

import org.catrobat.catroid.R;
import org.catrobat.catroid.ui.cast.PresentationService;
import org.catrobat.catroid.ui.cast.SlidingTabsBasicFragment;

/**
 * Created by davidwittenbrink on 08.07.15.
 */


public class CastProjectActivity extends ProjectActivity {

	public static final String TAG = "CastProjectActivity";
	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private CastDevice mSelectedDevice;
	private final MyMediaRouterCallback mMediaRouterCallback = new MyMediaRouterCallback();
	private final String REMOTE_DISPLAY_APP_ID = "CEBB9229";
	private boolean isChromecastConnected = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cast_project);

		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(
						CastMediaControlIntent.categoryForCast(REMOTE_DISPLAY_APP_ID))
				.build();
		mMediaRouter = MediaRouter.getInstance(getApplicationContext());

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
		MediaRouteActionProvider mediaRouteActionProvider =
				(MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
		mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);

		return true;
	}

	@Override
	protected void onStop() {
		mMediaRouter.removeCallback(mMediaRouterCallback);
		super.onStop();
	}

	@Override
	protected void onStart() {
		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
				MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
		super.onStart();
	}

	@Override
	public void handlePlayButton(View view) {
		if (!this.isChromecastConnected) {
			Toast.makeText(this, "Please connect your Chromecast first.", Toast.LENGTH_SHORT).show();
			return;
		}
		super.handlePlayButton(view);
	}

	@Override
	public void handleAddButton(View view) {
		int tabIndex = ((SlidingTabsBasicFragment) getSupportFragmentManager().findFragmentById(R.id.sliding_tabs_basic_fragment))
				.getSlidingTabLayout().getSlidingTabStrip().getSelectedPosition();

		//TODO (davidwittenbrink): Remove hardcoded tab values
		if (tabIndex == 0) {
			Toast.makeText(this, "Adding object to device", Toast.LENGTH_SHORT).show();
		} else if (tabIndex == 1) {
			Toast.makeText(this, "Adding object to cast", Toast.LENGTH_SHORT).show();
		}
		super.handleAddButton(view);
	}

	private class MyMediaRouterCallback extends MediaRouter.Callback {

		@Override
		public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
			mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
			String routeId = info.getId();

			if (mSelectedDevice != null) {
				startCastService();
			}
		}

		private void startCastService() {
			Intent intent = new Intent(CastProjectActivity.this,
					ProjectActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent notificationPendingIntent = PendingIntent.getActivity(
					CastProjectActivity.this, 0, intent, 0);

			CastRemoteDisplayLocalService.NotificationSettings settings =
					new CastRemoteDisplayLocalService.NotificationSettings.Builder()
							.setNotificationPendingIntent(notificationPendingIntent).build();

			CastRemoteDisplayLocalService.startService(CastProjectActivity.this,
					PresentationService.class, REMOTE_DISPLAY_APP_ID,
					mSelectedDevice, settings,
					new CastRemoteDisplayLocalService.Callbacks() {
						@Override
						public void onRemoteDisplaySessionStarted(
								CastRemoteDisplayLocalService service) {
							isChromecastConnected = true;
						}

						@Override
						public void onRemoteDisplaySessionError(Status errorReason) {
							int code = errorReason.getStatusCode();
							//initError();

							mSelectedDevice = null;
							CastProjectActivity.this.finish();
						}
					});
		}

		@Override
		public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
			//teardown();
			isChromecastConnected = false;
			mSelectedDevice = null;
		}
	}
}