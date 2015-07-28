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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.cast.CastPresentation;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;

import org.catrobat.catroid.R;

/**
 * Service to keep the remote display running even when the app goes into the background
 */
public class CastService extends CastRemoteDisplayLocalService {

    private static final String TAG = "PresentationService";
    private Display display;

    // First screen
    private CastPresentation mPresentation;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onCreatePresentation(Display display) {
        createPresentation(display);
    }

    @Override
    public void onDismissPresentation() {
        dismissPresentation();
    }

    private void dismissPresentation() {
        if (mPresentation != null) {
            mPresentation.dismiss();
            mPresentation = null;
        }
    }

    public void createPresentation(Display display) {
        if(display != null)
            this.display = display;
        dismissPresentation();
        mPresentation = new FirstScreenPresentation(this, this.display);

        try {
            mPresentation.show();
        } catch (WindowManager.InvalidDisplayException ex) {
            Log.e(TAG, "Unable to show presentation, display was removed.", ex);
            Toast.makeText(getApplicationContext(), getString(R.string.cast_connection_error_msg), Toast.LENGTH_SHORT).show();
            dismissPresentation();
        }
    }

    public class FirstScreenPresentation extends CastPresentation {

        private final String TAG = "FirstScreenPresentation";

        public FirstScreenPresentation(Context context, Display display) {
            super(context, display);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            RelativeLayout layout = new RelativeLayout(getApplication());
            CastManager.getInstance().setLayout(layout);
            CastManager.getInstance().setContext(getApplication());

            ImageView imageView = new ImageView(getContext());
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.cast_screensaver);
            imageView.setImageDrawable(drawable);
            layout.addView(imageView);
            setContentView(layout);
            CastManager.getInstance().setIsConnected(true);
        }
    }
}