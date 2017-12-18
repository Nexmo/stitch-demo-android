package com.nexmo.conversationdemo;

import android.app.Application;

import com.androidnetworking.AndroidNetworking;
import com.jacksonandroidnetworking.JacksonParserFactory;
import com.nexmo.sdk.conversation.client.ConversationClient;

public class ConversationClientApplication extends Application {

    private ConversationClient conversationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        conversationClient = new ConversationClient.ConversationClientBuilder().context(this).build();
        AndroidNetworking.initialize(getApplicationContext());
        AndroidNetworking.setParserFactory(new JacksonParserFactory());
    }

    public ConversationClient getConversationClient() {
        return conversationClient;
    }
}
