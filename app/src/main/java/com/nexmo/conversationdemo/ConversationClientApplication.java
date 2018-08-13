package com.nexmo.conversationdemo;

import android.app.Application;

import com.androidnetworking.AndroidNetworking;
import com.jacksonandroidnetworking.JacksonParserFactory;
import com.nexmo.sdk.conversation.client.ConversationClient;

public class ConversationClientApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidNetworking.initialize(getApplicationContext());
        AndroidNetworking.setParserFactory(new JacksonParserFactory());
    }
}
