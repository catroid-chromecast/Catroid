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
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.opengl.GLSurfaceView;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.stage.StageListener;

import java.lang.reflect.Method;

/**
 * Service to keep the remote display running even when the app goes into the background
 */
public class PresentationService extends CastAndroidInterface {

    private static final String TAG = "PresentationService";

    // First screen
    private CastPresentation mPresentation;
    public AndroidGraphics mGgraphics;
    public AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

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

    private void createPresentation(Display display) {
        dismissPresentation();

        mGgraphics = new AndroidGraphics(this, new AndroidApplicationConfiguration(), config.resolutionStrategy == null ? new FillResolutionStrategy()
                : config.resolutionStrategy);

        StageListener listener = new StageListener();
        initialize(listener);

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

        public void setRenderer() {
            firstScreenSurfaceView.setRenderer((GLSurfaceView.Renderer) ProjectManager.getInstance().gdxDevice.graphics);
        }
    }
}
