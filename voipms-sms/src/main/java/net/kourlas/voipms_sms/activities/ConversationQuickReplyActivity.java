/*
 * VoIP.ms SMS
 * Copyright (C) 2015 Michael Kourlas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.kourlas.voipms_sms.activities;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.*;
import net.kourlas.voipms_sms.*;
import net.kourlas.voipms_sms.model.Message;
import net.kourlas.voipms_sms.notifications.Notifications;

public class ConversationQuickReplyActivity extends AppCompatActivity {
    private final ConversationQuickReplyActivity activity = this;

    private Database database;
    private Preferences preferences;

    private String contact;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_quick_reply);

        database = Database.getInstance(getApplicationContext());
        preferences = Preferences.getInstance(getApplicationContext());

        contact = getIntent().getExtras().getString(getString(R.string.conversation_extra_contact));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Integer notificationId = Notifications
            .getInstance(getApplicationContext()).getNotificationIds().get(contact);
        if (notificationId != null) {
            manager.cancel(notificationId);
        }

        TextView replyToText = (TextView) findViewById(R.id.reply_to_edit_text);
        String contactName = Utils.getContactName(getApplicationContext(), contact);
        if (contactName == null) {
            replyToText.setText(getString(R.string.conversation_quick_reply_reply_to) + " " +
                    Utils.getFormattedPhoneNumber(contact));
        }
        else {
            replyToText.setText(getString(R.string.conversation_quick_reply_reply_to) + " " + contactName);
        }

        final EditText messageText = (EditText) findViewById(R.id.message_edit_text);
        Message draftMessage = database.getDraftMessageForConversation(preferences.getDid(), contact);
        if (draftMessage != null) {
            ViewSwitcher viewSwitcher =
                (ViewSwitcher) findViewById(R.id.view_switcher);
            viewSwitcher.setDisplayedChild(1);
            messageText.setText(draftMessage.getText());
            messageText.requestFocus();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            messageText.setOutlineProvider(new ViewOutlineProvider() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 15);
                }
            });
            messageText.setClipToOutline(true);
        }
        messageText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                ViewSwitcher viewSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);
                if (s.toString().equals("") && viewSwitcher.getDisplayedChild() == 1) {
                    viewSwitcher.setDisplayedChild(0);
                }
                else if (viewSwitcher.getDisplayedChild() == 0) {
                    viewSwitcher.setDisplayedChild(1);
                }

                Message previousDraftMessage = database.getDraftMessageForConversation(
                    preferences.getDid(), contact);
                String newDraftMessageString = s.toString();
                if (newDraftMessageString.equals("")) {
                    if (previousDraftMessage != null) {
                        database.removeMessage(
                            previousDraftMessage.getDatabaseId());
                    }
                } else {
                    if (previousDraftMessage != null) {
                        previousDraftMessage.setText(newDraftMessageString);
                        database.insertMessage(previousDraftMessage);
                    } else {
                        Message newDraftMessage = new Message(
                            preferences.getDid(), contact,
                            newDraftMessageString);
                        newDraftMessage.setDraft(true);
                        database.insertMessage(newDraftMessage);
                    }
                }
            }
        });

        QuickContactBadge photo = (QuickContactBadge) findViewById(R.id.photo);
        Utils.applyCircularMask(photo);
        photo.assignContactFromPhone(Preferences.getInstance(
            getApplicationContext()).getDid(), true);
        String photoUri = Utils.getContactPhotoUri(
            getApplicationContext(),
            Preferences.getInstance(getApplicationContext()).getDid());
        if (photoUri != null) {
            photo.setImageURI(Uri.parse(photoUri));
        }
        else {
            photo.setImageToDefault();
        }

        final ImageButton sendButton = (ImageButton) findViewById(R.id.send_button);
        Utils.applyCircularMask(sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preSendMessage();
            }
        });

        Button openAppButton = (Button) findViewById(R.id.open_app_button);
        openAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

                Intent intent = new Intent(activity, ConversationActivity.class);
                intent.putExtra(getString(R.string.conversation_extra_contact), contact);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                stackBuilder.addParentStack(ConversationActivity.class);
                stackBuilder.addNextIntent(intent);
                stackBuilder.startActivities();
            }
        });
        messageText.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityMonitor.getInstance().setCurrentActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ActivityMonitor.getInstance().deleteReferenceToActivity(this);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityMonitor.getInstance().deleteReferenceToActivity(this);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void preSendMessage() {
        EditText messageEditText = (EditText) findViewById(R.id.message_edit_text);
        String messageText = messageEditText.getText().toString();
        while (true) {
            if (messageText.length() > 160) {
                long databaseId = database.insertMessage(new Message(preferences.getDid(), contact,
                        messageText.substring(0, 160)));
                messageText = messageText.substring(160);
                database.sendMessage(this, databaseId);
            }
            else {
                long databaseId = database.insertMessage(new Message(preferences.getDid(), contact,
                        messageText.substring(0, messageText.length())));
                database.sendMessage(this, databaseId);
                break;
            }
        }
        messageEditText.setText("");
        finish();
    }

    public void postSendMessage(boolean success, long databaseId) {
        if (success) {
            database.removeMessage(databaseId);
            database.synchronize(true, false, null);
        }
        else {
            Message message = database.getMessageWithDatabaseId(databaseId);
            message.setDelivered(false);
            message.setDeliveryInProgress(false);
            database.insertMessage(message);
        }
    }
}
