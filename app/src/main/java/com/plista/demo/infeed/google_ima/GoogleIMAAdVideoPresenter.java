package com.plista.demo.infeed.google_ima;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.plista.demo.infeed.PlistaDemoDataEntity;
import com.plista.demo.infeed.R;
import com.plista.demo.infeed.data_request.ApiFactory;
import com.plista.demo.infeed.data_request.XMLVASTPojo;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.IOException;
import java.lang.ref.WeakReference;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;
import rx.schedulers.Schedulers;

/**
 * This class cares about the correct presentation of the ad and other content in the UI.
 *
 * Does VAST tag request and handles response
 * Handles Google IMA SDK Ad management
 * Controls VideoPlayer
 */
public class GoogleIMAAdVideoPresenter {

    private static final String TAG = GoogleIMAAdVideoPresenter.class.getSimpleName();

    // Our DEMO

    /** The video player */
    private SampleVideoPlayer sampleVideoPlayerAdvanced;

    /** The container for the ad's UI */
    private ViewGroup mAdUiContainer;

    /** The ViewHolder */
    private GoogleIMARecyclerAdapter.GoogleIMAItemViewHolder viewHolder;

    /** The meta data object */
    private PlistaDemoDataEntity dataEntity;

    /** The ClickThrough URL */
    private String clickThroughURL;

    // Google IMA SDK

    /** Factory class for creating SDK objects. */
    private ImaSdkFactory mSdkFactory;

    /** The AdsLoader instance exposes the requestAds method. */
    private AdsLoader mAdsLoader;

    /** AdsManager exposes methods to control ad playback and listen to ad events. */
    private AdsManager mAdsManager;


    // Ad Playback state

    private boolean isAdPlaying;
    private boolean isAdPaused;
    private boolean isAudioMuted;
    private boolean isAdExpanded;
    private int     currentVideoPosition;

    private PublishSubject<Pair<ViewGroup, ViewGroup>> onVideoExpandButtonClickedSubject;

    private final PublishSubject<String> vastResponseLoaded = PublishSubject.create();
    private CompositeDisposable          composite;

    @NonNull
    private WeakReference<Context> contextRef;

    private GoogleIMARecyclerAdapter adapter;

    public GoogleIMAAdVideoPresenter(@NonNull Context context,
                                     @NonNull PlistaDemoDataEntity dataEntity,
                                     @NonNull GoogleIMARecyclerAdapter.GoogleIMAItemViewHolder viewHolder,
                                     @NonNull PublishSubject<Pair<ViewGroup, ViewGroup>> onVideoExpandButtonClickedSubject,
                                     @NonNull GoogleIMARecyclerAdapter adapter) {

        // Assign values
        this.contextRef                        = new WeakReference<>(context);
        this.adapter                           = adapter;
        this.onVideoExpandButtonClickedSubject = onVideoExpandButtonClickedSubject;
        this.viewHolder                        = viewHolder;
        this.mAdUiContainer                    = viewHolder.adUiContainer;
        this.sampleVideoPlayerAdvanced         = viewHolder.itemVideoPlayer;
        this.dataEntity                        = dataEntity;

        // Create an AdsLoader and optionally set the language.
        ImaSdkSettings imaSdkSettings = new ImaSdkSettings();
        imaSdkSettings.setLanguage("en");
        mSdkFactory = ImaSdkFactory.getInstance();
        mAdsLoader = mSdkFactory.createAdsLoader(context, imaSdkSettings);

        // Add listeners for when ads are loaded and for errors.
        mAdsLoader.addAdErrorListener(adErrorEvent -> {
            Log.e(TAG, "AdsLoader.onAdError: " + adErrorEvent.getError().getMessage());
            setupViewHolderErrorCase(adErrorEvent.getError().getMessage());
        });

        mAdsLoader.addAdsLoadedListener(new AdsLoadedListener());

        sampleVideoPlayerAdvanced.setOnContentCompleteListener(
            new SampleVideoPlayer.OnContentCompleteListener() {
                /**
                 * Event raised by VideoPlayerWithAdPlayback when content video is complete.
                 * NOTE: This DEMO does not play a content video
                 */
                @Override
                public void onContentComplete() {
                    Log.d(TAG, "onContentComplete()");
                    mAdsLoader.contentComplete();
                }
            });

        composite = new CompositeDisposable();

        onBindObservables();
        requestVast();
    }

    /**
     * Call to the AdTagURL to retrieve a VAST XML Response
     */
    private void requestVast() {
        Log.d(TAG, "requestVast():");

        ApiFactory.getService().getVASTDataRAW(dataEntity.getUrlAdTag())
                  .subscribeOn(Schedulers.io())
                  .observeOn(Schedulers.computation())
                  .subscribe(xmlvastResponse -> {

                      try {
                          vastResponseLoaded.onNext(xmlvastResponse.string());
                      } catch (IOException e) {
                          e.printStackTrace();
                      }

                  }, error -> {

                      Log.e(TAG, "API Response: something went wrong: " + error);
                      vastResponseLoaded.onNext(null);
                  });
    }

    private void onBindObservables() {
        Log.d(TAG, "onBindObservables():");

        // This Subscription waits for the completion if the VAST call
        composite.add(vastResponseLoaded
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(vastXMLResponse -> {

                     if (dataEntity.isAutostart()) {
                         requestAds(vastXMLResponse);
                         viewHolder.itemVideoPlayButton.setVisibility(View.GONE);
                     } else {
                         setupViewHolder(vastXMLResponse);
                     }

                 }, error -> {

                     Log.e(TAG, "ERROR: remoteEntityLoaded" + error);
                     setupViewHolderErrorCase(error.getMessage());
                 }
              )
        );
    }

    /**
     * Request video ads from the given VAST ad tag.
     */
    public void requestAds(String vastXMLResponse) {
        Log.d(TAG, "requestAds():");

        setupViewHolder(vastXMLResponse);

        AdDisplayContainer adDisplayContainer = mSdkFactory.createAdDisplayContainer();
        adDisplayContainer.setPlayer(sampleVideoPlayerAdvanced.getVideoAdPlayer());
        adDisplayContainer.setAdContainer(mAdUiContainer);

        // Create the ads request.
        AdsRequest request = mSdkFactory.createAdsRequest();
        request.setAdDisplayContainer(adDisplayContainer);
        request.setAdsResponse(vastXMLResponse);

        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        mAdsLoader.requestAds(request);
    }

    /**
     * Setup the View for a valid VAST XML Response:
     *
     * - Set Title, Text and Image
     * - Set onClickListener for PlayButton when not Autostart
     * - Set onClickListener for AudioMuteButton
     * - Set onClickListener for VideoExpandButton
     *
     * @param xmlVastResponse
     */
    private void setupViewHolder(String xmlVastResponse) {
        Log.d(TAG, "setupViewHolder():");
        Log.v(TAG, "\n" + xmlVastResponse);

        Serializer serializer = new Persister();
        Context ctx = contextRef.get();

        try {
            XMLVASTPojo xmlvastPojo = serializer.read(XMLVASTPojo.class, xmlVastResponse);

            // From the broken adTagURL we get an XML response without the "Ad" tag
            if (xmlvastPojo.getAd() == null) {
                setupViewHolderErrorCase("VAST AdTag Response is invalid");
                return;
            }

            Log.d(TAG, "setupViewHolder(): got pojo: "
                + xmlvastPojo.getAd().getInline().getExtensions().getTitle());

            viewHolder.itemTitle.setText(xmlvastPojo.getAd().getInline().getExtensions().getTitle());
            viewHolder.itemBody.setText(xmlvastPojo.getAd().getInline().getExtensions().getText());
            viewHolder.itemFooter.setText(ctx.getString(R.string.powered_by));
            viewHolder.itemImage.setVisibility(View.VISIBLE);

            clickThroughURL = xmlvastPojo.getAd()
                                         .getInline()
                                         .getCreatives()
                                         .getCreative()
                                         .getLinear()
                                         .getVideoClicks()
                                         .getClickThroughURL();

            // Load Images from an URL (we use glide to support transparent async loading and
            // animated gifs)
            Glide
                .with(contextRef.get())
                .load(xmlvastPojo.getAd().getInline().getExtensions().getImages().get(0))
                .crossFade()
                .into(viewHolder.itemImage);

            // Set ClickListener

            // Play Button
            viewHolder.itemVideoPlayButton.setOnClickListener(v -> {
                Log.d(TAG, "onClick(): VideoPlayButton");

                if (!isAdPaused) {
                    requestAds(xmlVastResponse);
                } else {
                    togglePause();
                }
                viewHolder.itemVideoPlayButton.setVisibility(View.GONE);
            });

            // Audio Mute/Unmute
            viewHolder.toggleAudioMuteButton.setOnClickListener(v -> {
                Log.d(TAG, "onClick(): Audio Mute/Unmute");
                toggleMute();
            });

            // Video expand/collapse
            viewHolder.itemVideoExpandButton.setOnClickListener(v -> {
                Log.d(TAG, "onClick(): Video Expand/Collapse");
                toggleExpand();
            });

            // Trigger ClickThrough URL when tapping the Textarea
            viewHolder.itemTextContainer.setOnClickListener(v -> {
                Log.d(TAG, "onClick(): Textarea tapped");
                startClickThroughWebIntent();
            });

            if (dataEntity.isAutostart()) {
                sampleVideoPlayerAdvanced
                    .getOnMediaIsPrepared()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(prepared -> toggleMute());
            }

        } catch (Exception e) {
            e.printStackTrace();
            setupViewHolderErrorCase("VAST AdTag Response is broken");
        }
    }

    /**
     * Setup the View for an invalid VAST XML Response:
     * - Set Error message for Title and Text
     * - Set an Error indicator image
     */
    private void setupViewHolderErrorCase(String errorMessage) {
        Log.d(TAG, "setupViewHolderErrorCase(): ");

        // In case of an invalid VAST XML Response we remove the item from the RecyclerView
        // NOTE: first, before the VAST calls is sent, the item is visible, and will then
        // be smoothly removed when the call returns with an error.
        adapter.onItemDismiss(viewHolder.getAdapterPosition());

        // If an alternative Text, Title and Image shall be shown you can use the code below

        /*
        viewHolder.itemTitle.setText("An Error occurred !");
        viewHolder.itemBody.setText(errorMessage);
        viewHolder.itemFooter.setText(contextRef.get().getString(R.string.powered_by));

        viewHolder.itemVideoExpandButton.setVisibility(View.GONE);
        viewHolder.toggleAudioMuteButton.setVisibility(View.GONE);
        viewHolder.itemVideoPlayButton.setVisibility(View.GONE);
        viewHolder.itemImage.setVisibility(View.VISIBLE);
        */
    }

    /**
     * Pause/Resume the Ad presentation. Show/Hide the play button
     */
    private void togglePause() {

        if (isAdPlaying) {
            isAdPaused = true;
            mAdsManager.pause();
            viewHolder.itemVideoPlayButton.setVisibility(View.VISIBLE);

        } else {
            isAdPaused = false;
            mAdsManager.resume();
            viewHolder.itemVideoPlayButton.setVisibility(View.GONE);
        }
        isAdPlaying = !isAdPlaying;
    }

    /**
     * Mute/Unmute the Ad presentation. Switch the mute icon
     */
    private void toggleMute() {
        Log.d(TAG, "toggleMute(): muted: " + isAudioMuted);

        Context ctx = contextRef.get();

        if (isAudioMuted) {
            viewHolder.toggleAudioMuteButton.setImageDrawable(
                ctx.getResources().getDrawable(R.mipmap.ic_volume_up_white_24dp));
            sampleVideoPlayerAdvanced.unMute();
        } else {
            viewHolder.toggleAudioMuteButton.setImageDrawable(
                ctx.getResources().getDrawable(R.mipmap.ic_volume_mute_white_24dp));
            sampleVideoPlayerAdvanced.mute();
        }

        isAudioMuted = !isAudioMuted;
    }

    /**
     * Expand/Collapses the Ad presentation.
     */
    private void toggleExpand() {

        setupVideoExpandButton();
        savePlayerState();

        onVideoExpandButtonClickedSubject.onNext(new Pair<>(viewHolder.expandItemContainer,
                                                            viewHolder.expandParentContainer));

        sampleVideoPlayerAdvanced
            .getOnMediaIsPrepared()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(prepared -> restorePlayerState());
    }

    /**
     * Save the current position of the Ad presentation.
     */
    private void savePlayerState() {
        currentVideoPosition = sampleVideoPlayerAdvanced.getCurrentPosition();
    }

    /**
     * Restore the Ad presentation after expand/collapse
     */
    private void restorePlayerState() {
        Log.d(TAG, "restorePlayerState():");

        sampleVideoPlayerAdvanced.seekTo(currentVideoPosition);
        if (!isAdPaused) {
            sampleVideoPlayerAdvanced.start();
        }
        isAudioMuted = !isAudioMuted;
        toggleMute();
        isAdExpanded = !isAdExpanded;
        setupVideoExpandButton();
    }

    /**
     * Create and start an Intent with the ClickThrough URL.
     * Pauses the Video in the meantime
     */
    private void startClickThroughWebIntent() {
        Log.d(TAG, "startClickThroughWebIntent():");

        if (TextUtils.isEmpty(clickThroughURL)) {
            return;
        }

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(clickThroughURL));
        contextRef.get().startActivity(i);

        savePlayerState();

        sampleVideoPlayerAdvanced
            .getOnMediaIsPrepared()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(prepared -> restorePlayerState());
    }

    /**
     * Switch the expand icon
     */
    private void setupVideoExpandButton() {
        Log.d(TAG, "setupVideoExpandButton(): expanded: " + isAdExpanded);

        Context ctx = contextRef.get();

        if (isAdExpanded) {
            viewHolder.itemVideoExpandButton.setImageDrawable(
                ctx.getResources().getDrawable(R.mipmap.ic_fullscreen_white_24dp));
        } else {
            viewHolder.itemVideoExpandButton.setImageDrawable(
                ctx.getResources().getDrawable(R.mipmap.ic_fullscreen_exit_white_24dp));
        }

        isAdExpanded = !isAdExpanded;
    }

    // Inner class implementation of AdsLoader.AdsLoaderListener.

    private class AdsLoadedListener implements AdsLoader.AdsLoadedListener {

        /**
         * An event raised when ads are successfully loaded from the ad server via AdsLoader.
         */

        @Override
        public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
            // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
            // events for ad playback and errors.
            mAdsManager = adsManagerLoadedEvent.getAdsManager();

            // Attach event and error event listeners.
            mAdsManager.addAdErrorListener(new AdErrorEvent.AdErrorListener() {
                /**
                 * An event raised when there is an error loading or playing ads.
                 */
                @Override
                public void onAdError(AdErrorEvent adErrorEvent) {
                    Log.e(TAG, "AdsManager.AdErrorEvent: " + adErrorEvent.getError().getMessage());
                    setupViewHolderErrorCase(adErrorEvent.getError().getMessage());
                }
            });

            /**
             * The AdEventListener delivers all Events related to the Ad presentation
             */
            mAdsManager.addAdEventListener(new AdEvent.AdEventListener() {
                /**
                 * Responds to AdEvents.
                 */
                public void onAdEvent(AdEvent adEvent) {
                    Log.i(TAG, "Event: " + adEvent.getType());

                    // These are the suggested event types to handle. For full list of all ad event
                    // types, see the documentation for AdEvent.AdEventType.
                    switch (adEvent.getType()) {

                        case TAPPED:

                            // AdEventType.TAPPED will be fired when the AdContainer is tapped
                            // Our DEMO will pause/resume the Ad presentation when tapped

                            togglePause();
                            break;

                        case LOADED:

                            // AdEventType.LOADED will be fired when ads are ready to be played.
                            // AdsManager.start() begins ad playback
                            // (This method is ignored for VMAP or ad rules playlists, as the SDK
                            // will automatically start executing the playlist.)

                            mAdsManager.start();
                            isAdPlaying = true;

                            break;

                        case CONTENT_PAUSE_REQUESTED:

                            // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
                            // ad is played.

                            // Our DEMO will now hide the placeholder image and make the
                            // audio and expand controls visible
                            // An implementation that has a content video playing instead would
                            // now stop this video

                            viewHolder.itemImage.setVisibility(View.INVISIBLE);
                            viewHolder.itemVideoExpandButton.setVisibility(View.VISIBLE);
                            viewHolder.toggleAudioMuteButton.setVisibility(View.VISIBLE);

                            break;

                        case CONTENT_RESUME_REQUESTED:

                            // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
                            // and you should start playing your content.

                            // Our DEMO will now show the placeholder image and make the
                            // audio and expand controls visible
                            // An implementation that has a content video playing instead would
                            // now resume this video

                            viewHolder.itemImage.setVisibility(View.VISIBLE);
                            viewHolder.itemVideoExpandButton.setVisibility(View.GONE);
                            viewHolder.toggleAudioMuteButton.setVisibility(View.GONE);

                            break;

                        case ALL_ADS_COMPLETED:

                            // When Ads are completed we collapse the video if expanded

                            if (isAdExpanded) {
                                onVideoExpandButtonClickedSubject.onNext(
                                    new Pair<>(viewHolder.expandItemContainer,
                                               viewHolder.expandParentContainer));
                            }

                            if (mAdsManager != null) {
                                mAdsManager.destroy();
                                mAdsManager = null;
                            }
                            isAdPlaying = false;

                            break;

                        default:
                            break;
                    }
                }

            });

            mAdsManager.init();
        }
    }
}
