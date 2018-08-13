package com.nexmo.conversationdemo;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.nexmo.conversationdemo.utils.Stitch;
import com.nexmo.sdk.conversation.client.ConversationClient;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

public class StitchFirebaseInstanceIdService extends FirebaseInstanceIdService {
    public final String TAG = this.getClass().getName();

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);


        ConversationClient conversationClient = Stitch.Companion.getInstance(this.getApplicationContext()).getConversationClient();
        conversationClient.setPushDeviceToken(refreshedToken);
    }
}
