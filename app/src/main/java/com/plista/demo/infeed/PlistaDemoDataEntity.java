package com.plista.demo.infeed;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Data meta information model class to be passed to the presenter class.
 * You may use this class to hold all the information necessary to present
 * a list item in the recycler
 */
public class PlistaDemoDataEntity {

    @Nullable
    private String urlAdTag;

    @Nullable
    private String responseXML;

    @Nullable
    private String titleText;

    @Nullable
    private String bodyText;

    @Nullable
    private String footerText;

    @Nullable
    private List<String> imageUrls;

    private boolean hasAd;
    private boolean autostart;


    @Nullable
    public String getUrlAdTag() {
        return urlAdTag;
    }

    public void setUrlAdTag(@Nullable String urlAdTag) {
        this.urlAdTag = urlAdTag;
    }

    @Nullable
    public String getResponseXML() {
        return responseXML;
    }

    public void setResponseXML(@Nullable String responseXML) {
        this.responseXML = responseXML;
    }

    @Nullable
    public String getTitleText() {
        return titleText;
    }

    public void setTitleText(@Nullable String titleText) {
        this.titleText = titleText;
    }

    @Nullable
    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(@Nullable String bodyText) {
        this.bodyText = bodyText;
    }

    @Nullable
    public String getFooterText() {
        return footerText;
    }

    public void setFooterText(@Nullable String footerText) {
        this.footerText = footerText;
    }

    @Nullable
    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(@Nullable List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public boolean hasAd() {
        return hasAd;
    }

    public void setHasAd(boolean hasAd) {
        this.hasAd = hasAd;
    }

    public boolean isAutostart() {
        return autostart;
    }

    public void setAutostart(boolean autostart) {
        this.autostart = autostart;
    }

    @Nullable
    public String getImageUrl() {

        if (imageUrls != null && imageUrls.size() >= 1) {
            return imageUrls.get(0);
        }
        return null;
    }

    public void addImageUrl(@Nullable String url) {

        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }
        imageUrls.add(url);
    }
}
