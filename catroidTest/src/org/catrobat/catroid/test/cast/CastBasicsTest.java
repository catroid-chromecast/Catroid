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
package org.catrobat.catroid.test.cast;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenGamepadButtonScript;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.stage.StageListener;
import org.catrobat.catroid.test.utils.BaseActivityUnitTestCase;
import org.catrobat.catroid.uitest.util.UiTestUtils;

public class CastBasicsTest extends BaseActivityUnitTestCase<StageActivity> {

	private static final String TAG = CastBasicsTest.class.getSimpleName();

	public CastBasicsTest() {
		super(StageActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		UiTestUtils.createTestProject();
	}

	public void testHasSpriteGamepadScript() throws InterruptedException {
		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
		assertNotNull("Sprite is null", sprite);

		boolean hasGamepadScript = StageListener.hasSpriteGamepadScript(sprite);
		assertFalse("Gamepad script detected, but there should be none", hasGamepadScript);

		sprite.addScript(new WhenGamepadButtonScript());
		hasGamepadScript = StageListener.hasSpriteGamepadScript(sprite);
		assertTrue("Gamepad script not detected", hasGamepadScript);

	}

}
