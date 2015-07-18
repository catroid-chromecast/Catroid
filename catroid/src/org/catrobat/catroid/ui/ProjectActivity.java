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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import com.google.android.gms.common.api.Status;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.drone.DroneInitializer;
import org.catrobat.catroid.facedetection.FaceDetectionHandler;
import org.catrobat.catroid.formulaeditor.SensorHandler;
import org.catrobat.catroid.stage.PreStageActivity;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.ui.adapter.SpriteAdapter;
import org.catrobat.catroid.ui.dialogs.NewSpriteDialog;
import org.catrobat.catroid.ui.fragment.SpritesListFragment;
import org.catrobat.catroid.utils.Utils;

import java.util.concurrent.locks.Lock;

public class ProjectActivity extends BaseActivity {

	private SpritesListFragment spritesListFragment;
	private Lock viewSwitchLock = new ViewSwitchLock();

	private MediaRouteButton mMediaRouteButton;
	private CastMediaRouterButtonView mMediaRouterButtonView;
	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private CastDevice mSelectedDevice;
	private final MyMediaRouterCallback mMediaRouterCallback = new MyMediaRouterCallback();
	private final String REMOTE_DISPLAY_APP_ID = "CEBB9229";
	public static final String INTENT_EXTRA_CAST_DEVICE = "CastDevice";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_project);

		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(CastMediaControlIntent.categoryForCast(REMOTE_DISPLAY_APP_ID))
				.build();
		mMediaRouter = MediaRouter.getInstance(getApplicationContext());

		mMediaRouterButtonView = (CastMediaRouterButtonView) findViewById(R.id.media_route_button_view);
		if (mMediaRouterButtonView != null) {
			mMediaRouteButton = mMediaRouterButtonView.getMediaRouteButton();
			mMediaRouteButton.setRouteSelector(mMediaRouteSelector);
		}

		// TODO
		//if(Cc project)
		//{
			findViewById(R.id.media_route_button_view).setVisibility(View.VISIBLE);
			findViewById(R.id.button_play).setVisibility(View.GONE);
			//findViewById(R.id.button_play).weight
		//}
		//else
//		findViewById(R.id.media_route_button_view).setVisibility(View.GONE);
//		findViewById(R.id.button_play).setVisibility(View.VISIBLE);

		if (getIntent() != null && getIntent().hasExtra(Constants.PROJECT_OPENED_FROM_PROJECTS_LIST)) {
			setReturnToProjectsList(true);
		}

	}

	@Override
	protected void onStart() {
		super.onStart();

		if(isCastServiceRunning(CastService.class))
			CastRemoteDisplayLocalService.stopService();

		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);

		String programName;
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			programName = bundle.getString(Constants.PROJECTNAME_TO_LOAD);
		} else {
			programName = ProjectManager.getInstance().getCurrentProject().getName();
		}

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		setTitleActionBar(programName);

		spritesListFragment = (SpritesListFragment) getSupportFragmentManager().findFragmentById(
				R.id.fragment_sprites_list);

		SettingsActivity.setLegoMindstormsNXTSensorChooserEnabled(this, true);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (spritesListFragment != null && spritesListFragment.isLoading == false) {
			handleShowDetails(spritesListFragment.getShowDetails(), menu.findItem(R.id.show_details));
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (spritesListFragment != null && spritesListFragment.isLoading == false) {
			getMenuInflater().inflate(R.menu.menu_current_project, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onStop() {
		mMediaRouter.removeCallback(mMediaRouterCallback);
		super.onStop();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.show_details:
				handleShowDetails(!spritesListFragment.getShowDetails(), item);
				break;

			case R.id.copy:
				spritesListFragment.startCopyActionMode();
				break;

			case R.id.cut:
				break;

			case R.id.insert_below:
				break;

			case R.id.move:
				break;

			case R.id.rename:
				spritesListFragment.startRenameActionMode();
				break;

			case R.id.delete:
				spritesListFragment.startDeleteActionMode();
				break;

			case R.id.upload:
				ProjectManager.getInstance().uploadProject(Utils.getCurrentProjectName(this), this);
				break;

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == PreStageActivity.REQUEST_RESOURCES_INIT && resultCode == RESULT_OK) {
			Intent intent = new Intent(ProjectActivity.this, StageActivity.class);
			DroneInitializer.addDroneSupportExtraToNewIntentIfPresentInOldIntent(data, intent);
			startActivity(intent);
		}
		if (requestCode == StageActivity.STAGE_ACTIVITY_FINISH) {
			SensorHandler.stopSensorListeners();
			FaceDetectionHandler.stopFaceDetection();
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			sendBroadcast(new Intent(ScriptActivity.ACTION_SPRITES_LIST_INIT));
		}
	}

	public void handleCheckBoxClick(View view) {
		spritesListFragment.handleCheckBoxClick(view);
	}

	public void handleAddButton(View view) {
		if (!viewSwitchLock.tryLock()) {
			return;
		}
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		Fragment previousFragment = getSupportFragmentManager().findFragmentByTag(NewSpriteDialog.DIALOG_FRAGMENT_TAG);
		if (previousFragment != null) {
			fragmentTransaction.remove(previousFragment);
		}

		DialogFragment newFragment = new NewSpriteDialog();
		newFragment.show(fragmentTransaction, NewSpriteDialog.DIALOG_FRAGMENT_TAG);
	}

	public void handlePlayButton(View view) {
		if (!viewSwitchLock.tryLock()) {
			return;
		}

		if(mSelectedDevice != null)
		{
			ProjectManager.getInstance().getCurrentProject().getDataContainer().resetAllDataObjects();
			Intent intent = new Intent(this, PreStageActivity.class);
			startActivityForResult(intent, PreStageActivity.REQUEST_RESOURCES_INIT);

			startCastService();
		}
		else
		{
			Context context = getApplicationContext();
			CharSequence text = "Please connect to a cast device first!";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
		}
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		// Dismiss ActionMode without effecting sounds
		if (spritesListFragment.getActionModeActive() && event.getKeyCode() == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_UP) {
			SpriteAdapter adapter = (SpriteAdapter) spritesListFragment.getListAdapter();
			adapter.clearCheckedSprites();
		}

		return super.dispatchKeyEvent(event);
	}

	public void handleShowDetails(boolean showDetails, MenuItem item) {
		spritesListFragment.setShowDetails(showDetails);

		item.setTitle(showDetails ? R.string.hide_details : R.string.show_details);
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
		Intent intent = new Intent(ProjectActivity.this,ProjectActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent notificationPendingIntent = PendingIntent.getActivity(ProjectActivity.this, 0, intent, 0);

		CastRemoteDisplayLocalService.NotificationSettings settings =
				new CastRemoteDisplayLocalService.NotificationSettings.Builder()
				.setNotificationPendingIntent(notificationPendingIntent).build();
		CastRemoteDisplayLocalService.startService(ProjectActivity.this, CastService.class,
				REMOTE_DISPLAY_APP_ID, mSelectedDevice, settings,
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
						ProjectActivity.this.finish();
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
