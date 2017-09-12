package com.vrspatialaudiodemoandroid.ihimu.vrspatialaudiodemoandroid;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.ext.gvr.GvrAudioProcessor;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.vr.sdk.base.GvrActivity;
import net.protyposis.android.spectaculum.InputSurfaceHolder;
import net.protyposis.android.spectaculum.SpectaculumView;
import net.protyposis.android.spectaculum.effects.ImmersiveEffect;
import net.protyposis.android.spectaculum.effects.ImmersiveTouchNavigation;


public class StereoscopicView extends GvrActivity implements InputSurfaceHolder.Callback, View.OnClickListener {

    private static SpectaculumView mSpectaculumView;
    private PlaybackControlView mPlaybackControlView;
    private GvrAudioProcessor gvrAudioProcessor;
    private Context context;
    private String userAgent;
    private Button switchToMono;
    private float mPanX;
    private float mPanY;
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private GestureDetector mGestureDetector;

    private SimpleExoPlayer mExoPlayer;

    private final VideoSource[] mVideoSources = {
            // Orion360 Test Video: http://www.finwe.mobi/main/360-degree/orion360-test-images-videos/
            // Url extracted from https://littlstar.com/videos/c9c27ffc
            // Video format not supported on Nexus 9
            new VideoSource("https://360.littlstar.com/production/79f8bd2f-d137-46ac-a60a-f9b22f77b57d/download.mp4", ImmersiveEffect.Mode.MONO),
            // National Geographic Virtual Yellowstone: https://littlstar.com/videos/b541e0f4
            new VideoSource("https://360.littlstar.com/production/83ef40fe-6e8e-45ed-86f8-871c89c3a60f/download.mp4", ImmersiveEffect.Mode.MONO),
            // House Stereo Side-By-Side Demo
            // This video has a really low resolution but should play back on all devices
            new VideoSource("http://hosting.360heros.com/3D360Video/3D360/Demo3-House/3DH-Take1-Side-By-Side-1920x960.mp4", ImmersiveEffect.Mode.STEREO_SBS),
            //Audio test URL (Set to Mono to do Monocular view)
            new VideoSource("http://www.columbia.edu/~iuu1/pearl.webm", ImmersiveEffect.Mode.STEREO_SBS)
    };

    // SELECT THE VIDEO SOURCE HERE! (an index of the VideoSource array above)
    private int mSelectedVideoSource = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stereoscopic);

        //Load VR Audio library
        //System.loadLibrary("vraudio_engine");

        mPanX=0;
        mPanY=0;

        this.context = this;
        userAgent = Util.getUserAgent(context, "VideoExoPlayer");

        switchToMono = (Button) findViewById(R.id.switchtomono);
        switchToMono.setOnClickListener(this);

        // Get view references from layout
        mSpectaculumView = (SpectaculumView) findViewById(R.id.spectaculumview);

        mPlaybackControlView = (PlaybackControlView) findViewById(R.id.playbackcontrolview);

        // Register callbacks to initialize and release player
        mSpectaculumView.getInputHolder().addCallback(this);

        // Set the playback control view duration to as long as possible so we do not have to
        // handle view visibility toggling in this example.
        //mPlaybackControlView.setShowDurationMs(Integer.MAX_VALUE);
        mPlaybackControlView.setShowTimeoutMs(Integer.MAX_VALUE);

        // Setup Spectaculum view for immersive content
        ImmersiveEffect immersiveEffect = new ImmersiveEffect(); // create effect instance
        immersiveEffect.setMode(mVideoSources[mSelectedVideoSource].immersiveMode); // Set VR the mode for selected video source
        mSpectaculumView.addEffect(immersiveEffect); // add effect to view

        // Setup Spectaculum immersive viewport touch navigation
        ImmersiveTouchNavigation immersiveTouchNavigation = new ImmersiveTouchNavigation(mSpectaculumView);
        immersiveTouchNavigation.attachTo(immersiveEffect);
        immersiveTouchNavigation.activate(); // enable touch navigation/**/

        // Setup Spectaculum immersive viewport sensor navigation (highly experimental! does not work well together with touch navigation!)
        /*ImmersiveSensorNavigation immersiveSensorNavigation = new ImmersiveSensorNavigation(this);
        immersiveSensorNavigation.attachTo(immersiveEffect);
        immersiveSensorNavigation.activate();*/

        //Add gesture listener to view.

        mGestureDetector = new GestureDetector(mSpectaculumView.getContext(), mOnGestureListener);

        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // pass the events to the gesture detector
                // a return value of true means the detector is handling it
                // a return value of false means the detector didn't recognize the event
                return mGestureDetector.onTouchEvent(event);

            }
        };

        //Don't do. Overrides ImmersiveTouch.
        //mSpectaculumView.setOnTouchListener(touchListener);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.switchtomono:
                intent = new Intent(this, MonoscopicView.class);
                startActivity(intent);
                finish();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Release player to avoid writing frames to an invalid Spectaculum surface
        releasePlayer();

        // Pause Spectaculum rendering while it's inactive
        mSpectaculumView.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();

        // Resume Spectaculum rendering
        mSpectaculumView.onResume();
    }

    @Override
    public void surfaceCreated(InputSurfaceHolder holder) {
        // When the input surface (i.e. the Spectaculum surface that the player needs to write video frames)
        // has been successfully created, we can initialize the player and activate the shader effect
        initializePlayer();
        mSpectaculumView.selectEffect(0); // activate effect
    }

    @Override
    public void surfaceDestroyed(InputSurfaceHolder holder) {
        // When the input surface is gone, we cannot display video frames any more and release the player
        releasePlayer();
    }

    private GestureDetector.SimpleOnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // Scale the scroll/panning distance to rotation degrees
            // The view's with and height are mapped to 180 degree each
            // TODO map motion event positions from view to the rendered sphere and derive rotation
            //      angles to keep touchscreen positions and sphere positions in sync
            mPanX += distanceX / mSpectaculumView.getWidth() * 180f;
            mPanY += distanceY / mSpectaculumView.getHeight() * 180f;

            // Clamp horizontal rotation to avoid rotations beyond 90 degree which inverts the vertical
            // rotation and makes rotation handling more complicated
            mPanY = clamp(mPanY, -90, 90);

            // Apply the panning to the viewport
            // Horizontal panning along the view's X axis translates to a rotation around the viewport's Y axis
            // Vertical panning along the view's Y axis translates to a rotation around the viewport's X axis
            //setRotation(-mPanY, -mPanX);
            Log.i("Demo","("+Float.toString(mPanX)+", "+Float.toString(mPanY)+")");
            gvrAudioProcessor.updateOrientation(0, mPanX, mPanY, 0);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Reset rotation/viewport to initial value
            mPanX = 0;
            mPanY = 0;
            return true;
        }
    };

    /**
     * Create an initialize an ExoPlayer instance that is ready for playback.
     */
    private void initializePlayer() {
        /*
         * Creating the player
         * Code taken from docs and simplified
         * https://google.github.io/ExoPlayer/guide.html#creating-the-player
         */

        SimpleExoPlayer player;

        MediaSource videoSource;

        LoadControl loadControl = new DefaultLoadControl();

        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        gvrAudioProcessor = new GvrAudioProcessor();

        GvrRenderersFactory soundRenderer=new GvrRenderersFactory(this,null,gvrAudioProcessor);
        player = ExoPlayerFactory.newSimpleInstance(soundRenderer,trackSelector);

        // Auto play the video.
        player.setPlayWhenReady(true);

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "SpectaculumImmersiveSample"));
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        //Local file playback code.
            /*DataSpec dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(R.raw.pearl));
            final RawResourceDataSource rawResourceDataSource = new RawResourceDataSource(context);

            try
            {
                rawResourceDataSource.open(dataSpec);
            }
            catch (RawResourceDataSource.RawResourceDataSourceException e)
            {
                //Report error.
                Toast.makeText(StereoscopicView.this, R.string.datasourceerror, Toast.LENGTH_LONG).show();
            }

            DataSource.Factory factory = new DataSource.Factory() {
                @Override
                public DataSource createDataSource() {
                    return rawResourceDataSource;
                }
            };

            videoSource = new ExtractorMediaSource(rawResourceDataSource.getUri(),
                    factory, extractorsFactory, null, null);*/

        videoSource = new ExtractorMediaSource(Uri.parse(mVideoSources[mSelectedVideoSource].url),
                dataSourceFactory, extractorsFactory, null, null);

        player.prepare(videoSource, true, false);



        mPlaybackControlView.setPlayer(player);

        /*
         * Configure player for SpectaculumView
         */
        // Set Spectaculum view as playback surface
        player.setVideoSurface(mSpectaculumView.getInputHolder().getSurface());
        // Attach listener to listen to video size changed events
        player.setVideoListener(new SimpleExoPlayer.VideoListener() {
            @Override
            public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
                // When the video size changes, we update the Spectaculum view
                mSpectaculumView.updateResolution(width, height);
            }

            @Override
            public void onRenderedFirstFrame() {
                // Hide loading indicator when video is ready for playback
                findViewById(R.id.loadingindicator).setVisibility(View.INVISIBLE);

                // Inform user that he can look around in the video
                Toast.makeText(StereoscopicView.this, R.string.drag, Toast.LENGTH_LONG).show();
            }


            /*@Override
            public void onVideoTracksDisabled() {
            }*/
        });

        mExoPlayer = player;

        // Display a hint to check for errors in case the video doesn't render
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ((TextView)findViewById(R.id.loadingindicator)).setText(R.string.loading_hint_error);
            }
        }, 10000);
    }

    /**
     * Release the ExoPlayer instance.
     */
    private void releasePlayer() {
        if(mExoPlayer != null) {
            mExoPlayer.release();
            mExoPlayer = null;
            gvrAudioProcessor = null;
        }
    }

    private class VideoSource {

        private String url;
        private ImmersiveEffect.Mode immersiveMode;

        public VideoSource(String url, ImmersiveEffect.Mode immersiveMode) {
            this.url = url;
            this.immersiveMode = immersiveMode;
        }
    }

    private static final class GvrRenderersFactory extends DefaultRenderersFactory {

        private final GvrAudioProcessor gvrAudioProcessor;

        private GvrRenderersFactory(Context context,
                                    DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                    GvrAudioProcessor gvrAudioProcessor) {
            super(context, drmSessionManager);
            this.gvrAudioProcessor = gvrAudioProcessor;
        }

        @Override
        public AudioProcessor[] buildAudioProcessors() {
            return new AudioProcessor[] {gvrAudioProcessor};
        }
    }



    private DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(
                context, bandwidthMeter, buildHttpDataSourceFactory(bandwidthMeter));
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
    }

    //Adapted from speculum
    public static float clamp(float check, float lowerBound, float upperBound) {
        if(check < lowerBound) {
            return lowerBound;
        } else if(check > upperBound) {
            return upperBound;
        }
        return check;
    }

}