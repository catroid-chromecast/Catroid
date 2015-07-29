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

/**
 * Created by Paul on 18.07.2015.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.app.MediaRouteButton;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.catrobat.catroid.R;

/*
 * Composite UI for a button with a cast icon and text label
 */
public class CastMediaRouterButtonView extends LinearLayout {

	private MediaRouteButton mMediaRouteButton;
	private TextView mTextView;

	public CastMediaRouterButtonView(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MediaRouterButtonView, 0, 0);
		a.recycle();

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.media_router_button_view, this, true);

		mMediaRouteButton = (MediaRouteButton) findViewById(R.id.media_route_button);
		mTextView = (TextView) findViewById(R.id.chromecast_play_text);

	}

	public CastMediaRouterButtonView(Context context) {
		this(context, null);
	}

	public MediaRouteButton getMediaRouteButton() {
		return mMediaRouteButton;
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		// Simulate a click on the button as a click on the cast icon
		mMediaRouteButton.performClick();
		return false;
	}

}
