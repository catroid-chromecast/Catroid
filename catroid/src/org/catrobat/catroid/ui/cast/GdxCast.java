package org.catrobat.catroid.ui.cast;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.backends.android.AndroidGL20;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.profiling.GL20Profiler;

/**
 * Created by Paul on 09.07.2015.
 */
public class GdxCast {
    public Application app;
    public Graphics graphics;
    public Audio audio;
    public Input input;
    public Files files;
    public Net net;

    public GL20 gl;
    public GL20 gl20;
    public GL30 gl30;

    public GdxCast() {
        this.gl20 = new AndroidGL20();
        this.gl = this.gl20;
    }
}