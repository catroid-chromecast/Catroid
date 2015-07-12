package com.badlogic.gdx.backends.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.stage.StageListener;

import java.lang.reflect.Method;

/**
 * Created by Paul on 10.07.2015.
 */
public class CastAndroidInterface extends CastRemoteDisplayLocalService implements  AndroidApplicationBase{
    static {
        GdxNativesLoader.load();
    }

    protected AndroidGraphics graphics;
    protected AndroidInput input;
    protected AndroidAudio audio;
    protected AndroidFiles files;
    protected AndroidNet net;
    protected ApplicationListener listener;
    public Handler handler;
    protected boolean firstResume = true;
    protected final Array<Runnable> runnables = new Array<Runnable>();
    protected final Array<Runnable> executedRunnables = new Array<Runnable>();
    protected final Array<LifecycleListener> lifecycleListeners = new Array<LifecycleListener>();
    private final Array<AndroidEventListener> androidEventListeners = new Array<AndroidEventListener>();
    protected boolean useImmersiveMode = false;
    protected boolean hideStatusBar = false;
    private int wasFocusChanged = -1;
    private boolean isWaitingForAudio = false;

    public void initialize (StageListener listener) {
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        init(listener, config, false);

//        ProjectManager.getInstance().gdxCast.app = Gdx.app;
//        ProjectManager.getInstance().gdxCast.audio = Gdx.audio;
//        ProjectManager.getInstance().gdxCast.files = Gdx.files;
//        //ProjectManager.getInstance().gdxCast.gl = Gdx.gl;
//        //ProjectManager.getInstance().gdxCast.gl20 = Gdx.gl20;
//        ProjectManager.getInstance().gdxCast.gl30 = Gdx.gl30;
//        ProjectManager.getInstance().gdxCast.graphics = Gdx.graphics;
//        ProjectManager.getInstance().gdxCast.input = Gdx.input;
//        ProjectManager.getInstance().gdxCast.net = Gdx.net;
    }

    /** This method has to be called in the {@link Activity#onCreate(Bundle)} method. It sets up all the things necessary to get
     * input, render via OpenGL and so on. Uses a default {@link AndroidApplicationConfiguration}.
     * <p>
     * Note: you have to add the returned view to your layout!
     *
     * @param listener the {@link ApplicationListener} implementing the program logic
     * @return the GLSurfaceView of the application */
    public View initializeForView (ApplicationListener listener) {
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        return initializeForView(listener, config);
    }

    /** This method has to be called in the {@link Activity#onCreate(Bundle)} method. It sets up all the things necessary to get
     * input, render via OpenGL and so on. You can configure other aspects of the application with the rest of the fields in the
     * {@link AndroidApplicationConfiguration} instance.
     * <p>
     * Note: you have to add the returned view to your layout!
     *
     * @param listener the {@link ApplicationListener} implementing the program logic
     * @param config the {@link AndroidApplicationConfiguration}, defining various settings of the application (use accelerometer,
     *           etc.).
     * @return the GLSurfaceView of the application */
    public View initializeForView (ApplicationListener listener, AndroidApplicationConfiguration config) {
        init(listener, config, true);
        return graphics.getView();
    }

    private void init (ApplicationListener listener, AndroidApplicationConfiguration config, boolean isForView) {
        if (this.getVersion() < MINIMUM_SDK) {
            throw new GdxRuntimeException("LibGDX requires Android API Level " + MINIMUM_SDK + " or later.");
        }
        graphics = new AndroidGraphics(this, config, config.resolutionStrategy == null ? new FillResolutionStrategy()
                : config.resolutionStrategy);
        input = AndroidInputFactory.newAndroidInput(this, this, graphics.view, config);
        audio = new AndroidAudio(this, config);
        this.getFilesDir(); // workaround for Android bug #10515463
        files = new AndroidFiles(this.getAssets(), this.getFilesDir().getAbsolutePath());
        net = new AndroidNet(this);
        this.listener = listener;
        this.handler = new Handler();
        this.useImmersiveMode = config.useImmersiveMode;
        this.hideStatusBar = config.hideStatusBar;

        Gdx.app = this;
        Gdx.input = this.getInput();
        Gdx.audio = this.getAudio();
        Gdx.files = this.getFiles();
        Gdx.graphics = this.getGraphics();
        Gdx.net = this.getNet();

//        if (!isForView) {
//            try {
//                requestWindowFeature(Window.FEATURE_NO_TITLE);
//            } catch (Exception ex) {
//                log("AndroidApplication", "Content already displayed, cannot request FEATURE_NO_TITLE", ex);
//            }
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
//            setContentView(graphics.getView(), createLayoutParams());
//        }

        useImmersiveMode(this.useImmersiveMode);
        if (this.useImmersiveMode && getVersion() >= Build.VERSION_CODES.KITKAT) {
            try {
                Class<?> vlistener = Class.forName("com.badlogic.gdx.backends.android.AndroidVisibilityListener");
                Object o = vlistener.newInstance();
                Method method = vlistener.getDeclaredMethod("createListener", AndroidApplicationBase.class);
                method.invoke(o, this);
            } catch (Exception e) {
                log("AndroidApplication", "Failed to create AndroidVisibilityListener", e);
            }
        }
    }

    protected FrameLayout.LayoutParams createLayoutParams () {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        return layoutParams;
    }

    //@Override
    protected void onPause () {
        boolean isContinuous = graphics.isContinuousRendering();
        boolean isContinuousEnforced = AndroidGraphics.enforceContinuousRendering;

        // from here we don't want non continuous rendering
        AndroidGraphics.enforceContinuousRendering = true;
        graphics.setContinuousRendering(true);
        // calls to setContinuousRendering(false) from other thread (ex: GLThread)
        // will be ignored at this point...
        graphics.pause();

        input.onPause();

//        if (isFinishing()) {
//            graphics.clearManagedCaches();
//            graphics.destroy();
//        }

        AndroidGraphics.enforceContinuousRendering = isContinuousEnforced;
        graphics.setContinuousRendering(isContinuous);

        graphics.onPauseGLSurfaceView();
    }

    //@Override
    protected void onResume () {
        Gdx.app = this;
        Gdx.input = this.getInput();
        Gdx.audio = this.getAudio();
        Gdx.files = this.getFiles();
        Gdx.graphics = this.getGraphics();
        Gdx.net = this.getNet();

        input.onResume();

        if (graphics != null) {
            graphics.onResumeGLSurfaceView();
        }

        if (!firstResume) {
            graphics.resume();
        } else
            firstResume = false;

        this.isWaitingForAudio = true;
        if (this.wasFocusChanged == 1 || this.wasFocusChanged == -1) {
            this.audio.resume();
            this.isWaitingForAudio = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public ApplicationListener getApplicationListener () {
        return listener;
    }

    @Override
    public Audio getAudio () {
        return audio;
    }

    @Override
    public Files getFiles () {
        return files;
    }

    @Override
    public Graphics getGraphics () {
        return graphics;
    }

    @Override
    public AndroidInput getInput () {
        return input;
    }

    @Override
    public Net getNet () {
        return net;
    }

    @Override
    public Application.ApplicationType getType () {
        return Application.ApplicationType.Android;
    }

    @Override
    public int getVersion () {
        return android.os.Build.VERSION.SDK_INT;
    }

    @Override
    public long getJavaHeap () {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    @Override
    public long getNativeHeap () {
        return Debug.getNativeHeapAllocatedSize();
    }

    @Override
    public Preferences getPreferences (String name) {
        return new AndroidPreferences(getSharedPreferences(name, Context.MODE_PRIVATE));
    }

    AndroidClipboard clipboard;

    @Override
    public Clipboard getClipboard () {
        if (clipboard == null) {
            clipboard = new AndroidClipboard(this);
        }
        return clipboard;
    }


    @Override
    public void postRunnable (Runnable runnable) {
        synchronized (runnables) {
            runnables.add(runnable);
            Gdx.graphics.requestRendering();

        }
    }

    @Override
    public void onConfigurationChanged (Configuration config) {
        super.onConfigurationChanged(config);
        boolean keyboardAvailable = false;
        if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) keyboardAvailable = true;
        input.keyboardAvailable = keyboardAvailable;
    }

    @Override
    public void exit () {
//        handler.post(new Runnable() {
//            @Override
//            public void run () {
//                AndroidApplication.this.finish();
//            }
//        });
    }

    @Override
    public void debug (String tag, String message) {
            Log.d(tag, message);
    }

    @Override
    public void debug (String tag, String message, Throwable exception) {

            Log.d(tag, message, exception);
    }

    @Override
    public void log (String tag, String message) {

    }

    @Override
    public void log (String tag, String message, Throwable exception) {

    }

    @Override
    public void error (String tag, String message) {

    }

    @Override
    public void error (String tag, String message, Throwable exception) {

    }

    @Override
    public void setLogLevel (int logLevel) {

    }

    @Override
    public int getLogLevel () {
        return 0;
    }

    @Override
    public void addLifecycleListener (LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.add(listener);
        }
    }

    @Override
    public void removeLifecycleListener (LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.removeValue(listener, true);
        }
    }

    //@Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);

        // forward events to our listeners if there are any installed
        synchronized (androidEventListeners) {
            for (int i = 0; i < androidEventListeners.size; i++) {
                androidEventListeners.get(i).onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    /** Adds an event listener for Android specific event such as onActivityResult(...). */
    public void addAndroidEventListener (AndroidEventListener listener) {
        synchronized (androidEventListeners) {
            androidEventListeners.add(listener);
        }
    }

    /** Removes an event listener for Android specific event such as onActivityResult(...). */
    public void removeAndroidEventListener (AndroidEventListener listener) {
        synchronized (androidEventListeners) {
            androidEventListeners.removeValue(listener, true);
        }
    }

    @Override
    public Context getContext () {
        return this;
    }

    @Override
    public Array<Runnable> getRunnables () {
        return runnables;
    }

    @Override
    public Array<Runnable> getExecutedRunnables () {
        return executedRunnables;
    }

    @Override
    public void runOnUiThread(Runnable runnable) {

    }

    @Override
    public Array<LifecycleListener> getLifecycleListeners () {
        return lifecycleListeners;
    }

    @Override
    public Window getApplicationWindow () {
        return null;
    }

    @Override
    public WindowManager getWindowManager() {
        return (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public void useImmersiveMode(boolean b) {

    }

    @Override
    public Handler getHandler () {
        return this.handler;
    }

    @Override
    public void onCreatePresentation(Display display) {

    }

    @Override
    public void onDismissPresentation() {

    }
}
