package com.nexmo.conversationdemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.ConversationClient;
import com.nexmo.sdk.conversation.client.Event;
import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.SeenReceipt;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.client.event.ResultListener;
import com.nexmo.sdk.conversation.client.event.container.Receipt;
import com.nexmo.sdk.conversation.core.SubscriptionList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private String TAG = ChatActivity.class.getSimpleName();

    private EditText chatBox;
    private ImageButton sendBtn;
    private TextView typingNotificationTxt;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;

    private ConversationClient conversationClient;
    private Conversation conversation;
    private SubscriptionList subscriptions = new SubscriptionList();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        conversationClient = ((ConversationClientApplication) getApplication()).getConversationClient();
        Intent intent = getIntent();
        String conversationId = intent.getStringExtra("CONVERSATION_ID");
        conversation = conversationClient.getConversation(conversationId);

        recyclerView = (RecyclerView) findViewById(R.id.recycler);
        chatAdapter = new ChatAdapter(conversation);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ChatActivity.this);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);

        chatBox = (EditText) findViewById(R.id.chat_box);
        sendBtn = (ImageButton) findViewById(R.id.send_btn);
        typingNotificationTxt = (TextView) findViewById(R.id.typing_notification);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        attachListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        subscriptions.unsubscribeAll();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_users:
                showUsersDialog();
                return true;
            case R.id.invite_users:
                inviteUser();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void inviteUser() {
        final EditText input = new EditText(ChatActivity.this);
        final AlertDialog.Builder dialog = new AlertDialog.Builder(ChatActivity.this)
                .setTitle("Enter username to invite")
                .setPositiveButton("Invite", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        joinUser(input.getText().toString());
                    }
                });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        dialog.setView(input);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });
    }

    private void joinUser(String username) {
        conversation.join(username, new RequestHandler<Member>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                logAndShow(apiError.getMessage());
            }

            @Override
            public void onSuccess(Member member) {
                logAndShow(member + " invited.");
            }
        });
    }

    private void showUsersDialog() {
        final List<Member> members = conversation.getMembers();
        List<String> memberNames = new ArrayList<>(conversation.getMembers().size());
        for (Member member: members) {
            memberNames.add(member.getName());
        }

        final AlertDialog.Builder dialog = new AlertDialog.Builder(ChatActivity.this)
                .setTitle("View or edit members")
                .setItems(memberNames.toArray(new CharSequence[memberNames.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showUserInfoDialog(members.get(which));
                    }
                });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });
    }

    private void showUserInfoDialog(final Member member) {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(ChatActivity.this)
                .setTitle(member.getName())
                .setMessage("ID: " + member.getMemberId())
                .setPositiveButton("Kick", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        member.kick(new RequestHandler<Void>() {
                            @Override
                            public void onError(NexmoAPIError apiError) {
                                logAndShow(apiError.getMessage());
                            }

                            @Override
                            public void onSuccess(Void result) {
                                logAndShow(member.getName() + " kicked");
                            }
                        });
                    }
                });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });
    }

    private void attachListeners() {
        conversation.messageEvent().add(new ResultListener<Event>() {
            @Override
            public void onSuccess(Event result) {
                chatAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(chatAdapter.getItemCount());
            }
        }).addTo(subscriptions);

        chatBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //intentionally left blank
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //intentionally left blank
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    sendTypeIndicator(Member.TYPING_INDICATOR.ON);
                } else {
                    sendTypeIndicator(Member.TYPING_INDICATOR.OFF);
                }
            }
        });

        conversation.typingEvent().add(new ResultListener<Member>() {
            @Override
            public void onSuccess(final Member member) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String typingMsg = member.getTypingIndicator().equals(Member.TYPING_INDICATOR.ON) ? member.getName() + " is typing" : null;
                        typingNotificationTxt.setText(typingMsg);
                    }
                });
            }
        }).addTo(subscriptions);

        conversation.seenEvent().add(new ResultListener<Receipt<SeenReceipt>>() {
            @Override
            public void onSuccess(Receipt<SeenReceipt> result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chatAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).addTo(subscriptions);
    }

    private void sendTypeIndicator(Member.TYPING_INDICATOR typingIndicator) {
        switch (typingIndicator){
            case ON: {
                conversation.startTyping(new RequestHandler<Member.TYPING_INDICATOR>() {
                    @Override
                    public void onSuccess(Member.TYPING_INDICATOR typingIndicator) {
                        //intentionally left blank
                    }

                    @Override
                    public void onError(NexmoAPIError apiError) {
                        logAndShow("Error start typing: " + apiError.getMessage());
                    }
                });
                break;
            }
            case OFF: {
                conversation.stopTyping(new RequestHandler<Member.TYPING_INDICATOR>() {
                    @Override
                    public void onSuccess(Member.TYPING_INDICATOR typingIndicator) {
                        //intentionally left blank
                    }

                    @Override
                    public void onError(NexmoAPIError apiError) {
                        logAndShow("Error stop typing: " + apiError.getMessage());
                    }
                });
                break;
            }
        }
    }

    private void sendMessage() {
        conversation.sendText(chatBox.getText().toString(), new RequestHandler<Event>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                logAndShow("Error sending message: " + apiError.getMessage());
            }

            @Override
            public void onSuccess(Event result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chatBox.setText(null);
                    }
                });
            }
        });
    }

    private void logAndShow(final String message) {
        Log.d(TAG, message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ChatActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
