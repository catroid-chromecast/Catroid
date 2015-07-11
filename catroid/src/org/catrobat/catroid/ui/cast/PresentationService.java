package org.catrobat.catroid.ui.cast;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.android.AndroidApplicationBase;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidAudio;
import com.badlogic.gdx.backends.android.AndroidClipboard;
import com.badlogic.gdx.backends.android.AndroidEventListener;
import com.badlogic.gdx.backends.android.AndroidFiles;
import com.badlogic.gdx.backends.android.AndroidGraphics;
import com.badlogic.gdx.backends.android.AndroidInput;
import com.badlogic.gdx.backends.android.AndroidInputFactory;
import com.badlogic.gdx.backends.android.AndroidNet;
import com.badlogic.gdx.backends.android.AndroidPreferences;
import com.badlogic.gdx.backends.android.CastAndroidInterface;
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy;
import com.badlogic.gdx.backends.android.surfaceview.GLSurfaceView20;
import com.badlogic.gdx.backends.android.surfaceview.GLSurfaceView20API18;
import com.badlogic.gdx.backends.android.surfaceview.GdxEglConfigChooser;
import com.badlogic.gdx.backends.android.surfaceview.ResolutionStrategy;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.android.gms.cast.CastPresentation;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.bluetooth.base.BluetoothDevice;
import org.catrobat.catroid.bluetooth.base.BluetoothDeviceService;
import org.catrobat.catroid.camera.CameraManager;
import org.catrobat.catroid.common.CatroidService;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.common.ServiceProvider;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.drone.DroneInitializer;
import org.catrobat.catroid.facedetection.FaceDetectionHandler;
import org.catrobat.catroid.formulaeditor.SensorHandler;
import org.catrobat.catroid.io.StageAudioFocus;
import org.catrobat.catroid.stage.DroneConnection;
import org.catrobat.catroid.stage.OnUtteranceCompletedListenerContainer;
import org.catrobat.catroid.stage.PreStageActivity;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.stage.StageListener;
import org.catrobat.catroid.ui.dialogs.CustomAlertDialogBuilder;
import org.catrobat.catroid.ui.dialogs.StageDialog;
import org.catrobat.catroid.utils.LedUtil;
import org.catrobat.catroid.utils.ToastUtil;
import org.catrobat.catroid.utils.VibratorUtil;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Service to keep the remote display running even when the app goes into the background
 */
public class PresentationService extends CastAndroidInterface {

    private static final String TAG = "PresentationService";

    // First screen
    private CastPresentation mPresentation;
    public AndroidGraphics mGgraphics;
    public AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

    private static final int REQUEST_CONNECT_DEVICE = 1000;
    public static final int REQUEST_RESOURCES_INIT = 101;
    public static final int REQUEST_TEXT_TO_SPEECH = 10;

    private int requiredResourceCounter;

    private static TextToSpeech textToSpeech;
    private static OnUtteranceCompletedListenerContainer onUtteranceCompletedListenerContainer;

    private DroneInitializer droneInitializer = null;

    public static StageListener stageListener;
    private boolean resizePossible;
    private StageDialog stageDialog;

    private DroneConnection droneConnection = null;

    public static final int STAGE_ACTIVITY_FINISH = 7777;

    private StageAudioFocus stageAudioFocus;

    @Override
    public void onCreate() {
        super.onCreate();

        int requiredResources = ProjectManager.getInstance().getCurrentProject().getRequiredResources();
        requiredResourceCounter = Integer.bitCount(requiredResources);

        if ((requiredResources & Brick.TEXT_TO_SPEECH) > 0) {
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        }

        if ((requiredResources & Brick.BLUETOOTH_LEGO_NXT) > 0) {
            connectBTDevice(BluetoothDevice.LEGO_NXT);
        }

        if ((requiredResources & Brick.BLUETOOTH_PHIRO) > 0) {
            connectBTDevice(BluetoothDevice.PHIRO);
        }

        if ((requiredResources & Brick.ARDRONE_SUPPORT) > 0) {
            droneInitializer = getDroneInitializer();
            droneInitializer.initialise();
        }

        FaceDetectionHandler.resetFaceDedection();
        if ((requiredResources & Brick.FACE_DETECTION) > 0) {
            boolean success = FaceDetectionHandler.startFaceDetection(this);
            if (success) {
                resourceInitialized();
            } else {
                resourceFailed();
            }
        }

        if ((requiredResources & Brick.CAMERA_LED ) > 0) {
            if (!CameraManager.getInstance().isFacingBack()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.led_and_front_camera_warning)).setCancelable(false)
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                ledInitialize();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                ledInitialize();
            }
        }

        if ((requiredResources & Brick.VIBRATOR) > 0) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                requiredResourceCounter--;
                VibratorUtil.setContext(this.getBaseContext());
                VibratorUtil.activateVibratorThread();
            } else {
                ToastUtil.showError(this, R.string.no_vibrator_available);
                resourceFailed();
            }
        }

        if (requiredResourceCounter == Brick.NO_RESOURCES) {
            startStage();
        }

        int virtualScreenWidth = ProjectManager.getInstance().getCurrentProject().getXmlHeader().virtualScreenWidth;
        int virtualScreenHeight = ProjectManager.getInstance().getCurrentProject().getXmlHeader().virtualScreenHeight;
//        if (virtualScreenHeight > virtualScreenWidth) {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        } else {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        }
//
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        if (getIntent().getBooleanExtra(DroneInitializer.INIT_DRONE_STRING_EXTRA, false)) {
//            droneConnection = new DroneConnection(this);
//        }
        stageListener = new StageListener();
        //stageDialog = new StageDialog(this, stageListener, R.style.stage_dialog);
        calculateScreenSizes();

        //ProjectManager.getInstance().view = initializeForView(stageListener, new AndroidApplicationConfiguration());
        //initialize(stageListener, new AndroidApplicationConfiguration());

        mGgraphics = new AndroidGraphics(this, new AndroidApplicationConfiguration(), config.resolutionStrategy == null ? new FillResolutionStrategy()
                : config.resolutionStrategy);

        Gdx.graphics = mGgraphics;


        initialize((StageListener) listener);

        if (droneConnection != null) {
            try {
                droneConnection.initialise();
            } catch (RuntimeException runtimeException) {
                Log.e(TAG, "Failure during drone service startup", runtimeException);
                ToastUtil.showError(this, R.string.error_no_drone_connected);
            }
        }

        ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE).initialise();

        stageAudioFocus = new StageAudioFocus(this);
    }

    public void onBackPressed() {
        //pause();
        stageDialog.show();
    }

    public void manageLoadAndFinish() {
        stageListener.pause();
        stageListener.finish();

        PreStageActivity.shutdownResources();
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

    private void createPresentation(Display display) {
        dismissPresentation();

//        mGgraphics = new AndroidGraphics(this, new AndroidApplicationConfiguration(), config.resolutionStrategy == null ? new FillResolutionStrategy()
//                : config.resolutionStrategy);
//
//        StageListener listener = new StageListener();
//        initialize(listener);

        mPresentation = new FirstScreenPresentation(this, display, mGgraphics);

        try {
            mPresentation.show();
        } catch (WindowManager.InvalidDisplayException ex) {
            Log.e(TAG, "Unable to show presentation, display was removed.", ex);
            dismissPresentation();
        }
    }

    protected View createGLSurfaceView (final ResolutionStrategy resolutionStrategy) {

        GLSurfaceView.EGLConfigChooser configChooser = getEglConfigChooser();
        int sdkVersion = android.os.Build.VERSION.SDK_INT;
        if (sdkVersion <= 10 && config.useGLSurfaceView20API18) {
            GLSurfaceView20API18 view = new GLSurfaceView20API18(getApplicationContext(), resolutionStrategy);
            if (configChooser != null)
                view.setEGLConfigChooser(configChooser);
            else
                view.setEGLConfigChooser(config.r, config.g, config.b, config.a, config.depth, config.stencil);
            view.setRenderer(mGgraphics);
            return view;
        } else {
            GLSurfaceView20 view = new GLSurfaceView20(getApplicationContext(), resolutionStrategy);
            if (configChooser != null)
                view.setEGLConfigChooser(configChooser);
            else
                view.setEGLConfigChooser(config.r, config.g, config.b, config.a, config.depth, config.stencil);
            view.setRenderer(mGgraphics);
            return view;
        }
    }
    protected GLSurfaceView.EGLConfigChooser getEglConfigChooser () {
        return new GdxEglConfigChooser(config.r, config.g, config.b, config.a, config.depth, config.stencil, config.numSamples);
    }

    static {
        GdxNativesLoader.load();
    }


    private void connectBTDevice(Class<? extends BluetoothDevice> service) {
        BluetoothDeviceService btService = ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE);

        /*if (btService.connectDevice(service, getAppl, REQUEST_CONNECT_DEVICE)
                == BluetoothDeviceService.ConnectDeviceResult.ALREADY_CONNECTED) {
            resourceInitialized();
        }*/
    }

    public DroneInitializer getDroneInitializer() {
//        if (droneInitializer == null) {
//            droneInitializer = new DroneInitializer(this, returnToActivityIntent);
//        }
        return droneInitializer;
    }

    protected boolean hasFlash() {
        boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        boolean hasLed = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (!hasCamera || !hasLed) {
            return false;
        }

        Camera camera = CameraManager.getInstance().getCamera();

        try {
            if (camera == null) {
                camera = CameraManager.getInstance().getCamera();
            }
        } catch (Exception exception) {
            Log.e(TAG, "failed to open Camera", exception);
        }

        if (camera == null) {
            return false;
        }

        Camera.Parameters parameters = camera.getParameters();

        if (parameters.getFlashMode() == null) {
            return false;
        }

        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        if (supportedFlashModes == null || supportedFlashModes.isEmpty() ||
                supportedFlashModes.size() == 1 && supportedFlashModes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF)) {
            return false;
        }

        return true;
    }

    @Override
    public void onResume() {
        if (droneInitializer != null) {
            droneInitializer.onPrestageActivityResume();
        }
        SensorHandler.startSensorListener(this);
        stageListener.activityResume();
        stageListener.menuResume();
        stageAudioFocus.requestAudioFocus();
        LedUtil.resumeLed();
        VibratorUtil.resumeVibrator();
        super.onResume();

        if (droneConnection != null) {
            droneConnection.start();
        }

        ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE).start();


//        stageListener.menuResume();
//        LedUtil.resumeLed();
//        VibratorUtil.resumeVibrator();
//        SensorHandler.startSensorListener(this);
//        FaceDetectionHandler.startFaceDetection(this);
//
//        ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE).start();
    }

    @Override
    protected void onPause() {
        if (droneInitializer != null) {
            droneInitializer.onPrestageActivityPause();
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (droneInitializer != null) {
            droneInitializer.onPrestageActivityDestroy();
        }

        super.onDestroy();
    }

    //all resources that should be reinitialized with every stage start
    public static void shutdownResources() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE).pause();

        if (FaceDetectionHandler.isFaceDetectionRunning()) {
            FaceDetectionHandler.stopFaceDetection();
        }
    }

    //all resources that should not have to be reinitialized every stage start
    public static void shutdownPersistentResources() {

        ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE).disconnectDevices();

        deleteSpeechFiles();
        if (LedUtil.isActive()) {
            LedUtil.destroy();
        }
        if (VibratorUtil.isActive()) {
            VibratorUtil.destroy();
        }
    }

    private static void deleteSpeechFiles() {
        File pathToSpeechFiles = new File(Constants.TEXT_TO_SPEECH_TMP_PATH);
        if (pathToSpeechFiles.isDirectory()) {
            for (File file : pathToSpeechFiles.listFiles()) {
                file.delete();
            }
        }
    }

    public void resourceFailed() {
        //setResult(RESULT_CANCELED, returnToActivityIntent);
        //finish();
    }

    public synchronized void resourceInitialized() {
        requiredResourceCounter--;
        if (requiredResourceCounter == 0) {
            Log.d(TAG, "Start Stage");

            startStage();
        }
    }

    public void startStage() {
        //setResult(RESULT_OK, returnToActivityIntent);
        //finish();


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("bt", "requestcode " + requestCode + " result code" + resultCode);

        switch (requestCode) {

            case REQUEST_CONNECT_DEVICE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        resourceInitialized();
                        break;

                    case Activity.RESULT_CANCELED:
                        resourceFailed();
                        break;
                }
                break;

            case REQUEST_TEXT_TO_SPEECH:
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            onUtteranceCompletedListenerContainer = new OnUtteranceCompletedListenerContainer();
                            textToSpeech.setOnUtteranceCompletedListener(onUtteranceCompletedListenerContainer);
                            resourceInitialized();
                            if (status == TextToSpeech.ERROR) {
                                //ToastUtil.showError(this, "Error occurred while initializing Text-To-Speech engine");
                                resourceFailed();
                            }
                        }
                    });
                    if (textToSpeech.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_MISSING_DATA) {
                        Intent installIntent = new Intent();
                        installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        startActivity(installIntent);
                        resourceFailed();
                    }
                } else {
                    AlertDialog.Builder builder = new CustomAlertDialogBuilder(this);
                    builder.setMessage(R.string.text_to_speech_engine_not_installed).setCancelable(false)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent installIntent = new Intent();
                                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                                    startActivity(installIntent);
                                    resourceFailed();
                                }
                            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            resourceFailed();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                break;
            default:
                resourceFailed();
                break;
        }
    }

    public static void textToSpeech(String text, File speechFile, TextToSpeech.OnUtteranceCompletedListener listener,
                                    HashMap<String, String> speakParameter) {
        if (text == null) {
            text = "";
        }

        if (onUtteranceCompletedListenerContainer.addOnUtteranceCompletedListener(speechFile, listener,
                speakParameter.get(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))) {
            int status = textToSpeech.synthesizeToFile(text, speakParameter, speechFile.getAbsolutePath());
            if (status == TextToSpeech.ERROR) {
                Log.e(TAG, "File synthesizing failed");
            }
        }
    }

    private void ledInitialize() {
        if ( hasFlash() ) {
            resourceInitialized();
            LedUtil.activateLedThread();
        } else {
            //ToastUtil.showError(PreStageActivity.this, R.string.no_flash_led_available);
            resourceFailed();
        }
    }


    public boolean getResizePossible() {
        return resizePossible;
    }

    private void calculateScreenSizes() {
        int virtualScreenWidth = ProjectManager.getInstance().getCurrentProject().getXmlHeader().virtualScreenWidth;
        int virtualScreenHeight = ProjectManager.getInstance().getCurrentProject().getXmlHeader().virtualScreenHeight;
        if (virtualScreenHeight > virtualScreenWidth) {
            ifLandscapeSwitchWidthAndHeight();
        } else {
            ifPortraitSwitchWidthAndHeight();
        }
        float aspectRatio = (float) virtualScreenWidth / (float) virtualScreenHeight;
        float screenAspectRatio = ScreenValues.getAspectRatio();

        if ((virtualScreenWidth == ScreenValues.SCREEN_WIDTH && virtualScreenHeight == ScreenValues.SCREEN_HEIGHT)
                || Float.compare(screenAspectRatio, aspectRatio) == 0) {
            resizePossible = false;
            stageListener.maximizeViewPortWidth = ScreenValues.SCREEN_WIDTH;
            stageListener.maximizeViewPortHeight = ScreenValues.SCREEN_HEIGHT;
            return;
        }

        resizePossible = true;

        float scale = 1f;
        float ratioHeight = (float) ScreenValues.SCREEN_HEIGHT / (float) virtualScreenHeight;
        float ratioWidth = (float) ScreenValues.SCREEN_WIDTH / (float) virtualScreenWidth;

        if (aspectRatio < screenAspectRatio) {
            scale = ratioHeight / ratioWidth;
            stageListener.maximizeViewPortWidth = (int) (ScreenValues.SCREEN_WIDTH * scale);
            stageListener.maximizeViewPortX = (int) ((ScreenValues.SCREEN_WIDTH - stageListener.maximizeViewPortWidth) / 2f);
            stageListener.maximizeViewPortHeight = ScreenValues.SCREEN_HEIGHT;

        } else if (aspectRatio > screenAspectRatio) {
            scale = ratioWidth / ratioHeight;
            stageListener.maximizeViewPortHeight = (int) (ScreenValues.SCREEN_HEIGHT * scale);
            stageListener.maximizeViewPortY = (int) ((ScreenValues.SCREEN_HEIGHT - stageListener.maximizeViewPortHeight) / 2f);
            stageListener.maximizeViewPortWidth = ScreenValues.SCREEN_WIDTH;
        }
    }

    private void ifLandscapeSwitchWidthAndHeight() {
        if (ScreenValues.SCREEN_WIDTH > ScreenValues.SCREEN_HEIGHT) {
            int tmp = ScreenValues.SCREEN_HEIGHT;
            ScreenValues.SCREEN_HEIGHT = ScreenValues.SCREEN_WIDTH;
            ScreenValues.SCREEN_WIDTH = tmp;
        }
    }

    private void ifPortraitSwitchWidthAndHeight() {
        if (ScreenValues.SCREEN_WIDTH < ScreenValues.SCREEN_HEIGHT) {
            int tmp = ScreenValues.SCREEN_HEIGHT;
            ScreenValues.SCREEN_HEIGHT = ScreenValues.SCREEN_WIDTH;
            ScreenValues.SCREEN_WIDTH = tmp;
        }
    }

    @Override
    public ApplicationListener getApplicationListener() {
        return stageListener;
    }

    @Override
    public void log(String tag, String message, Throwable exception) {
        Log.d(tag, message, exception);
    }

    @Override
    public int getLogLevel() {
        return 0;
    }



    /**
     * The presentation to show on the first screen (the TV).
     * <p>
     * Note that this display may have different metrics from the display on
     * which the main activity is showing so we must be careful to use the
     * presentation's own {@link Context} whenever we load resources.
     * </p>
     */
    public class FirstScreenPresentation extends CastPresentation {

        private final String TAG = "FirstScreenPresentation";
        //private final View mView;
        private final AndroidGraphics mGraphics;
        private GLSurfaceView firstScreenSurfaceView;

        //public View mView = (AndroidGraphics) ProjectManager.getInstance().gdxCast.graphics;
        //public AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

        public FirstScreenPresentation(Context context, Display display, AndroidGraphics graphics) {
            super(context, display);
            this.mGraphics = graphics;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

//            // Create the layout
//            RelativeLayout layout = new RelativeLayout(getApplication());
//
//            // Add the libgdx view
//            layout.addView(mView);
//
//            // Hook it all up
//            setContentView(layout);

            ProjectManager.getInstance().presentation = this;

            setContentView(R.layout.first_screen_layout);

            TextView titleTextView = (TextView) findViewById(R.id.title);

            firstScreenSurfaceView = (GLSurfaceView) findViewById(R.id.surface_view);
            // Create an OpenGL ES 2.0 context.
            firstScreenSurfaceView.setEGLContextClientVersion(2);
            // Allow UI elements above this surface; used for text overlay
            firstScreenSurfaceView.setZOrderMediaOverlay(true);
            // Enable anti-aliasing
            //firstScreenSurfaceView.setEGLConfigChooser(new AndroidApplicationConfiguration());
            //mCubeRenderer = new com.example.castremotedisplay.CubeRenderer();
            firstScreenSurfaceView.setRenderer((AndroidGraphics) mGraphics);
        }
    }
}
