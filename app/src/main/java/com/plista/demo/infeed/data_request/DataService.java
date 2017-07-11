package com.plista.demo.infeed.data_request;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

public interface DataService {

    // Query parameters for the API call
    String VAST_DEMO_PUBLIC_KEY = "f144bc1edf542f5c5c30bf1c";
    String VAST_DEMO_WIDGETNAME = "outstream";
    String VAST_DEMO_INFEED     = "";
    String VAST_DEMO_ITEM_ID    = "354852121";
    String VAST_DEMO_TEST       = "1";

    /**
     * This call delivers the deserialized response as an Observable of type XMLVASTPojo
     *
     * Example URL:
     * http://farm.plista.com/video/vast/?publickey=f144bc1edf542f5c5c30bf1c&widgetname=outstream&infeed&itemid=354852121
     */
    @Headers({
        "Content-Type: text/xml; charset=utf-8",
        "Accept: text/xml"
    })
    @GET("video/vast/")
    Observable<Response<XMLVASTPojo>> getVASTData(@Query("publickey")  String publicKey,
                                                  @Query("widgetname") String widgetname,
                                                  @Query("infeed")     String infeed,
                                                  @Query("itemid")     String itemid,
                                                  @Query("test")       String test);

    /**
     * This call delivers the raw response XML body as Observable. Without deserialization.
     *
     * Example URL:
     * http://farm.plista.com/video/vast/?publickey=f144bc1edf542f5c5c30bf1c&widgetname=outstream&infeed&itemid=354852121
     */
    @Headers({
        "Content-Type: text/xml; charset=utf-8",
        "Accept: text/xml"
    })
    @GET("video/vast/")
    Observable<ResponseBody> getVASTDataRAW(@Query("publickey")  String publicKey,
                                            @Query("widgetname") String widgetname,
                                            @Query("infeed")     String infeed,
                                            @Query("itemid")     String itemid,
                                            @Query("test")       String test);

    /**
     * Same call as above but consumes a complete url
     * @param url
     * @return
     */
    @GET
    Observable<ResponseBody> getVASTDataRAW(@Url String url);

}
