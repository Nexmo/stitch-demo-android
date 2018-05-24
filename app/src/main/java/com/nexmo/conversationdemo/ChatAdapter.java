package com.nexmo.conversationdemo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.Event;
import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.MemberMedia;
import com.nexmo.sdk.conversation.client.SeenReceipt;
import com.nexmo.sdk.conversation.client.Text;
import com.nexmo.sdk.conversation.client.event.EventType;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private static final String TAG = "ChatAdapter";
    private Member self;
    private Conversation conversation;

    ChatAdapter(Conversation conversation) {
        self = conversation.getSelf();
        this.conversation = conversation;
    }

    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.chat_item, parent, false);

        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(ChatAdapter.ViewHolder holder, int position) {
        if (conversation.getEvents().get(position).getType().equals(EventType.TEXT)) {
            final Text textMessage = (Text) conversation.getEvents().get(position);
            if (!textMessage.getMember().equals(self) && !memberHasSeen(textMessage)) {
                textMessage.markAsSeen(new RequestHandler<SeenReceipt>() {
                    @Override
                    public void onSuccess(SeenReceipt result) {
                        //Left blank
                    }

                    @Override
                    public void onError(NexmoAPIError apiError) {
                        Log.d(TAG, "mark as seen onError: " + apiError.getMessage());
                    }
                });
            }
            holder.text.setText(textMessage.getMember().getName() + ": " + textMessage.getText());
            if (textMessage.getSeenReceipts().isEmpty()) {
                holder.seenIcon.setVisibility(View.INVISIBLE);
            } else {
                holder.seenIcon.setVisibility(View.VISIBLE);
            }
        } else if (conversation.getEvents().get(position).getType().equals(EventType.MEMBER_MEDIA)) {
            final MemberMedia mediaMessage = (MemberMedia) conversation.getEvents().get(position);
            holder.text.setText(mediaMessage.getMember().getName() + (mediaMessage.isAudioEnabled() ? " enabled" : " disabled") + " audio.");
            holder.seenIcon.setVisibility(View.INVISIBLE);
        }
    }

    private boolean memberHasSeen(Text textMessage) {
        boolean seen = false;
        for (SeenReceipt receipt : textMessage.getSeenReceipts()) {
            if (receipt.getMember().equals(self)) {
                seen = true;
                break;
            }
        }
        return seen;
    }

    @Override
    public int getItemCount() {
        return conversation.getEvents().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView text;
        private final ImageView seenIcon;

        public ViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.item_chat_txt);
            seenIcon = itemView.findViewById(R.id.item_chat_seen_img);
        }
    }
}
