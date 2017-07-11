package com.plista.demo.infeed.data_request;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class ApiFactory {

    public static final int HTTP_CLIENT_TIMEOUT_SECONDS    = 30;

    public static final String BASE_URL = "http://farm.plista.com/";

    private static DataService service;


    public static DataService getService() {

        if (service == null) {
            initialize();
        }

        return service;
    }

    private static void initialize() {

        final OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
            .connectTimeout(HTTP_CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        final HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        okHttpClientBuilder.addInterceptor(logging);
        final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .client(okHttpClientBuilder.build())
            .build();

        service = retrofit.create(DataService.class);
    }
}
