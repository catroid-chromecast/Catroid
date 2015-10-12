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
package org.catrobat.catroid.common;

import android.content.Context;
import android.util.Log;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.BroadcastScript;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.WhenGamepadButtonScript;
import org.catrobat.catroid.content.WhenScript;
import org.catrobat.catroid.content.bricks.BrickBaseType;
import org.catrobat.catroid.content.bricks.BroadcastBrick;
import org.catrobat.catroid.content.bricks.ChangeXByNBrick;
import org.catrobat.catroid.content.bricks.ChangeYByNBrick;
import org.catrobat.catroid.content.bricks.ComeToFrontBrick;
import org.catrobat.catroid.content.bricks.ForeverBrick;
import org.catrobat.catroid.content.bricks.GlideToBrick;
import org.catrobat.catroid.content.bricks.HideBrick;
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick;
import org.catrobat.catroid.content.bricks.IfLogicElseBrick;
import org.catrobat.catroid.content.bricks.IfLogicEndBrick;
import org.catrobat.catroid.content.bricks.LoopEndlessBrick;
import org.catrobat.catroid.content.bricks.NextLookBrick;
import org.catrobat.catroid.content.bricks.PlaceAtBrick;
import org.catrobat.catroid.content.bricks.PlaySoundBrick;
import org.catrobat.catroid.content.bricks.SetLookBrick;
import org.catrobat.catroid.content.bricks.SetSizeToBrick;
import org.catrobat.catroid.content.bricks.SetVariableBrick;
import org.catrobat.catroid.content.bricks.SetXBrick;
import org.catrobat.catroid.content.bricks.SetYBrick;
import org.catrobat.catroid.content.bricks.ShowBrick;
import org.catrobat.catroid.content.bricks.StopAllSoundsBrick;
import org.catrobat.catroid.content.bricks.WaitBrick;
import org.catrobat.catroid.devices.mindstorms.nxt.sensors.NXTSensor;
import org.catrobat.catroid.drone.DroneBrickFactory;
import org.catrobat.catroid.formulaeditor.DataContainer;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.FormulaElement;
import org.catrobat.catroid.formulaeditor.FormulaElement.ElementType;
import org.catrobat.catroid.formulaeditor.Functions;
import org.catrobat.catroid.formulaeditor.Operators;
import org.catrobat.catroid.formulaeditor.Sensors;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.io.StorageHandler;
import org.catrobat.catroid.soundrecorder.SoundRecorder;
import org.catrobat.catroid.stage.StageListener;
import org.catrobat.catroid.utils.ImageEditing;
import org.catrobat.catroid.utils.UtilFile;

import java.io.File;
import java.io.IOException;

public final class StandardProjectHandler {

	private static final String TAG = StandardProjectHandler.class.getSimpleName();
	private static double backgroundImageScaleFactor = 1;

	// Suppress default constructor for noninstantiability
	private StandardProjectHandler() {
		throw new AssertionError();
	}

	public static Project createAndSaveStandardProject(Context context, boolean landscape) throws IOException {
		String projectName = context.getString(R.string.default_project_name);
		Project standardProject = null;

		if (StorageHandler.getInstance().projectExists(projectName)) {
			StorageHandler.getInstance().deleteProject(projectName);
		}

		try {
			standardProject = createAndSaveStandardProject(projectName, context, landscape);
		} catch (IllegalArgumentException ilArgument) {
			Log.e(TAG, "Could not create standard project!", ilArgument);
		}

		return standardProject;
	}

	public static Project createAndSaveStandardProject(Context context) throws IOException {
		return createAndSaveStandardProject(context, false);
	}

	public static Project createAndSaveStandardDroneProject(Context context) throws IOException {
		Log.d(TAG, "createAndSaveStandardDroneProject");
		String projectName = context.getString(R.string.default_drone_project_name);
		return createAndSaveStandardDroneProject(projectName, context);
	}

	public static Project createAndSaveStandardDroneProject(String projectName, Context context) throws IOException,
			IllegalArgumentException {
		if (StorageHandler.getInstance().projectExists(projectName)) {
			throw new IllegalArgumentException("Project with name '" + projectName + "' already exists!");
		}

		String backgroundName = context.getString(R.string.default_project_backgroundname);

		Project defaultDroneProject = new Project(context, projectName);
		defaultDroneProject.setDeviceData(context); // density anywhere here
		StorageHandler.getInstance().saveProject(defaultDroneProject);
		ProjectManager.getInstance().setProject(defaultDroneProject);

		backgroundImageScaleFactor = ImageEditing.calculateScaleFactorToScreenSize(
				R.drawable.default_project_background, context);

		File backgroundFile = UtilFile.copyImageFromResourceIntoProject(projectName, backgroundName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_project_background, context, true,
				backgroundImageScaleFactor);

		LookData backgroundLookData = new LookData();
		backgroundLookData.setLookName(backgroundName);
		backgroundLookData.setLookFilename(backgroundFile.getName());

		Sprite backgroundSprite = defaultDroneProject.getSpriteList().get(0);

		// Background sprite
		backgroundSprite.getLookDataList().add(backgroundLookData);
		Script backgroundStartScript = new StartScript();

		SetLookBrick setLookBrick = new SetLookBrick();
		setLookBrick.setLook(backgroundLookData);
		backgroundStartScript.addBrick(setLookBrick);

		backgroundSprite.addScript(backgroundStartScript);

		//Takeoff sprite
		String takeOffSpriteName = context.getString(R.string.default_drone_project_sprites_takeoff);

		File takeOffArrowFile = UtilFile.copyImageFromResourceIntoProject(projectName, takeOffSpriteName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_drone_project_orange_takeoff, context, true,
				backgroundImageScaleFactor);

		defaultDroneProject.addSprite(createDroneSprite(takeOffSpriteName, DroneBrickFactory.DroneBricks.DRONE_TAKE_OFF_BRICK,
				-260, -200, takeOffArrowFile));

		//land Sprite start
		String landSpriteName = context.getString(R.string.default_drone_project_srpites_land);

		File landArrowFile = UtilFile.copyImageFromResourceIntoProject(projectName, takeOffSpriteName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_drone_project_orange_land, context, true,
				backgroundImageScaleFactor);

		defaultDroneProject.addSprite(createDroneSprite(landSpriteName, DroneBrickFactory.DroneBricks.DRONE_LAND_BRICK, -260,
				-325, landArrowFile));

		//rotate Sprite start
		String rotateSpriteName = context.getString(R.string.default_drone_project_srpites_rotate);

		File rotateFile = UtilFile.copyImageFromResourceIntoProject(projectName, rotateSpriteName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_drone_project_orange_rotate, context, true,
				backgroundImageScaleFactor);

		defaultDroneProject.addSprite(createDroneSprite(rotateSpriteName, DroneBrickFactory.DroneBricks.DRONE_FLIP_BRICK,
				-260, -450, rotateFile));

		//Led Sprite
		//TODO Drone: add when PlayLedAnimationBrick works
		//String blinkLedSpriteName = context.getString(R.string.default_drone_project_sprites_blink_led);

		//TODO Drone: add when PlayLedAnimationBrick works
		//File playLedFile = UtilFile.copyImageFromResourceIntoProject(projectName, blinkLedSpriteName
		//		+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_drone_project_orange_light_bulb, context,
		//		true, backgroundImageScaleFactor);

		//TODO Drone: add when PlayLedAnimationBrick works
		//defaultDroneProject.addSprite(createDroneSprite(blinkLedSpriteName,
		//		DroneUtils.DroneBricks.DRONE_PLAY_LED_ANIMATION_BRICK, -100, -450, playLedFile));

		//Up Sprite
		String upSpriteName = context.getString(R.string.default_drone_project_sprites_up);

		File upFile = UtilFile.copyImageFromResourceIntoProject(projectName, upSpriteName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_drone_project_orange_arrow_up, context, true,
				backgroundImageScaleFactor);

		defaultDroneProject.addSprite(createDroneSprite(upSpriteName, DroneBrickFactory.DroneBricks.DRONE_MOVE_UP_BRICK, -100,
				-200, upFile, 2000));

		//Down Sprite
		String downSpriteName = context.getString(R.string.default_drone_project_sprites_down);

		File downFile = UtilFile.copyImageFromResourceIntoProject(projectName, downSpriteName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_drone_project_orange_arrow_down, context,
				true, backgroundImageScaleFactor);

		defaultDroneProject.addSprite(createDroneSprite(downSpriteName, DroneBrickFactory.DroneBricks.DRONE_MOVE_DOWN_BRICK,
				-100, -325, downFile, 2000));

		//Forward Sprite
		String forwardSpriteName = context.getString(R.string.default_drone_project_sprites_forward);

		File forwardFile = UtilFile.copyImageFromResourceIntoProject(projectName, forwardSpriteName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_drone_project_orange_go_forward, context,
				true, backgroundImageScaleFactor);

		defaultDroneProject.addSprite(createDroneSprite(forwardSpriteName,
				DroneBrickFactory.DroneBricks.DRONE_MOVE_FORWARD_BRICK, 180, -75, forwardFile, 2000));

		//Backward Sprite
		String backwardpriteName = context.getString(R.string.default_drone_project_sprites_back);

		File backwardFile = UtilFile.copyImageFromResourceIntoProject(projectName, downSpriteName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_drone_project_orange_go_back, context, true,
				backgroundImageScaleFactor);

		defaultDroneProject.addSprite(createDroneSprite(backwardpriteName,
				DroneBrickFactory.DroneBricks.DRONE_MOVE_BACKWARD_BRICK, 180, -450, backwardFile, 2000));

		//Left Sprite
		String leftSpriteName = context.getString(R.string.default_drone_project_sprites_left);

		File leftFile = UtilFile.copyImageFromResourceIntoProject(projectName, leftSpriteName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_drone_project_orange_go_left, context, true,
				backgroundImageScaleFactor);

		defaultDroneProject.addSprite(createDroneSprite(leftSpriteName, DroneBrickFactory.DroneBricks.DRONE_MOVE_LEFT_BRICK,
				100, -325, leftFile, 2000));

		//Right Sprite
		String rightSpriteName = context.getString(R.string.default_drone_project_sprites_right);

		File rightFile = UtilFile.copyImageFromResourceIntoProject(projectName, rightSpriteName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_drone_project_orange_go_right, context, true,
				backgroundImageScaleFactor);

		defaultDroneProject.addSprite(createDroneSprite(rightSpriteName, DroneBrickFactory.DroneBricks.DRONE_MOVE_RIGHT_BRICK,
				260, -325, rightFile, 2000));

		//Turn Left Sprite
		String turnLeftSpriteName = context.getString(R.string.default_drone_project_sprites_turn_left);

		File turnLeftFile = UtilFile.copyImageFromResourceIntoProject(projectName, turnLeftSpriteName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_drone_project_orange_turn_left, context, true,
				backgroundImageScaleFactor);

		defaultDroneProject.addSprite(createDroneSprite(turnLeftSpriteName,
				DroneBrickFactory.DroneBricks.DRONE_TURN_LEFT_BRICK, 100, -200, turnLeftFile, 2000));

		//Turn Right Sprite
		String turnRightSpriteName = context.getString(R.string.default_drone_project_sprites_turn_right);

		File turnrightFile = UtilFile.copyImageFromResourceIntoProject(projectName, turnRightSpriteName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_drone_project_orange_turn_right, context,
				true, backgroundImageScaleFactor);

		defaultDroneProject.addSprite(createDroneSprite(turnRightSpriteName,
				DroneBrickFactory.DroneBricks.DRONE_TURN_RIGHT_BRICK, 260, -200, turnrightFile, 2000));

		StorageHandler.getInstance().saveProject(defaultDroneProject);
		return defaultDroneProject;
	}

	private static Sprite createDroneSprite(String spriteName, DroneBrickFactory.DroneBricks brickName, int xPostition,
			int yPosition, File lookFile) {
		return createDroneSprite(spriteName, brickName, xPostition, yPosition, lookFile, 0, 0);
	}

	private static Sprite createDroneSprite(String spriteName, DroneBrickFactory.DroneBricks brickName, int xPostition,
			int yPosition, File lookFile, int timeInMilliseconds) {
		return createDroneSprite(spriteName, brickName, xPostition, yPosition, lookFile, timeInMilliseconds, 20);
	}

	private static Sprite createDroneSprite(String spriteName, DroneBrickFactory.DroneBricks brickName, int xPostition,
			int yPosition, File lookFile, int timeInMilliseconds, int powerInPercent) {
		//
		Sprite sprite = new Sprite(spriteName);
		//defaultDroneProject.addSprite(takeOffSprite);

		Script whenSpriteTappedScript = new WhenScript();
		BrickBaseType brick = DroneBrickFactory.getInstanceOfDroneBrick(brickName, sprite, timeInMilliseconds, powerInPercent);
		whenSpriteTappedScript.addBrick(brick);

		Script whenProjectStartsScript = new StartScript();
		PlaceAtBrick placeAtBrick = new PlaceAtBrick(calculateValueRelativeToScaledBackground(xPostition),
				calculateValueRelativeToScaledBackground(yPosition));
		SetSizeToBrick setSizeBrick = new SetSizeToBrick(50.0);

		whenProjectStartsScript.addBrick(placeAtBrick);
		whenProjectStartsScript.addBrick(setSizeBrick);

		LookData lookData = new LookData();
		lookData.setLookName(spriteName + " icon");

		lookData.setLookFilename(lookFile.getName());

		sprite.getLookDataList().add(lookData);

		sprite.addScript(whenSpriteTappedScript);
		sprite.addScript(whenProjectStartsScript);

		return sprite;
	}

	public static Project createAndSaveStandardProject(String projectName, Context context, boolean landscape) throws
			IOException,
			IllegalArgumentException {
		// temporarily until standard landscape project exists.
		if (landscape) {
			return createAndSaveEmptyProject(projectName, context, landscape, false);
		}
		if (StorageHandler.getInstance().projectExists(projectName)) {
			throw new IllegalArgumentException("Project with name '" + projectName + "' already exists!");
		}
		String moleLookName = context.getString(R.string.default_project_sprites_mole_name);
		String mole1Name = moleLookName + " 1";
		String mole2Name = moleLookName + " 2";
		String mole3Name = moleLookName + " 3";
		String mole4Name = moleLookName + " 4";
		String whackedMoleLookName = context.getString(R.string.default_project_sprites_mole_whacked);
		String movingMoleLookName = context.getString(R.string.default_project_sprites_mole_moving);
		String soundName = context.getString(R.string.default_project_sprites_mole_sound);
		String backgroundName = context.getString(R.string.default_project_backgroundname);

		String varRandomFrom = context.getString(R.string.default_project_var_random_from);
		String varRandomTo = context.getString(R.string.default_project_var_random_to);

		Project defaultProject = new Project(context, projectName, landscape);
		defaultProject.setDeviceData(context); // density anywhere here
		StorageHandler.getInstance().saveProject(defaultProject);
		ProjectManager.getInstance().setProject(defaultProject);

		backgroundImageScaleFactor = ImageEditing.calculateScaleFactorToScreenSize(
				R.drawable.default_project_background, context);

		File backgroundFile = UtilFile.copyImageFromResourceIntoProject(projectName, backgroundName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_project_background, context, true,
				backgroundImageScaleFactor);
		File movingMoleFile = UtilFile.copyImageFromResourceIntoProject(projectName, movingMoleLookName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_project_mole_moving, context, true,
				backgroundImageScaleFactor);
		File diggedOutMoleFile = UtilFile.copyImageFromResourceIntoProject(projectName, moleLookName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_project_mole_digged_out, context, true,
				backgroundImageScaleFactor);
		File whackedMoleFile = UtilFile.copyImageFromResourceIntoProject(projectName, whackedMoleLookName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_project_mole_whacked, context, true,
				backgroundImageScaleFactor);
		try {
			File soundFile1 = UtilFile.copySoundFromResourceIntoProject(projectName, soundName + "1"
					+ SoundRecorder.RECORDING_EXTENSION, R.raw.default_project_sound_mole_1, context, true);
			File soundFile2 = UtilFile.copySoundFromResourceIntoProject(projectName, soundName + "2"
					+ SoundRecorder.RECORDING_EXTENSION, R.raw.default_project_sound_mole_2, context, true);
			File soundFile3 = UtilFile.copySoundFromResourceIntoProject(projectName, soundName + "3"
					+ SoundRecorder.RECORDING_EXTENSION, R.raw.default_project_sound_mole_3, context, true);
			File soundFile4 = UtilFile.copySoundFromResourceIntoProject(projectName, soundName + "4"
					+ SoundRecorder.RECORDING_EXTENSION, R.raw.default_project_sound_mole_4, context, true);
			UtilFile.copyFromResourceIntoProject(projectName, ".", StageListener.SCREENSHOT_AUTOMATIC_FILE_NAME,
					R.drawable.default_project_screenshot, context, false);

			Log.i(TAG, String.format("createAndSaveStandardProject(%s) %s created%n %s created%n %s created%n %s created%n %s created%n %s created%n %s created%n %s created%n",
					projectName, backgroundFile.getName(), movingMoleFile.getName(), diggedOutMoleFile.getName(), whackedMoleFile.getName(),
					soundFile1.getName(), soundFile2.getName(), soundFile3.getName(), soundFile4.getName()));

			LookData movingMoleLookData = new LookData();
			movingMoleLookData.setLookName(movingMoleLookName);
			movingMoleLookData.setLookFilename(movingMoleFile.getName());

			LookData diggedOutMoleLookData = new LookData();
			diggedOutMoleLookData.setLookName(moleLookName);
			diggedOutMoleLookData.setLookFilename(diggedOutMoleFile.getName());

			LookData whackedMoleLookData = new LookData();
			whackedMoleLookData.setLookName(whackedMoleLookName);
			whackedMoleLookData.setLookFilename(whackedMoleFile.getName());

			LookData backgroundLookData = new LookData();
			backgroundLookData.setLookName(backgroundName);
			backgroundLookData.setLookFilename(backgroundFile.getName());

			SoundInfo soundInfo = new SoundInfo();
			soundInfo.setTitle(soundName);
			soundInfo.setSoundFileName(soundFile1.getName());

			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(soundInfo.getChecksum(), soundInfo.getAbsolutePath());

			DataContainer userVariables = defaultProject.getDataContainer();
			Sprite backgroundSprite = defaultProject.getSpriteList().get(0);

			userVariables.addProjectUserVariable(varRandomFrom);
			UserVariable randomFrom = userVariables.getUserVariable(varRandomFrom, backgroundSprite);

			userVariables.addProjectUserVariable(varRandomTo);
			UserVariable randomTo = userVariables.getUserVariable(varRandomTo, backgroundSprite);

			// Background sprite
			backgroundSprite.getLookDataList().add(backgroundLookData);
			Script backgroundStartScript = new StartScript();

			SetLookBrick setLookBrick = new SetLookBrick();
			setLookBrick.setLook(backgroundLookData);
			backgroundStartScript.addBrick(setLookBrick);

			SetVariableBrick setVariableBrick = new SetVariableBrick(new Formula(1), randomFrom);
			backgroundStartScript.addBrick(setVariableBrick);

			setVariableBrick = new SetVariableBrick(new Formula(5), randomTo);
			backgroundStartScript.addBrick(setVariableBrick);

			backgroundSprite.addScript(backgroundStartScript);

			FormulaElement randomElement = new FormulaElement(ElementType.FUNCTION, Functions.RAND.toString(), null);
			randomElement.setLeftChild(new FormulaElement(ElementType.USER_VARIABLE, varRandomFrom, randomElement));
			randomElement.setRightChild(new FormulaElement(ElementType.USER_VARIABLE, varRandomTo, randomElement));
			Formula randomWait = new Formula(randomElement);

			FormulaElement waitOneOrTwoSeconds = new FormulaElement(ElementType.FUNCTION, Functions.RAND.toString(),
					null);
			waitOneOrTwoSeconds.setLeftChild(new FormulaElement(ElementType.NUMBER, "1", waitOneOrTwoSeconds));
			waitOneOrTwoSeconds.setRightChild(new FormulaElement(ElementType.NUMBER, "2", waitOneOrTwoSeconds));

			// Mole 1 sprite
			Sprite mole1Sprite = new Sprite(mole1Name);
			mole1Sprite.getLookDataList().add(movingMoleLookData);
			mole1Sprite.getLookDataList().add(diggedOutMoleLookData);
			mole1Sprite.getLookDataList().add(whackedMoleLookData);
			mole1Sprite.getSoundList().add(soundInfo);

			Script mole1StartScript = new StartScript();
			Script mole1WhenScript = new WhenScript();

			// start script
			SetSizeToBrick setSizeToBrick = new SetSizeToBrick(new Formula(30));
			mole1StartScript.addBrick(setSizeToBrick);

			ForeverBrick foreverBrick = new ForeverBrick();
			mole1StartScript.addBrick(foreverBrick);

			PlaceAtBrick placeAtBrick = new PlaceAtBrick(calculateValueRelativeToScaledBackground(-160),
					calculateValueRelativeToScaledBackground(-110));
			mole1StartScript.addBrick(placeAtBrick);

			WaitBrick waitBrick = new WaitBrick(new Formula(waitOneOrTwoSeconds));
			mole1StartScript.addBrick(waitBrick);

			ShowBrick showBrick = new ShowBrick();
			mole1StartScript.addBrick(showBrick);

			setLookBrick = new SetLookBrick();
			setLookBrick.setLook(movingMoleLookData);
			mole1StartScript.addBrick(setLookBrick);

			GlideToBrick glideToBrick = new GlideToBrick(calculateValueRelativeToScaledBackground(-160),
					calculateValueRelativeToScaledBackground(-95), 100);
			mole1StartScript.addBrick(glideToBrick);

			setLookBrick = new SetLookBrick();
			setLookBrick.setLook(diggedOutMoleLookData);
			mole1StartScript.addBrick(setLookBrick);

			//add filechecksums
			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(movingMoleLookData.getChecksum(), movingMoleLookData.getAbsolutePath());
			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(diggedOutMoleLookData.getChecksum(), diggedOutMoleLookData.getAbsolutePath());
			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(whackedMoleLookData.getChecksum(), whackedMoleLookData.getAbsolutePath());
			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(backgroundLookData.getChecksum(), backgroundLookData.getAbsolutePath());

			waitBrick = new WaitBrick(randomWait.clone());
			mole1StartScript.addBrick(waitBrick);

			HideBrick hideBrick = new HideBrick();
			mole1StartScript.addBrick(hideBrick);

			waitBrick = new WaitBrick(randomWait.clone());
			mole1StartScript.addBrick(waitBrick);

			LoopEndlessBrick loopEndlessBrick = new LoopEndlessBrick(foreverBrick);
			mole1StartScript.addBrick(loopEndlessBrick);

			// when script
			PlaySoundBrick playSoundBrick = new PlaySoundBrick();
			playSoundBrick.setSoundInfo(soundInfo);
			mole1WhenScript.addBrick(playSoundBrick);

			setLookBrick = new SetLookBrick();
			setLookBrick.setLook(whackedMoleLookData);
			mole1WhenScript.addBrick(setLookBrick);

			waitBrick = new WaitBrick(1500);
			mole1WhenScript.addBrick(waitBrick);

			hideBrick = new HideBrick();
			mole1WhenScript.addBrick(hideBrick);

			mole1Sprite.addScript(mole1StartScript);
			mole1Sprite.addScript(mole1WhenScript);
			defaultProject.addSprite(mole1Sprite);

			StorageHandler.getInstance().fillChecksumContainer();

			// Mole 2 sprite
			Sprite mole2Sprite = mole1Sprite.clone();
			mole2Sprite.getSoundList().get(0).setSoundFileName(soundFile2.getName());

			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(soundFile2.getName(), soundFile2.getAbsolutePath());

			mole2Sprite.setName(mole2Name);
			defaultProject.addSprite(mole2Sprite);

			Script tempScript = mole2Sprite.getScript(0);
			placeAtBrick = (PlaceAtBrick) tempScript.getBrick(2);
			placeAtBrick.setXPosition(new Formula(calculateValueRelativeToScaledBackground(160)));
			placeAtBrick.setYPosition(new Formula(calculateValueRelativeToScaledBackground(-110)));

			glideToBrick = (GlideToBrick) tempScript.getBrick(6);
			glideToBrick.setXDestination(new Formula(calculateValueRelativeToScaledBackground(160)));
			glideToBrick.setYDestination(new Formula(calculateValueRelativeToScaledBackground(-95)));

			// Mole 3 sprite
			Sprite mole3Sprite = mole1Sprite.clone();
			mole3Sprite.getSoundList().get(0).setSoundFileName(soundFile3.getName());

			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(soundFile3.getName(), soundFile3.getAbsolutePath());

			mole3Sprite.setName(mole3Name);
			defaultProject.addSprite(mole3Sprite);

			tempScript = mole3Sprite.getScript(0);
			placeAtBrick = (PlaceAtBrick) tempScript.getBrick(2);
			placeAtBrick.setXPosition(new Formula(calculateValueRelativeToScaledBackground(-160)));
			placeAtBrick.setYPosition(new Formula(calculateValueRelativeToScaledBackground(-290)));

			glideToBrick = (GlideToBrick) tempScript.getBrick(6);
			glideToBrick.setXDestination(new Formula(calculateValueRelativeToScaledBackground(-160)));
			glideToBrick.setYDestination(new Formula(calculateValueRelativeToScaledBackground(-275)));

			// Mole 4 sprite
			Sprite mole4Sprite = mole1Sprite.clone();
			mole4Sprite.getSoundList().get(0).setSoundFileName(soundFile4.getName());

			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(soundFile4.getName(), soundFile4.getAbsolutePath());

			mole4Sprite.setName(mole4Name);
			defaultProject.addSprite(mole4Sprite);

			tempScript = mole4Sprite.getScript(0);
			placeAtBrick = (PlaceAtBrick) tempScript.getBrick(2);
			placeAtBrick.setXPosition(new Formula(calculateValueRelativeToScaledBackground(160)));
			placeAtBrick.setYPosition(new Formula(calculateValueRelativeToScaledBackground(-290)));

			glideToBrick = (GlideToBrick) tempScript.getBrick(6);
			glideToBrick.setXDestination(new Formula(calculateValueRelativeToScaledBackground(160)));
			glideToBrick.setYDestination(new Formula(calculateValueRelativeToScaledBackground(-275)));
		} catch (IllegalArgumentException illegalArgumentException) {
			throw new IOException(TAG, illegalArgumentException);
		}

		StorageHandler.getInstance().saveProject(defaultProject);

		return defaultProject;
	}


	public static Project createAndSaveStandardProjectCast(String projectName, Context context) throws IOException,
			IllegalArgumentException {
		if (StorageHandler.getInstance().projectExists(projectName)) {
			throw new IllegalArgumentException("Project with name '" + projectName + "' already exists!");
		}

		String androidLookName = context.getString(R.string.default_cast_project_sprites_android);
		String teleportLookName = context.getString(R.string.default_cast_project_sprites_teleport);
		String startLookName = context.getString(R.string.default_cast_project_sprites_start);
		String plateLookName = context.getString(R.string.default_cast_project_sprites_plate);
		String coinLookName = context.getString(R.string.default_cast_project_sprites_coin);
		String backgroundName = context.getString(R.string.default_cast_project_background_name);

		String androidName = androidLookName;
		String startName = startLookName;
		String coinName = coinLookName;

		String backgroundSoundName = context.getString(R.string.default_cast_project_background_sound);
		String coinSoundName = context.getString(R.string.default_cast_project_coin_sound);
		String teleportSoundName = context.getString(R.string.default_cast_project_teleport_sound);

		String varAndroidX = context.getString(R.string.default_cast_project_var_android_x);
		String varAndroidY = context.getString(R.string.default_cast_project_var_android_Y);
		String varPlateX = context.getString(R.string.default_cast_project_var_plate_x);
		String varPlateY = context.getString(R.string.default_cast_project_var_plate_y);
		String varCoinX = context.getString(R.string.default_cast_project_var_coin_x);
		String varCoinY = context.getString(R.string.default_cast_project_var_coin_y);
		String varNotRunning = context.getString(R.string.default_cast_project_var_not_running);

		Project defaultProject = new Project(context, projectName, true);
		defaultProject.setDeviceData(context); // density anywhere here
		StorageHandler.getInstance().saveProject(defaultProject);
		ProjectManager.getInstance().setProject(defaultProject);


		backgroundImageScaleFactor = ImageEditing.calculateScaleFactorToScreenSize(
				R.drawable.default_cast_project_background, context);

		File backgroundFile = UtilFile.copyImageFromResourceIntoProject(projectName, backgroundName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_cast_project_background, context, true,
				backgroundImageScaleFactor);

		File androidFile = UtilFile.copyImageFromResourceIntoProject(projectName, androidLookName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_cast_project_android, context, true,
				backgroundImageScaleFactor);

		File teleportFile = UtilFile.copyImageFromResourceIntoProject(projectName, teleportLookName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_cast_project_teleport, context, true,
				backgroundImageScaleFactor);

		File plateFile = UtilFile.copyImageFromResourceIntoProject(projectName, plateLookName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_cast_project_plate, context, true,
				backgroundImageScaleFactor);

		File coinFile = UtilFile.copyImageFromResourceIntoProject(projectName, coinLookName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_cast_project_coin, context, true,
				backgroundImageScaleFactor);

		File startFile = UtilFile.copyImageFromResourceIntoProject(projectName, startLookName
						+ Constants.IMAGE_STANDARD_EXTENTION, R.drawable.default_cast_project_start, context, true,
				backgroundImageScaleFactor);



		try {
			File backgroundSoundFile = UtilFile.copySoundFromResourceIntoProject(projectName, backgroundSoundName
					+ SoundRecorder.RECORDING_EXTENSION, R.raw.default_cast_project_background_sound, context, true);

			File coinSoundFile = UtilFile.copySoundFromResourceIntoProject(projectName, coinSoundName
					+ SoundRecorder.RECORDING_EXTENSION, R.raw.default_cast_project_coin_sound, context, true);

			File teleportSoundFile = UtilFile.copySoundFromResourceIntoProject(projectName, teleportSoundName
					+ SoundRecorder.RECORDING_EXTENSION, R.raw.default_cast_project_teleport_sound, context, true);



			LookData backgroundLookData = new LookData();
			backgroundLookData.setLookName(backgroundName);
			backgroundLookData.setLookFilename(backgroundFile.getName());

			LookData androidLookData = new LookData();
			androidLookData.setLookName(androidLookName);
			androidLookData.setLookFilename(androidFile.getName());

			LookData teleportLookData = new LookData();
			teleportLookData.setLookName(teleportLookName);
			teleportLookData.setLookFilename(teleportFile.getName());

			LookData plateLookData = new LookData();
			plateLookData.setLookName(plateLookName);
			plateLookData.setLookFilename(plateFile.getName());

			LookData coinLookData = new LookData();
			coinLookData.setLookName(coinLookName);
			coinLookData.setLookFilename(coinFile.getName());

			LookData startLookData = new LookData();
			startLookData.setLookName(startLookName);
			startLookData.setLookFilename(startFile.getName());

			SoundInfo backgroundSoundInfo = new SoundInfo();
			backgroundSoundInfo.setTitle(backgroundSoundName);
			backgroundSoundInfo.setSoundFileName(backgroundSoundFile.getName());
			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(backgroundSoundInfo.getChecksum(), backgroundSoundInfo.getAbsolutePath());

			SoundInfo coinSoundInfo = new SoundInfo();
			coinSoundInfo.setTitle(coinSoundName);
			coinSoundInfo.setSoundFileName(coinSoundFile.getName());

			SoundInfo teleportSoundInfo = new SoundInfo();
			teleportSoundInfo.setTitle(teleportSoundName);
			teleportSoundInfo.setSoundFileName(teleportSoundFile.getName());

			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(coinSoundInfo.getChecksum(), coinSoundInfo.getAbsolutePath());
			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(teleportSoundInfo.getChecksum(), teleportSoundInfo.getAbsolutePath());

			DataContainer userVariables = defaultProject.getDataContainer();
			Sprite backgroundSprite = defaultProject.getSpriteList().get(0);

			userVariables.addProjectUserVariable(varAndroidX);
			UserVariable androidX = userVariables.getUserVariable(varAndroidX, backgroundSprite);

			userVariables.addProjectUserVariable(varAndroidY);
			UserVariable androidY = userVariables.getUserVariable(varAndroidY, backgroundSprite);

			userVariables.addProjectUserVariable(varPlateX);
			UserVariable plateX = userVariables.getUserVariable(varPlateX, backgroundSprite);

			userVariables.addProjectUserVariable(varPlateY);
			UserVariable plateY = userVariables.getUserVariable(varPlateY, backgroundSprite);

			userVariables.addProjectUserVariable(varCoinX);
			UserVariable coinX = userVariables.getUserVariable(varCoinX, backgroundSprite);

			userVariables.addProjectUserVariable(varCoinY);
			UserVariable coinY = userVariables.getUserVariable(varCoinY, backgroundSprite);

			userVariables.addProjectUserVariable(varNotRunning);
			UserVariable notRunning = userVariables.getUserVariable(varNotRunning, backgroundSprite);


			// BACKGROUND SPRITE
			backgroundSprite.getLookDataList().add(backgroundLookData);
			backgroundSprite.getSoundList().add(backgroundSoundInfo);

			// When Start Script
			Script startScript = new StartScript();
			SetLookBrick setLookBrick = new SetLookBrick();
			setLookBrick.setLook(backgroundLookData);
			startScript.addBrick(setLookBrick);
			SetVariableBrick setVariableBrick = new SetVariableBrick(new Formula
					(new FormulaElement(ElementType.FUNCTION, Functions.TRUE.name(), null)), notRunning);
			startScript.addBrick(setVariableBrick);
			backgroundSprite.addScript(startScript);

			// When Broadcast "start" received script
			Script broadcastScript = new BroadcastScript("start");
			ForeverBrick foreverBrick = new ForeverBrick();
			broadcastScript.addBrick(foreverBrick);
			PlaySoundBrick playSoundBrick = new PlaySoundBrick();
			playSoundBrick.setSoundInfo(backgroundSoundInfo);
			broadcastScript.addBrick(playSoundBrick);
			WaitBrick waitBrick = new WaitBrick(new Formula(123));
			broadcastScript.addBrick(waitBrick);
			LoopEndlessBrick loopEndlessBrick = new LoopEndlessBrick(foreverBrick);
			broadcastScript.addBrick(loopEndlessBrick);
			backgroundSprite.addScript(broadcastScript);


			// ANDROID SPRITE
			Sprite sprite = new Sprite(androidName);
			sprite.getLookDataList().add(androidLookData);
			sprite.getLookDataList().add(teleportLookData);
			sprite.getSoundList().add(teleportSoundInfo);

			// Start script
			startScript = new StartScript();
			HideBrick hideBrick = new HideBrick();
			startScript.addBrick(hideBrick);
			foreverBrick = new ForeverBrick();
			startScript.addBrick(foreverBrick);
			setVariableBrick = new SetVariableBrick(Sensors.OBJECT_X);
			setVariableBrick.setUserVariable(androidX);
			startScript.addBrick(setVariableBrick);
			setVariableBrick = new SetVariableBrick(Sensors.OBJECT_Y);
			setVariableBrick.setUserVariable(androidY);
			startScript.addBrick(setVariableBrick);
			LoopEndlessBrick endlessBrick = new LoopEndlessBrick(foreverBrick);
			startScript.addBrick(endlessBrick);
			sprite.addScript(startScript);

			// When "Start" received Script
			broadcastScript = new BroadcastScript("start");
			setLookBrick = new SetLookBrick();
			setLookBrick.setLook(androidLookData);
			broadcastScript.addBrick(setLookBrick);
			SetSizeToBrick setSizeToBrick = new SetSizeToBrick(new Formula(20));
			broadcastScript.addBrick(setSizeToBrick);
			PlaceAtBrick placeAtBrick = new PlaceAtBrick(calculateValueRelativeToScaledBackground(0),
					calculateValueRelativeToScaledBackground(0));
			broadcastScript.addBrick(placeAtBrick);
			ShowBrick showBrick = new ShowBrick();
			broadcastScript.addBrick(showBrick);
			sprite.addScript(broadcastScript);

			// When Gamepad Button B pressed
			WhenGamepadButtonScript whenGamepadButtonScript = new WhenGamepadButtonScript(context.getString(R.string.cast_gamepad_B));
			SetLookBrick setLookBrick1 = new SetLookBrick();
			setLookBrick1.setLook(androidLookData);
			whenGamepadButtonScript.addBrick(setLookBrick1);
			playSoundBrick = new PlaySoundBrick();
			playSoundBrick.setSoundInfo(teleportSoundInfo);
			whenGamepadButtonScript.addBrick(playSoundBrick);
			waitBrick = new WaitBrick(new Formula(0.1));
			whenGamepadButtonScript.addBrick(waitBrick);
			ComeToFrontBrick comeToFrontBrick = new ComeToFrontBrick();
			whenGamepadButtonScript.addBrick(comeToFrontBrick);
			placeAtBrick = new PlaceAtBrick();
			placeAtBrick.setXPosition(new Formula(new FormulaElement(ElementType.USER_VARIABLE, plateX.getName(), null)));
			placeAtBrick.setYPosition(new Formula(new FormulaElement(ElementType.USER_VARIABLE, plateY.getName(), null)));
			whenGamepadButtonScript.addBrick(placeAtBrick);
			SetLookBrick setLookBrick2 = new SetLookBrick();
			setLookBrick2.setLook(teleportLookData);
			whenGamepadButtonScript.addBrick(setLookBrick2);
			whenGamepadButtonScript.addBrick(setLookBrick1);
			sprite.addScript(whenGamepadButtonScript);

			// When Start received
			broadcastScript = new BroadcastScript("start");
			broadcastScript.addBrick(foreverBrick);

			IfLogicBeginBrick ifLogicBeginBrick = new IfLogicBeginBrick(new Formula
					(new FormulaElement(ElementType.OPERATOR, Operators.GREATER_THAN.name(), null,
							new FormulaElement(ElementType.SENSOR, Sensors.OBJECT_X.name(), null),
									new FormulaElement(ElementType.NUMBER, "600", null))));
			broadcastScript.addBrick(ifLogicBeginBrick);
			SetXBrick setXBrick = new SetXBrick(-600);
			broadcastScript.addBrick(setXBrick);
			IfLogicElseBrick ifLogicElseBrick = new IfLogicElseBrick(ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicElseBrick);
			IfLogicEndBrick ifLogicEndBrick = new IfLogicEndBrick(ifLogicElseBrick, ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicEndBrick);

			ifLogicBeginBrick = new IfLogicBeginBrick(new Formula
					(new FormulaElement(ElementType.OPERATOR, Operators.SMALLER_THAN.name(), null,
							new FormulaElement(ElementType.SENSOR, Sensors.OBJECT_X.name(), null),
							new FormulaElement(ElementType.NUMBER, "-600", null))));
			broadcastScript.addBrick(ifLogicBeginBrick);
			setXBrick = new SetXBrick(600);
			broadcastScript.addBrick(setXBrick);
			ifLogicElseBrick = new IfLogicElseBrick(ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicElseBrick);
			ifLogicEndBrick = new IfLogicEndBrick(ifLogicElseBrick, ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicEndBrick);

			ifLogicBeginBrick = new IfLogicBeginBrick(new Formula
					(new FormulaElement(ElementType.OPERATOR, Operators.SMALLER_THAN.name(), null,
							new FormulaElement(ElementType.SENSOR, Sensors.OBJECT_Y.name(), null),
							new FormulaElement(ElementType.NUMBER, "-360", null))));
			broadcastScript.addBrick(ifLogicBeginBrick);
			SetYBrick setYBrick = new SetYBrick(360);
			broadcastScript.addBrick(setYBrick);
			ifLogicElseBrick = new IfLogicElseBrick(ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicElseBrick);
			ifLogicEndBrick = new IfLogicEndBrick(ifLogicElseBrick, ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicEndBrick);

			ifLogicBeginBrick = new IfLogicBeginBrick(new Formula
					(new FormulaElement(ElementType.OPERATOR, Operators.GREATER_THAN.name(), null,
							new FormulaElement(ElementType.SENSOR, Sensors.OBJECT_Y.name(), null),
							new FormulaElement(ElementType.NUMBER, "360", null))));
			broadcastScript.addBrick(ifLogicBeginBrick);
			setYBrick = new SetYBrick(-360);
			broadcastScript.addBrick(setYBrick);
			ifLogicElseBrick = new IfLogicElseBrick(ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicElseBrick);
			ifLogicEndBrick = new IfLogicEndBrick(ifLogicElseBrick, ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicEndBrick);
			broadcastScript.addBrick(loopEndlessBrick);
			sprite.addScript(broadcastScript);

			// When Start received
			broadcastScript = new BroadcastScript("start");
			broadcastScript.addBrick(foreverBrick);

			ifLogicBeginBrick = new IfLogicBeginBrick(new Formula
					(new FormulaElement(ElementType.SENSOR, Sensors.GAMEPAD_UP_PRESSED.name(), null)));
			broadcastScript.addBrick(ifLogicBeginBrick);
			ChangeYByNBrick changeYByNBrick = new ChangeYByNBrick(5);
			broadcastScript.addBrick(changeYByNBrick);
			ifLogicElseBrick = new IfLogicElseBrick(ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicElseBrick);
			ifLogicEndBrick = new IfLogicEndBrick(ifLogicElseBrick, ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicEndBrick);

			ifLogicBeginBrick = new IfLogicBeginBrick(new Formula
					(new FormulaElement(ElementType.SENSOR, Sensors.GAMEPAD_DOWN_PRESSED.name(), null)));
			broadcastScript.addBrick(ifLogicBeginBrick);
			changeYByNBrick = new ChangeYByNBrick(-5);
			broadcastScript.addBrick(changeYByNBrick);
			ifLogicElseBrick = new IfLogicElseBrick(ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicElseBrick);
			ifLogicEndBrick = new IfLogicEndBrick(ifLogicElseBrick, ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicEndBrick);

			ifLogicBeginBrick = new IfLogicBeginBrick(new Formula
					(new FormulaElement(ElementType.SENSOR, Sensors.GAMEPAD_RIGHT_PRESSED.name(), null)));
			broadcastScript.addBrick(ifLogicBeginBrick);
			ChangeXByNBrick changeXByNBrick = new ChangeXByNBrick(5);
			broadcastScript.addBrick(changeXByNBrick);
			ifLogicElseBrick = new IfLogicElseBrick(ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicElseBrick);
			ifLogicEndBrick = new IfLogicEndBrick(ifLogicElseBrick, ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicEndBrick);

			ifLogicBeginBrick = new IfLogicBeginBrick(new Formula
					(new FormulaElement(ElementType.SENSOR, Sensors.GAMEPAD_LEFT_PRESSED.name(), null)));
			broadcastScript.addBrick(ifLogicBeginBrick);
			changeXByNBrick = new ChangeXByNBrick(-5);
			broadcastScript.addBrick(changeXByNBrick);
			ifLogicElseBrick = new IfLogicElseBrick(ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicElseBrick);
			ifLogicEndBrick = new IfLogicEndBrick(ifLogicElseBrick, ifLogicBeginBrick);
			broadcastScript.addBrick(ifLogicEndBrick);

			broadcastScript.addBrick(loopEndlessBrick);
			sprite.addScript(broadcastScript);
			defaultProject.addSprite(sprite);

			// START SPRITE
			sprite = new Sprite(startName);
			sprite.getLookDataList().add(startLookData);

			// start script
			startScript = new StartScript();
			placeAtBrick = new PlaceAtBrick(0,0);
			startScript.addBrick(placeAtBrick);
			sprite.addScript(startScript);

			//When Gamepad Button A pressed
			whenGamepadButtonScript = new WhenGamepadButtonScript(context.getString(R.string.cast_gamepad_A));
			ifLogicBeginBrick = new IfLogicBeginBrick(new Formula
					(new FormulaElement(ElementType.USER_VARIABLE, notRunning.getName(), null)));
			whenGamepadButtonScript.addBrick(ifLogicBeginBrick);
			BroadcastBrick broadcastBrick = new BroadcastBrick("start");
			whenGamepadButtonScript.addBrick(broadcastBrick);
			setVariableBrick = new SetVariableBrick(new Formula( new FormulaElement(
					ElementType.FUNCTION, Functions.FALSE.name(), null)), notRunning);
			whenGamepadButtonScript.addBrick(setVariableBrick);
			whenGamepadButtonScript.addBrick(hideBrick);
			ifLogicElseBrick = new IfLogicElseBrick(ifLogicBeginBrick);
			whenGamepadButtonScript.addBrick(ifLogicElseBrick);
			ifLogicEndBrick = new IfLogicEndBrick(ifLogicElseBrick, ifLogicBeginBrick);
			whenGamepadButtonScript.addBrick(ifLogicEndBrick);
			sprite.addScript(whenGamepadButtonScript);
			defaultProject.addSprite(sprite);

			// PLATE SPRITE
			sprite = new Sprite(plateLookName);
			sprite.getLookDataList().add(plateLookData);

			// start script
			startScript = new StartScript();
			startScript.addBrick(hideBrick);
			startScript.addBrick(foreverBrick);
			setVariableBrick = new SetVariableBrick(Sensors.OBJECT_X);
			setVariableBrick.setUserVariable(plateX);
			startScript.addBrick(setVariableBrick);
			setVariableBrick = new SetVariableBrick(Sensors.OBJECT_Y);
			setVariableBrick.setUserVariable(plateY);
			startScript.addBrick(setVariableBrick);
			startScript.addBrick(loopEndlessBrick);
			sprite.addScript(startScript);

			// when received start
			broadcastScript = new BroadcastScript("start");
			setSizeToBrick = new SetSizeToBrick(20);
			broadcastScript.addBrick(setSizeToBrick);
			broadcastScript.addBrick(showBrick);
			broadcastScript.addBrick(foreverBrick);
			FormulaElement randomElement = new FormulaElement(ElementType.FUNCTION, Functions.RAND.toString(), null);
			randomElement.setLeftChild(new FormulaElement(ElementType.NUMBER, "-600", randomElement));
			randomElement.setRightChild(new FormulaElement(ElementType.NUMBER, "600", randomElement));
			placeAtBrick = new PlaceAtBrick();
			placeAtBrick.setXPosition(new Formula(randomElement));
			FormulaElement randomElement2 = new FormulaElement(ElementType.FUNCTION, Functions.RAND.toString(), null);
			randomElement2.setLeftChild(new FormulaElement(ElementType.NUMBER, "-300", randomElement2));
			randomElement2.setRightChild(new FormulaElement(ElementType.NUMBER, "300", randomElement2));
			placeAtBrick.setYPosition(new Formula(randomElement2));
			broadcastScript.addBrick(placeAtBrick);
			waitBrick.setTimeToWait(new Formula(2));
			broadcastScript.addBrick(waitBrick);
			broadcastScript.addBrick(loopEndlessBrick);
			sprite.addScript(broadcastScript);
			defaultProject.addSprite(sprite);

			// COIN SPRITE
			GlideToBrick glideToBrick = new GlideToBrick();
			sprite = new Sprite(coinLookName);
			sprite.getLookDataList().add(coinLookData);
			sprite.getSoundList().add(coinSoundInfo);

			//when start script
			startScript = new StartScript();
			randomElement.setLeftChild(new FormulaElement(ElementType.NUMBER, "-600", randomElement));
			randomElement.setRightChild(new FormulaElement(ElementType.NUMBER, "600", randomElement));
			placeAtBrick = new PlaceAtBrick();
			placeAtBrick.setXPosition(new Formula(randomElement));
			placeAtBrick.setYPosition(new Formula(450));
			startScript.addBrick(placeAtBrick);
			startScript.addBrick(hideBrick);
			startScript.addBrick(foreverBrick);
			setVariableBrick = new SetVariableBrick(Sensors.OBJECT_X);
			setVariableBrick.setUserVariable(coinX);
			startScript.addBrick(setVariableBrick);
			setVariableBrick = new SetVariableBrick(Sensors.OBJECT_Y);
			setVariableBrick.setUserVariable(coinY);
			startScript.addBrick(setVariableBrick);
			startScript.addBrick(loopEndlessBrick);
			sprite.addScript(startScript);

			//when I receive START
			broadcastScript = new BroadcastScript("start");
			broadcastScript.addBrick(foreverBrick);
			setSizeToBrick = new SetSizeToBrick(25);
			broadcastScript.addBrick(setSizeToBrick);
			broadcastScript.addBrick(placeAtBrick);
			broadcastScript.addBrick(showBrick);
			Formula x = new Formula(new FormulaElement(
					ElementType.SENSOR, Sensors.OBJECT_X.name(), null));
			glideToBrick = new GlideToBrick(x,new Formula(-450), new Formula(2.5));
			broadcastScript.addBrick(glideToBrick);
			broadcastScript.addBrick(endlessBrick);
			sprite.addScript(broadcastScript);

			//when I receive START
			broadcastScript = new BroadcastScript("start");
			broadcastScript.addBrick(foreverBrick);

			// Collusion detection
			FormulaElement plus = new FormulaElement(
					ElementType.OPERATOR, Operators.PLUS.name(), null);
			FormulaElement plus2 = new FormulaElement(
					ElementType.OPERATOR, Operators.PLUS.name(), null);
			FormulaElement minus = new FormulaElement(
					ElementType.OPERATOR, Operators.MINUS.name(), null);
			FormulaElement minus2 = new FormulaElement(
					ElementType.OPERATOR, Operators.MINUS.name(), null);

			FormulaElement greaterThan = new FormulaElement(
					ElementType.OPERATOR, Operators.GREATER_THAN.name(), null);
			FormulaElement smallerThan = new FormulaElement(
					ElementType.OPERATOR, Operators.SMALLER_THAN.name(), null);
			FormulaElement greaterThan2 = new FormulaElement(
					ElementType.OPERATOR, Operators.GREATER_THAN.name(), null);
			FormulaElement smallerThan2 = new FormulaElement(
					ElementType.OPERATOR, Operators.SMALLER_THAN.name(), null);

			FormulaElement andrX = new FormulaElement(
					ElementType.USER_VARIABLE, androidX.getName(), null);
			FormulaElement andrY = new FormulaElement(
					ElementType.USER_VARIABLE, androidY.getName(), null);

			FormulaElement posX = new FormulaElement(
					ElementType.SENSOR, Sensors.OBJECT_X.name(), null);
			FormulaElement posY = new FormulaElement(
					ElementType.SENSOR, Sensors.OBJECT_Y.name(), null);

			FormulaElement fifty = new FormulaElement(
					ElementType.NUMBER, "50", null);

			minus.setLeftChild(andrX);
			minus.setRightChild(fifty);
			plus.setLeftChild(andrX);
			plus.setRightChild(fifty);
			greaterThan.setLeftChild(posX);
			greaterThan.setRightChild(minus);
			smallerThan.setLeftChild(posX);
			smallerThan.setRightChild(plus);

			FormulaElement widthCollusion = new FormulaElement(
					ElementType.OPERATOR, Operators.LOGICAL_AND.name(), null, greaterThan, smallerThan);


			minus2.setLeftChild(andrY);
			minus2.setRightChild(fifty);
			plus2.setLeftChild(andrY);
			plus2.setRightChild(fifty);
			greaterThan2.setLeftChild(posY);
			greaterThan2.setRightChild(minus2);
			smallerThan2.setLeftChild(posY);
			smallerThan2.setRightChild(plus2);

			FormulaElement heightCollusion = new FormulaElement(
					ElementType.OPERATOR, Operators.LOGICAL_AND.name(), null, greaterThan2, smallerThan2);

			FormulaElement collusionDetection = new FormulaElement(
					ElementType.OPERATOR, Operators.LOGICAL_AND.name(), null, widthCollusion, heightCollusion);

			ifLogicBeginBrick = new IfLogicBeginBrick(new Formula(collusionDetection));
			broadcastScript.addBrick(ifLogicBeginBrick);
			placeAtBrick = new PlaceAtBrick(calculateValueRelativeToScaledBackground(500),
					calculateValueRelativeToScaledBackground(-600));
			broadcastScript.addBrick(placeAtBrick);
			playSoundBrick.setSoundInfo(coinSoundInfo);
			broadcastScript.addBrick(playSoundBrick);
			broadcastScript.addBrick(ifLogicElseBrick);
			broadcastScript.addBrick(ifLogicEndBrick);
			broadcastScript.addBrick(loopEndlessBrick);
			sprite.addScript(broadcastScript);
			defaultProject.addSprite(sprite);

			//add filechecksums
			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(backgroundLookData.getChecksum(), backgroundLookData.getAbsolutePath());
			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(androidLookData.getChecksum(), androidLookData.getAbsolutePath());
			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(teleportLookData.getChecksum(), teleportLookData.getAbsolutePath());
			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(coinLookData.getChecksum(), coinLookData.getAbsolutePath());
			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(plateLookData.getChecksum(), plateLookData.getAbsolutePath());
			ProjectManager.getInstance().getFileChecksumContainer().addChecksum(startLookData.getChecksum(), startLookData.getAbsolutePath());

			StorageHandler.getInstance().fillChecksumContainer();

		} catch (IllegalArgumentException illegalArgumentException) {
			throw new IOException(TAG, illegalArgumentException);
		}
		defaultProject.setChromecastFields();
		StorageHandler.getInstance().saveProject(defaultProject);

		return defaultProject;
	}

	public static Project createAndSaveStandardProject(String projectName, Context context) throws
			IOException,
			IllegalArgumentException {
		return createAndSaveStandardProject(projectName, context, false);
	}

	public static Project createAndSaveEmptyProject(String projectName, Context context, boolean landscape, boolean chromecast) {
		if (StorageHandler.getInstance().projectExists(projectName)) {
			throw new IllegalArgumentException("Project with name '" + projectName + "' already exists!");
		}
		Project emptyProject = new Project(context, projectName, landscape);
		emptyProject.setDeviceData(context);
		StorageHandler.getInstance().saveProject(emptyProject);
		ProjectManager.getInstance().setProject(emptyProject);

		if (chromecast) {
			emptyProject.setChromecastFields();
		}

		return emptyProject;
	}

	public static Project createAndSaveEmptyProject(String projectName, Context context) {
		return createAndSaveEmptyProject(projectName, context, false, false);
	}

	private static int calculateValueRelativeToScaledBackground(int value) {
		int returnValue = (int) (value * backgroundImageScaleFactor);
		int differenceToNextFive = returnValue % 5;
		return returnValue - differenceToNextFive;
	}
}
