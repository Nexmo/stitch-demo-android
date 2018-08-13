package com.nexmo.conversationdemo;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.Image;
import com.nexmo.sdk.conversation.client.Text;
import com.nexmo.sdk.conversation.client.event.container.Invitation;
import com.nexmo.sdk.conversation.push.PushNotification;

import static com.nexmo.sdk.conversation.push.PushNotification.CONVERSATION_PUSH_ACTION;
import static com.nexmo.sdk.conversation.push.PushNotification.CONVERSATION_PUSH_TYPE;

public class StitchPushReceiver extends BroadcastReceiver {
    private static final String TAG = StitchPushReceiver.class.getSimpleName();
    private static final String NOTIFICATION_CHANNEL_ID = "misc";
    private static final String NOTIFICATION_CHANNEL_NAME = "Miscellaneous";
    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        Bundle bundle = intent.getExtras();

        if (bundle != null && intent.getAction() != null && intent.getAction().equals(CONVERSATION_PUSH_ACTION) && bundle.containsKey(CONVERSATION_PUSH_TYPE)) {
            String type = bundle.getString(CONVERSATION_PUSH_TYPE);
            if (type != null) {
                switch(type) {
                    case CONVERSATION_PUSH_ACTION: {
                        Log.d(TAG, "push action");
                        Text text = bundle.getParcelable(Text.class.getSimpleName());
                        Log.d(TAG, text != null ? text.toString() : null);
                        showNotification(context, text);
                    }
                    case PushNotification.ACTION_TYPE_TEXT: {
                        Log.d(TAG, PushNotification.ACTION_TYPE_TEXT);
                        Text text = bundle.getParcelable(Text.class.getSimpleName());
                        Log.d(TAG, text != null ? text.toString() : null);
                        showNotification(context, text);
                        break;
                    }
                    case PushNotification.ACTION_TYPE_IMAGE: {
                        Log.d(TAG, PushNotification.ACTION_TYPE_IMAGE);
                        Image image = bundle.getParcelable(Image.class.getSimpleName());
                        Log.d(TAG, image != null ? image.toString() : null);
                        showNotification(context, image);
                        break;
                    }
                    case PushNotification.ACTION_TYPE_INVITE: {
                        Log.d(TAG, PushNotification.ACTION_TYPE_INVITE);
                        Invitation invitation = bundle.getParcelable(Invitation.class.getSimpleName());
                        Log.d(TAG, invitation != null ? invitation.toString() : null);
                        showNotification(context, invitation);
                        break;
                    }
                    default: {
                        Log.d(TAG, "unknown push notification action");
                        showNotification(context, "New event");
                        break;
                    }
                }
            }


        }
    }

    private void showNotification(final Context context, Text text) {
        if (text != null) {
            showNotification(context, text.getText(), text.getConversation(), false);
        } else {
            showNotification(context, "New Text Event");
        }
    }

    private void showNotification(final Context context, Image image) {
        if (image != null) {
            showNotification(context, "Image from " + image.getMember().getName(), image.getConversation(), false);
        } else {
            showNotification(context, "New Image Event");
        }
    }

    private void showNotification(final Context context, Invitation invitation) {
        if (invitation != null) {
            showNotification(context, "Invite from " + invitation.getInvitedBy(), invitation.getConversation(), true);
        } else {
            showNotification(context, "New Invitation");
        }
    }

    private void showNotification(final Context context, final String payload) {
        showNotification(context, payload, null, false);
    }

    /**
     * Manually display a notification indicating the user that a message was received.
     * If a notification with the same id has already been posted by your application and
     * has not yet been canceled, it will be replaced by the updated information.
     * @param payload           The notification message.
     */
    private void showNotification(final Context context, final String payload, Conversation conversation, boolean pendingInvitation) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Nexmo Stitch Notification")
                //max characters for a push notification is 4KB or about 1000 characters
                .setContentText(payload.substring(0, Math.min(1000, payload.length())))
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 100, 100, 100, 100, 100})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setSmallIcon(R.drawable.ic_send_black_24dp);

        if (conversation != null) {
            // Example action to take when a user taps the notification
            Intent resultIntent = new Intent(context, ChatActivity.class);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            resultIntent.putExtra("CONVERSATION_ID", conversation.getConversationId());
            resultIntent.putExtra("PENDING_INVITATION", pendingInvitation);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                    0,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            notificationBuilder.setContentIntent(resultPendingIntent);

        }



        if (notificationManager != null) {
            //On Android versions starting with Oreo (SDKversion 26), any local notification your app is trying to build needs to be attached to a NotificationChannel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                        NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(notificationChannel);
            }

            // Build and issue the notification. All pending notifications with same id will be canceled.
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }
}
