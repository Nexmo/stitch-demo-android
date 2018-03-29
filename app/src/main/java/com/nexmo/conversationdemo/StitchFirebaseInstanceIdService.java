package com.nexmo.conversationdemo;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
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

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
//        sendRegistrationToServer(refreshedToken);

        final ConversationClientApplication app = (ConversationClientApplication) getApplication();
        ConversationClient client = app.getConversationClient();
        client.setPushDeviceToken(refreshedToken);
        client.enableAllPushNotifications(true, new RequestHandler<Void>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                Log.d(TAG, "enableAllPushNotifications onError: " + apiError.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "enableAllPushNotifications onSuccess: ");
            }
        });
    }
}
