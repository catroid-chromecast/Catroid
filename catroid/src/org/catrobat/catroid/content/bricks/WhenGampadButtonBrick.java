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
package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.BaseAdapter;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenGamepadButtonScript;
import org.catrobat.catroid.content.WhenScript;

import java.util.List;

public class WhenGampadButtonBrick extends ScriptBrick {
	protected WhenGamepadButtonScript whenVirtualButtonScript;
	private static final long serialVersionUID = 1L;

	public WhenGampadButtonBrick(WhenGamepadButtonScript whenVirtualButtonScript) {
		this.whenVirtualButtonScript = whenVirtualButtonScript;
	}

	@Override
	public int getRequiredResources() {
		return NO_RESOURCES;
	}

	@Override
	public Brick copyBrickForSprite(Sprite sprite) {
		WhenGampadButtonBrick copyBrick = (WhenGampadButtonBrick) clone();
		copyBrick.whenVirtualButtonScript = whenVirtualButtonScript;
		return copyBrick;
	}

	@Override
	public View getView(final Context context, int brickId, final BaseAdapter baseAdapter) {
		if (animationState) {
			return view;
		}

		view = View.inflate(context, R.layout.brick_when_gamepad_button, null);

		setCheckboxView(R.id.brick_when_gamepad_button_checkbox);

		return view;
	}

	@Override
	public View getViewWithAlpha(int alphaValue) {

		if (view != null) {

			View layout = view.findViewById(R.id.brick_when_gamepad_button_layout);
			Drawable background = layout.getBackground();
			background.setAlpha(alphaValue);
			this.alphaValue = (alphaValue);

		}

		return view;
	}

	@Override
	public View getPrototypeView(Context context) {
		return getView(context, 0, null);
	}

	@Override
	public Brick clone() {
		return new WhenGampadButtonBrick(null);
	}

	@Override
	public Script getScriptSafe() {
		if (whenVirtualButtonScript == null) {
			whenVirtualButtonScript = new WhenGamepadButtonScript();
		}

		return whenVirtualButtonScript;
	}

	@Override
	public List<SequenceAction> addActionToSequence(Sprite sprite, SequenceAction sequence) {
		return null;
	}
}