package com.plista.demo.infeed.google_ima;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.plista.demo.infeed.PlistaDemoDataEntity;
import com.plista.demo.infeed.R;


import java.lang.ref.WeakReference;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class GoogleIMARecyclerAdapter extends RecyclerView.Adapter<GoogleIMARecyclerAdapter.GoogleIMAItemViewHolder>{

    private static final String TAG = GoogleIMARecyclerAdapter.class.getSimpleName();

    private final PublishSubject<Pair<ViewGroup, ViewGroup>> onVideoExpandButtonClickedSubject
        = PublishSubject.create();

    @NonNull
    private WeakReference<Context>     contextRef;
    @NonNull
    private List<PlistaDemoDataEntity> dataEntities;


    public GoogleIMARecyclerAdapter(@NonNull Context context,
                                    @NonNull List<PlistaDemoDataEntity> dataEntities) {

        this.contextRef = new WeakReference<>(context);
        this.dataEntities = dataEntities;
    }

    //
    // RecyclerView.Adapter callbacks
    //

    @Override
    public GoogleIMAItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.google_ima_recycler_item,
                                           parent,
                                           false);
        return new GoogleIMAItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GoogleIMAItemViewHolder viewHolder, int position) {

        PlistaDemoDataEntity dataEntity = dataEntities.get(position);

        if (dataEntity.hasAd()) {

            viewHolder.setGoogleIMAAdVideoPresenter(
                new GoogleIMAAdVideoPresenter(contextRef.get(),
                                              dataEntity,
                                              viewHolder,
                                              onVideoExpandButtonClickedSubject,
                                              this));

            viewHolder.itemVideoPlayButton.setVisibility(View.VISIBLE);
            viewHolder.itemImage.setVisibility(View.INVISIBLE);

        } else {

            viewHolder.itemTitle.setText(dataEntity.getTitleText());
            viewHolder.itemBody.setText(dataEntity.getBodyText());
            viewHolder.itemFooter.setText(dataEntity.getFooterText());

            viewHolder.itemVideoPlayButton.setVisibility(View.GONE);
            viewHolder.toggleAudioMuteButton.setVisibility(View.GONE);
            viewHolder.itemVideoExpandButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return dataEntities.size();
    }

    @Override
    public void onViewRecycled(GoogleIMAItemViewHolder holder) {
        super.onViewRecycled(holder);
        Log.d(TAG, "onViewRecycled(): ");

        // TODO:
    }

    public void onItemDismiss(int position) {
        Log.d(TAG, "onItemDismiss(): position: " + position);
        if (position >= 0) {
            dataEntities.remove(position);
            notifyItemRemoved(position);
        } else {
            Log.w(TAG, "onItemDismiss(): INVALID POSITION");
        }
    }

    /**
     * Get an Observable that fires when an video expand button is clicked.
     * We handle the expand/collapse action in the Activity that contains this
     * RecyclerView
     * The Observable emits a Pair that contains the layout container of the AdPlayer
     * and it's parent in order to attach the AdPlayer container to the Activities layout
     * for fullscreen presentation
     *
     * @return
     */
    public Observable<Pair<ViewGroup, ViewGroup>> getOnVideoExpandButtonClickedObservable() {
        return onVideoExpandButtonClickedSubject.hide();
    }


    //
    // The ViewHolder for the Recycler (google_ima_recycler_item.xml)
    //

    static class GoogleIMAItemViewHolder extends RecyclerView.ViewHolder {

        // Placeholder Image
        @BindView(R.id.itemImage)
        ImageView itemImage;

        // Ad Video Play Button
        @BindView(R.id.playButton)
        ImageButton itemVideoPlayButton;

        // Ad Video Expand to Fullscreen Button
        @BindView(R.id.toggleVideoExpandButton)
        ImageButton itemVideoExpandButton;

        // Ad Video Toggle Audio Mute Button
        @BindView(R.id.toggleAudioButton)
        ImageButton toggleAudioMuteButton;

        // ViewGroup ID required for expand/collapse
        @BindView(R.id.expandItemContainer)
        ViewGroup expandItemContainer;

        // ViewGroup ID required for expand/collapse
        @BindView(R.id.expandParentContainer)
        ViewGroup expandParentContainer;

        // Container for AdUI. This will be passed to the Google IMA SDK
        // which will add some UI elements
        // (i.e. "Learn More" link and timer)
        @BindView(R.id.adUiContainer)
        ViewGroup adUiContainer;

        // Ad VideoPlayer (the ad will be played in here. we can use it as well
        // to play a "normal" content video
        @BindView(R.id.itemVideo)
        SampleVideoPlayer itemVideoPlayer;

        // The ViewGroup containing Title, Textbody and Footer
        @BindView(R.id.itemTextContainer)
        ViewGroup itemTextContainer;

        // List Item Title
        @BindView(R.id.itemTitle)
        TextView itemTitle;

        // List Item Body
        @BindView(R.id.itemBody)
        TextView itemBody;

        // List Item Footer
        @BindView(R.id.itemFooter)
        TextView itemFooter;

        // The presenter cares about all the ad handling
        @NonNull
        private GoogleIMAAdVideoPresenter adPresenter;


        GoogleIMAItemViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        public void setGoogleIMAAdVideoPresenter(@NonNull GoogleIMAAdVideoPresenter adPresenter) {
            this.adPresenter = adPresenter;
        }
    }
}
