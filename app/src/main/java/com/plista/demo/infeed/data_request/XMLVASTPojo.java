package com.plista.demo.infeed.data_request;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * The model class which will be deserialized from the AdTag response.
 * Maps only the requireed Extension field. All other fields are ignored.
 */
@Root(name = "VAST", strict = false)
public class XMLVASTPojo {

    public XMLVASTPojo() {

    }

    @Element(name = "Ad", required = false)
    private Ad ad;

    public Ad getAd() {
        return ad;
    }

    public void setAd(Ad ad) {
        this.ad = ad;
    }

    @Root(strict = false)
    @Element(name = "Ad")
    public static class Ad {

        @Element(name = "InLine")
        private InLine inline;

        public InLine getInline() {
            return inline;
        }

        public void setInline(InLine inline) {
            this.inline = inline;
        }
    }

    @Root(strict = false)
    @Element(name = "InLine")
    public static class InLine {

        @Element(name = "Extensions")
        private Extensions extensions;

        @Element(name = "Creatives")
        private Creatives creatives;

        public Extensions getExtensions() {
            return extensions;
        }

        public void setExtensions(Extensions extensions) {
            this.extensions = extensions;
        }

        public Creatives getCreatives() {
            return creatives;
        }

        public void setCreatives(Creatives creatives) {
            this.creatives = creatives;
        }
    }

    @Root(strict = false)
    @Element(name = "Extensions")
    public static class Extensions {

        @Element(name = "Title")
        private String title;

        @Element(name = "Text")
        private String text;

        @ElementList(entry = "Image", inline = true)
        private List<String> images;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public List<String> getImages() {
            return images;
        }

        public void setImages(List<String> images) {
            this.images = images;
        }
    }

    @Root(strict = false)
    @Element(name = "Creatives")
    public static class Creatives {

        @Element(name = "Creative")
        private Creative creative;

        public Creative getCreative() {
            return creative;
        }

        public void setCreative(Creative creative) {
            this.creative = creative;
        }
    }

    @Root(strict = false)
    @Element(name = "Creative")
    public static class Creative {

        @Element(name = "Linear")
        private Linear linear;

        public Linear getLinear() {
            return linear;
        }

        public void setLinear(Linear linear) {
            this.linear = linear;
        }
    }

    @Root(strict = false)
    @Element(name = "Linear")
    public static class Linear {

        @Element(name = "VideoClicks")
        private VideoClicks videoClicks;

        @Element(name = "Duration")
        private String duration;

        public VideoClicks getVideoClicks() {
            return videoClicks;
        }

        public void setVideoClicks(VideoClicks videoClicks) {
            this.videoClicks = videoClicks;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }
    }

    @Root(strict = false)
    @Element(name = "VideoClicks")
    public static class VideoClicks {

        @Element(name = "ClickThrough")
        private String clickThroughURL;

        @Element(name = "ClickTracking")
        private String clickTrackingURL;

        public String getClickThroughURL() {
            return clickThroughURL;
        }

        public void setClickThroughURL(String clickThroughURL) {
            this.clickThroughURL = clickThroughURL;
        }

        public String getClickTrackingURL() {
            return clickTrackingURL;
        }

        public void setClickTrackingURL(String clickTrackingURL) {
            this.clickTrackingURL = clickTrackingURL;
        }
    }
}
