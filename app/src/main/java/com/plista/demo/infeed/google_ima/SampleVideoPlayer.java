// Copyright 2014 Google Inc. All Rights Reserved.

package com.plista.demo.infeed.google_ima;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * A VideoView that intercepts various methods and reports them back via a PlayerCallback.
 * This code was taken and modified from the AdvancedExample.
 * (https://github.com/googleads/googleads-ima-android)
 *
 * The Google IMA SDK uses this class for playing the Ads using the VideoAdPlayer interface
 * which is implemented in here.
 *
 * The VideoPlayer interface which is also implemented here is used to control this VideoView
 * from an own controller instance (In our case that is the GoogleIMAAdVideoPresenter).
 * That way this VideoView can be used to play both: Ads and normal content videos.
 *
 * The decision which one is presented is done by the controller (GoogleIMAAdVideoPresenter).
 *
 * In this implementation we do NOT show a demo content video.
 */
public class SampleVideoPlayer extends VideoView
    implements VideoPlayer {

    private static final String TAG = SampleVideoPlayer.class.getSimpleName();

    /** the MediaPlayer instance behind this VideoView */
    private MediaPlayer mediaPlayer;

    /** Interface for alerting caller of video completion. */
    public interface OnContentCompleteListener {
        public void onContentComplete();
    }

    private enum PlaybackState {
        STOPPED, PAUSED, PLAYING
    }

    private final List<VideoAdPlayer.VideoAdPlayerCallback> mAdCallbacks =
        new ArrayList<>(1);

    /** VideoAdPlayer interface implementation for the SDK to send ad play/pause type events. */
    private VideoAdPlayer videoAdPlayer;

    /** Used to track if the current video is an ad (as opposed to a content video). */
    private boolean mIsAdDisplayed;

    /** Called when the content is completed. */
    private OnContentCompleteListener  mOnContentCompleteListener;

    private MediaController            mMediaController;
    private PlaybackState              mPlaybackState;
    private final List<PlayerCallback> mVideoPlayerCallbacks = new ArrayList<PlayerCallback>(1);

    /** An Observable that allows subscribers to get the onPrepared() event */
    private final PublishSubject<Boolean> onMediaIsPrepared = PublishSubject.create();


    public SampleVideoPlayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public SampleVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SampleVideoPlayer(Context context) {
        super(context);
        init();
    }

    private void init() {
        Log.d(TAG, "init():");

        mPlaybackState = PlaybackState.STOPPED;
        mMediaController = new MediaController(getContext());
        mMediaController.setAnchorView(this);

        // For this DEMO we disabled the media control overlay of the VideoView
        // If you intend to show a content video you may want to re-enable them
        setMediaController(null);

        // Set OnCompletionListener to notify our callbacks when the video is completed.
        super.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.d(TAG, "onCompletion():");

                // Reset the MediaPlayer.
                // This prevents a race condition which occasionally results in the media
                // player crashing when switching between videos.
                disablePlaybackControls();
                mediaPlayer.reset();
                mediaPlayer.setDisplay(getHolder());
                mPlaybackState = PlaybackState.STOPPED;

                for (PlayerCallback callback : mVideoPlayerCallbacks) {
                    callback.onCompleted();
                }
            }
        });

        // Set OnErrorListener to notify our callbacks if the video errors.
        super.setOnErrorListener((mp, what, extra) -> {
            Log.d(TAG, "onError():");
            mPlaybackState = PlaybackState.STOPPED;
            for (PlayerCallback callback : mVideoPlayerCallbacks) {
                callback.onError();
            }

            // Returning true signals to MediaPlayer that we handled the error. This will
            // prevent the completion handler from being called.
            return true;
        });

        // In onPrepared we can get a reference of the MediaPlayer instance.
        // This is required for the mute/unmute finctionality.
        super.setOnPreparedListener(mp -> {
            Log.d(TAG, "onPrepared():");

            mediaPlayer = mp;
            onMediaIsPrepared.onNext(true);
        });

        setupAdPlayer();
    }

    /**
     * Mute the video
     */
    public void mute() {

        if (mediaPlayer != null) {
            mediaPlayer.setVolume(0, 0);
        }
    }

    /**
     * Unmute the video
     */
    public void unMute() {

        if (mediaPlayer != null) {
            mediaPlayer.setVolume(1, 1);
        }
    }

    /**
     * Get an Observable that reports to the subscriber that the MediaPlayer is prepared.
     * @return
     */
    public Observable<Boolean> getOnMediaIsPrepared() {
        return onMediaIsPrepared.hide();
    }

    @Override
    public int getDuration() {
        return mPlaybackState == PlaybackState.STOPPED ? 0 : super.getDuration();
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        // The OnCompletionListener can only be implemented by SampleVideoPlayer.
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        // The OnErrorListener can only be implemented by SampleVideoPlayer.
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() {
        Log.d(TAG, "start():");
        super.start();

        switch (mPlaybackState) {
            case STOPPED:
                Log.d(TAG, "start(): STOPPED");
                for (PlayerCallback callback : mVideoPlayerCallbacks) {
                    callback.onPlay();
                }
                break;
            case PAUSED:
                Log.d(TAG, "start(): PAUSED");
                for (PlayerCallback callback : mVideoPlayerCallbacks) {
                    callback.onResume();
                }
                break;
            default:
                // Already playing; do nothing.
                break;
        }
        mPlaybackState = PlaybackState.PLAYING;
    }

    //
    // Methods implementing the VideoPlayer interface.
    //

    @Override
    public void loadAd(String url) {
        Log.d(TAG, "loadAd()");
        setVideoPath(url);
    }

    @Override
    public void play() {
        Log.d(TAG, "play()");
        start();
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause()");
        super.pause();

        mPlaybackState = PlaybackState.PAUSED;
        for (PlayerCallback callback : mVideoPlayerCallbacks) {
            callback.onPause();
        }
    }

    @Override
    public void stopPlayback() {
        Log.d(TAG, "stopPlayback()");

        if (mPlaybackState == PlaybackState.STOPPED) {
            return;
        }
        super.stopPlayback();
        mPlaybackState = PlaybackState.STOPPED;
    }

    @Override
    public void disablePlaybackControls() {

        // For this DEMO we disabled the media control overlay of the VideoView
        // If you intend to show a content video you may want to re-enable them

        // setMediaController(null);
    }

    @Override
    public void enablePlaybackControls() {

        // For this DEMO we disabled the media control overlay of the VideoView
        // If you intend to show a content video you may want to re-enable them

        // setMediaController(mMediaController);
    }

    @Override
    public void addPlayerCallback(PlayerCallback callback) {
        Log.d(TAG, "addPlayerCallback():");

        mVideoPlayerCallbacks.add(callback);
    }

    @Override
    public void removePlayerCallback(PlayerCallback callback) {
        Log.d(TAG, "removePlayerCallback():");

        mVideoPlayerCallbacks.remove(callback);
    }

    /**
     * Set a listener to be triggered when the content (non-ad) video completes.
     */
    public void setOnContentCompleteListener(OnContentCompleteListener listener) {
        mOnContentCompleteListener = listener;
    }

    /**
     * Returns an implementation of the SDK's VideoAdPlayer interface.
     */
    public VideoAdPlayer getVideoAdPlayer() {
        return videoAdPlayer;
    }

    /**
     * Create and setup the implementation of the SDK's VideoAdPlayer interface.
     */
    private void setupAdPlayer() {
        mIsAdDisplayed = false;

        videoAdPlayer = new VideoAdPlayer() {
            @Override
            public void playAd() {
                Log.d(TAG, "playAd()");

                mIsAdDisplayed = true;
                play();
            }

            @Override
            public void loadAd(String url) {
                Log.d(TAG, "loadAd(): URL: " + url);

                mIsAdDisplayed = true;
                setVideoPath(url);
            }

            @Override
            public void stopAd() {
                Log.d(TAG, "stopAd()");

                stopPlayback();
            }

            @Override
            public void pauseAd() {
                Log.d(TAG, "pauseAd()");

                pause();
            }

            @Override
            public void resumeAd() {
                Log.d(TAG, "resumeAd()");

                playAd();
            }

            @Override
            public void addCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
                Log.d(TAG, "VideoAdPlayer.addCallback(): " + videoAdPlayerCallback.toString());

                mAdCallbacks.add(videoAdPlayerCallback);
            }

            @Override
            public void removeCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
                Log.d(TAG, "VideoAdPlayer.removeCallback()");

                mAdCallbacks.remove(videoAdPlayerCallback);
            }

            @Override
            public VideoProgressUpdate getAdProgress() {
                Log.v(TAG, "VideoAdPlayer.getAdProgress()");
                if (!mIsAdDisplayed || getDuration() <= 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                return new VideoProgressUpdate(getCurrentPosition(),
                                               getDuration());
            }
        };

        // Set player callbacks for delegating major video events.
        addPlayerCallback(new VideoPlayer.PlayerCallback() {

            @Override
            public void onPlay() {
                Log.d(TAG, "PlayerCallback.onPlay(): " + mIsAdDisplayed);
                if (mIsAdDisplayed) {
                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                        Log.d(TAG, "PlayerCallback.onPlay(): calling: " + callback.toString());
                        callback.onPlay();
                    }
                }
            }

            @Override
            public void onPause() {
                Log.d(TAG, "PlayerCallback.onPause()");
                if (mIsAdDisplayed) {
                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                        callback.onPause();
                    }
                }
            }

            @Override
            public void onResume() {
                Log.d(TAG, "PlayerCallback.onResume()");
                if (mIsAdDisplayed) {
                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                        callback.onResume();
                    }
                }
            }

            @Override
            public void onError() {
                Log.d(TAG, "PlayerCallback.onError()");
                if (mIsAdDisplayed) {
                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                        callback.onError();
                    }
                }
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, "PlayerCallback.onCompleted()");
                if (mIsAdDisplayed) {
                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                        callback.onEnded();
                    }
                } else {
                    // Alert an external listener that our content video is complete.
                    if (mOnContentCompleteListener != null) {
                        mOnContentCompleteListener.onContentComplete();
                    }
                }
            }
        });
    }
}
