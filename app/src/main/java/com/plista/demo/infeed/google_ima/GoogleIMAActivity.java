package com.plista.demo.infeed.google_ima;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.plista.demo.infeed.PlistaDemoDataEntity;
import com.plista.demo.infeed.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

/**
 * This Activity contains the demo RecyclerView and handles the expand/collapse
 * action of the video-ad items.
 */
public class GoogleIMAActivity extends AppCompatActivity {

    private static final String TAG = GoogleIMAActivity.class.getSimpleName();

    private static final int NUM_LIST_ITEMS = 10;

    private List<PlistaDemoDataEntity> plistaDemoDataEntities;

    @BindView(R.id.rootView)
    FrameLayout rootView;

    @BindView(R.id.recycler_view)
    RecyclerView recycler;

    private GoogleIMARecyclerAdapter  listAdapter;
    private CompositeDisposable       composite;

    private boolean                   isInExpandedState;
    private int                       restoreOrientation;
    private LinearLayout.LayoutParams restoreExpandParams;

    //
    // Activity Lifecycle
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_google_ima);
        ButterKnife.bind(this);

        setupDemoData();

        listAdapter = new GoogleIMARecyclerAdapter(this, plistaDemoDataEntities);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recycler.setLayoutManager(llm);
        recycler.setAdapter(listAdapter);

        composite = new CompositeDisposable();
    }

    @Override
    protected void onResume() {
        super.onResume();

        onBindObservables();
    }

    @Override
    protected void onPause() {
        super.onPause();

        composite.clear();
    }

    //
    // Privates
    //

    /**
     * Subscribe to RxJava Observables in here.
     */
    private void onBindObservables() {

        // subscribe to the subject that signals that the expand button was clicked on
        // an ad video item
        composite.add(
            listAdapter.getOnVideoExpandButtonClickedObservable()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((videoContainer) -> {
                            Log.d(TAG, "OnVideoExpandButtonClick():");

                            if (isInExpandedState) {
                                collapseAdPlayback(videoContainer);
                            } else {
                                expandAdPlayback(videoContainer);
                            }
                            isInExpandedState = !isInExpandedState;

                        },
            error -> Log.e(TAG, "ERROR: OnVideoExpandButtonClick() " + error))
        );
    }

    /**
     * Expand the ad video container. Detach the VideoView from it's parent and
     * attach it to this Activity's base layout.
     * Additionally we hide the Toolbar, Statusbar and Navigationbar.
     *
     * @param videoContainer
     */
    private void expandAdPlayback(Pair<ViewGroup, ViewGroup> videoContainer) {

        // NOTE: is the fullscreen video shall be presented in landscape
        // you can use the next lines

        // remember orientation
        // restoreOrientation = this.getResources().getConfiguration().orientation;
        // set orientation to landscape
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // hide navigationbar and statusbar
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);

        // hide the toolbar/actionbar if present
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // remove the video container from it's parent (i.e. the recycler item)
        // and make it a child of this activities view
        ViewGroup expandItem = videoContainer.first;
        ViewGroup parent     = videoContainer.second;

        if (parent != null) {
            // detach the child from parent or you get an exception if you try
            // to add it to another one
            parent.removeView(expandItem);
        }

        // We need to remember the layout params and the current height
        restoreExpandParams = (LinearLayout.LayoutParams) expandItem.getLayoutParams();
        restoreExpandParams.height = expandItem.getHeight();

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT);

        recycler.setVisibility(View.INVISIBLE);

        expandItem.setLayoutParams(params);
        rootView.addView(expandItem);
    }

    /**
     * Collapse the ad video container. Detach it from the Activity's base layout
     * and re-attach it to it's origin parent (in the recycler viewholder item).
     * Also restore Toolbar, Statusbar and Navigationbar.
     *
     * @param videoContainer
     */
    private void collapseAdPlayback(Pair<ViewGroup, ViewGroup> videoContainer) {

        // Make the statusbar and navigationbar visible again.
        getWindow().getDecorView().setSystemUiVisibility(0);
        // Make toolbar/action bar visible again
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }

        // NOTE: is the fullscreen video shall be presented in landscape
        // you can use the next lines

        // Restore Orientation
        // setRequestedOrientation(restoreOrientation);

        // Now we add the video container back to it's original parent
        ViewGroup expandItem = videoContainer.first;
        ViewGroup parent     = videoContainer.second;

        if (rootView != null) {
            // detach the child from parent or you get an exception if you try
            // to add it to another one
            rootView.removeView(expandItem);
        }

        recycler.setVisibility(View.VISIBLE);

        expandItem.setLayoutParams(restoreExpandParams);
        expandItem.invalidate();
        parent.addView(expandItem, 0);
    }

    /**
     * Setup the List with Demo DataEntities.
     *
     * Place the Ad item at position 4 and 7, if available (i.e. not null)
     * The item with the faulty AdTag is at position 10
     */
    private void setupDemoData() {
        Log.d(TAG, "setupDemoData()");

        plistaDemoDataEntities = new ArrayList<>();

        for (int i = 0; i < NUM_LIST_ITEMS; i++) {
            PlistaDemoDataEntity item = new PlistaDemoDataEntity();

            // Add sample Ad to item #1 and #6
            if (i == 1 || i == 6) {

                item.setHasAd(true);
                item.setUrlAdTag(getString(R.string.ad_tag_url));

            // Add broken-tag sample Ad to item #3
            } else if (i == 3) {

                item.setHasAd(true);
                item.setUrlAdTag(getString(R.string.ad_tag_url_broken));

            // Others
            } else {

                item.setHasAd(false);
                item.setTitleText(getString(R.string.item_blind_title));
                item.setBodyText(getString(R.string.item_blind_body));
                item.setFooterText(getString(R.string.item_blind_footer));
            }

            // When WIFI is connected set autostart to true

            item.setAutostart(isWifiConnected());

            // TODO: disable Autostart for debugging
            // item.setAutostart(false);

            plistaDemoDataEntities.add(item);
        }
    }

    /**
     * Check if WIFI is connected
     *
     * @return
     */
    private boolean isWifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }
}
