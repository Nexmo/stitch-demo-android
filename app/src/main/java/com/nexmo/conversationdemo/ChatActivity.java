package com.nexmo.conversationdemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.nexmo.conversationdemo.utils.Stitch;
import com.nexmo.sdk.conversation.client.Call;
import com.nexmo.sdk.conversation.client.CallEvent;
import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.ConversationClient;
import com.nexmo.sdk.conversation.client.Event;
import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.SeenReceipt;
import com.nexmo.sdk.conversation.client.audio.AppRTCAudioManager;
import com.nexmo.sdk.conversation.client.audio.AudioCallEventListener;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.client.event.ResultListener;
import com.nexmo.sdk.conversation.client.event.container.Receipt;
import com.nexmo.sdk.conversation.core.SubscriptionList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.RECORD_AUDIO;
import static com.nexmo.conversationdemo.LoginActivity.API_URL;

public class ChatActivity extends BaseActivity {
    private static final int PERMISSION_REQUEST_AUDIO = 0;
    private boolean AUDIO_ENABLED = false;
    private String TAG = ChatActivity.class.getSimpleName();

    private EditText chatBox;
    private TextView typingNotificationTxt;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;

    private ConversationClient conversationClient;
    private Conversation conversation;
    private SubscriptionList subscriptions = new SubscriptionList();
    private Call currentCall;
    private Menu optionsMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        conversationClient = Stitch.Companion.getInstance(this.getApplicationContext()).getConversationClient();
        Intent intent = getIntent();
        String conversationId = intent.getStringExtra("CONVERSATION_ID");
        boolean pendingInvitation = intent.getBooleanExtra("PENDING_INVITATION", false);

        conversation = conversationClient.getConversation(conversationId);

        if (pendingInvitation) {
            conversation.join(new RequestHandler<Member>() {
                @Override
                public void onError(NexmoAPIError apiError) {
                    logAndShow(apiError.getMessage());
                }

                @Override
                public void onSuccess(Member result) {
                    retrieveConversationHistory(conversation);
                }
            });
        }

        recyclerView = findViewById(R.id.recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ChatActivity.this);
        recyclerView.setLayoutManager(linearLayoutManager);
        chatAdapter = new ChatAdapter(conversation);
        recyclerView.setAdapter(chatAdapter);

        chatBox = findViewById(R.id.chat_box);
        ImageButton sendBtn = findViewById(R.id.send_btn);
        typingNotificationTxt = findViewById(R.id.typing_notification);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        setTitle(conversation.getDisplayName());
        retrieveConversationHistory(conversation);
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        optionsMenu = menu;
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_users:
                showUsersDialog();
                return true;
            case R.id.invite_users:
                getAllUsers();
                return true;
            case R.id.leave_conversation:
                leaveConversation();
                return true;
            case R.id.audio:
                requestAudio();
                return true;
            case R.id.hangup:
                hangup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void retrieveConversationHistory(final Conversation conversation) {
        conversation.updateEvents(null, null, new RequestHandler<Conversation>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                Log.e(TAG, " updateEvents onError: ", apiError);
                logAndShow("Error Updating Conversation: " + apiError.getMessage());
            }

            @Override
            public void onSuccess(final Conversation result) {
                showConversationHistory(result);
            }
        });
    }

    private void showConversationHistory(Conversation result) {
        this.conversation = result;
        chatAdapter.notifyDataSetChanged();
    }

    private void hangup() {
        if (currentCall != null) {
            currentCall.hangup(new RequestHandler<Void>() {
                @Override
                public void onError(NexmoAPIError apiError) {
                    logAndShow("Cannot hangup: " + apiError.toString());
                }

                @Override
                public void onSuccess(Void result) {
                    logAndShow("Call completed.");
                    showHangUpButton(false);
                }
            });

        }
    }

    private void requestAudio() {
        if (ContextCompat.checkSelfPermission(ChatActivity.this, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            toggleAudio();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO)) {
                logAndShow("Need permissions granted for Audio to work");
            } else {
                ActivityCompat.requestPermissions(ChatActivity.this, new String[]{RECORD_AUDIO}, PERMISSION_REQUEST_AUDIO);
            }
        }
    }

    private void toggleAudio() {
        if(AUDIO_ENABLED) {
            conversation.media(Conversation.MEDIA_TYPE.AUDIO).disable(new RequestHandler<Void>() {
                @Override
                public void onError(NexmoAPIError apiError) {
                    logAndShow(apiError.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    AUDIO_ENABLED = false;
                    logAndShow("Audio is disabled");
                }
            });
        } else {
            conversation.media(Conversation.MEDIA_TYPE.AUDIO).enable(new AudioCallEventListener() {
                @Override
                public void onRinging() {
                    logAndShow("Ringing");
                }

                @Override
                public void onCallConnected() {
                    logAndShow("Connected");
                    AUDIO_ENABLED = true;
                }

                @Override
                public void onCallEnded() {
                    logAndShow("Call Ended");
                    AUDIO_ENABLED = false;
                }

                @Override
                public void onGeneralCallError(NexmoAPIError apiError) {
                    logAndShow(apiError.getMessage());
                    AUDIO_ENABLED = false;
                }

                @Override
                public void onAudioRouteChange(AppRTCAudioManager.AudioDevice device) {
                    logAndShow("Audio Route changed");
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    toggleAudio();
                    break;
                } else {
                    logAndShow("Enable audio permissions to continue");
                    break;
                }
            }
            default: {
                logAndShow("Issue with onRequestPermissionsResult");
                break;
            }
        }
    }

    private void leaveConversation() {
        conversation.leave(new RequestHandler<Void>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                logAndShow(apiError.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                finish();
            }
        });
    }

    private void showInviteUserDialog(final List<String> names) {
        final CharSequence[] charSequenceItems = names.toArray(new CharSequence[names.size()]);
        final AlertDialog.Builder dialog = new AlertDialog.Builder(ChatActivity.this)
                .setTitle("Select user")
                .setItems(charSequenceItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        joinUser(names.get(which));
                    }
                });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });
    }

    private void getAllUsers() {
        AndroidNetworking.get(API_URL + "users")
                .setPriority(Priority.LOW)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        List<String> names = new ArrayList<>(response.length());
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject user = response.getJSONObject(i);
                                names.add(user.getString("name"));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        showInviteUserDialog(names);
                    }

                    @Override
                    public void onError(ANError anError) {
                        logAndShow(anError.getMessage());
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
                logAndShow(member.getName() + " invited.");
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
                .setNeutralButton("Call", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        conversationClient.call(Collections.singletonList(member.getName()), new RequestHandler<Call>() {
                            @Override
                            public void onError(NexmoAPIError apiError) {
                                logAndShow("Cannot initiate call: " + apiError.getMessage());
                                Log.e(TAG, "onError: ", apiError);
                            }

                            @Override
                            public void onSuccess(Call result) {
                                logAndShow("Waiting for users to join call: " + result.getName());
                                currentCall = result;
                                showHangUpButton(true);
                            }
                        });
                    }
                })
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

        //Listen for incoming calls
        conversationClient.callEvent().add(new ResultListener<Call>() {
            @Override
            public void onSuccess(final Call incomingCall) {
                logAndShow("answering Call");
                //Answer an incoming call
                incomingCall.answer(new RequestHandler<Void>() {
                    @Override
                    public void onError(NexmoAPIError apiError) {
                        logAndShow("Error answer: " + apiError.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        currentCall = incomingCall;
                        attachCallListeners(incomingCall);
                        showHangUpButton(true);
                    }
                });
            }
        });
    }

    private void showHangUpButton(boolean visible) {
        if (optionsMenu != null) {
            optionsMenu.findItem(R.id.hangup).setVisible(visible);
        }
    }

    private void attachCallListeners(Call incomingCall) {
        //Listen for incoming member events in a call
        ResultListener<CallEvent> callEventListener = new ResultListener<CallEvent>() {
            @Override
            public void onSuccess(CallEvent message) {
                Log.d(TAG, "callEvent : state: " + message.getState() + " .content:" + message.toString());
            }
        };
        incomingCall.event().add(callEventListener);
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
}
