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

import org.catrobat.catroid.ProjectManager;

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

    public void setGdx() {
        Gdx.app = this.app;
        //Gdx.audio = this.audio;
        //Gdx.files = this.files;
        Gdx.gl = this.gl;
        Gdx.gl20 = this.gl20;
        Gdx.gl30 = this.gl30;
        Gdx.graphics = this.graphics;
        //Gdx.input = this.input;
        //Gdx.net = this.net;
    }

    public void setMgdxFromGdx() {
        this.app = Gdx.app;
        this.audio = Gdx.audio;
        this.files = Gdx.files;
        //this.gl = Gdx.gl;
        //this.gl20 = Gdx.gl20;
        this.gl30 = Gdx.gl30;
        this.graphics = Gdx.graphics;
        this.input = Gdx.input;
        this.net = Gdx.net;
    }
}