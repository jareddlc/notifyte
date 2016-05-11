package com.solderbyte.notifyte;

import java.util.ArrayList;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

public class NotificationService extends NotificationListenerService {
    private static final String LOG_TAG = "Notifyte:Notification";

    private ArrayList<String> ListPackageNames = new ArrayList<String>();

    // view data
    private String NOTIFICATION_TITLE = null;
    private String NOTIFICATION_TEXT = null;
    private String NOTIFICATION_BIG_TEXT = null;

    // global
    private PackageManager packageManager = null;
    private Context context = null;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "Created NotificationService");
        this.registerReceiver(serviceStopReceiver, new IntentFilter(Intents.INTENT_SERVICE_STOP));
        this.registerReceiver(applicationsReceiver, new IntentFilter(Intents.INTENT_APPLICATION));
        context = NotificationService.this.getApplicationContext();
        packageManager = this.getPackageManager();

        Intent msg = new Intent(Intents.INTENT_NOTIFICATION_START);
        context.sendBroadcast(msg);
        super.onCreate();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(LOG_TAG, "onNotificationPosted");
        String packageName = sbn.getPackageName();
        String appName = this.getAppName(packageName);

        /*if(!ListPackageNames.contains(packageName)) {
            return;
        }*/

        // API v19
        Notification notification = sbn.getNotification();
        this.getViewNotification(notification, packageName);
        Bundle extras = notification.extras;

        if((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0) {
            return;
        }

        String ticker = null;
        String message = null;
        String submessage = null;
        String summary = null;
        String info = null;
        String title = null;
        try {
            ticker = (String) sbn.getNotification().tickerText;
        }
        catch(Exception e) {
            Log.d(LOG_TAG, "Notification does not have tickerText");
        }
        String tag = sbn.getTag();
        long time = sbn.getPostTime();
        int id = sbn.getId();

        if(extras.getCharSequence("android.title") != null) {
            title = extras.getString("android.title");
        }
        if(extras.getCharSequence("android.text") != null) {
            message = extras.getCharSequence("android.text").toString();
        }
        if(extras.getCharSequence("android.subText") != null) {
            submessage = extras.getCharSequence("android.subText").toString();
        }
        if(extras.getCharSequence("android.summaryText") != null) {
            summary = extras.getCharSequence("android.summaryText").toString();
        }
        if(extras.getCharSequence("android.infoText") != null) {
            info = extras.getCharSequence("android.infoText").toString();
        }

        Log.d(LOG_TAG, "Captured notification message: " + message + " from source:" + packageName);
        Log.d(LOG_TAG, "app name: " + appName);
        Log.d(LOG_TAG, "ticker: " + ticker);
        Log.d(LOG_TAG, "title: " + title);
        Log.d(LOG_TAG, "message: " + message);
        Log.d(LOG_TAG, "tag: " + tag);
        Log.d(LOG_TAG, "time: " + time);
        Log.d(LOG_TAG, "id: " + id);
        Log.d(LOG_TAG, "submessage: " + submessage);
        Log.d(LOG_TAG, "summary: " + summary);
        Log.d(LOG_TAG, "info: " + info);
        Log.d(LOG_TAG, "view title: " + NOTIFICATION_TITLE);
        Log.d(LOG_TAG, "view big text: " + NOTIFICATION_BIG_TEXT);
        Log.d(LOG_TAG, "view text: " + NOTIFICATION_TEXT);


        String APP_NAME = appName;
        String PACKAGE_NAME = packageName;
        String NAME = null;
        String CONTACT = null;
        String MESSAGE = null;
        int ID = id;
        long CREATED = time;

        if(packageName.equals("com.facebook.orca")) {
            // facebook messenger
            // appName: Messenger
            // packageName: com.facebook.orca
            // name: title or view title
            // contact: n/a
            // message: message or view big text

            if(title != null) {
                NAME = title;
            }
            else {
                NAME = NOTIFICATION_TITLE;
            }
            if(message != null) {
                MESSAGE = message;
            }
            else if(NOTIFICATION_BIG_TEXT != null) {
                MESSAGE = NOTIFICATION_BIG_TEXT;
            }
            else {
                MESSAGE = NOTIFICATION_TEXT;
            }
        }
        else if(packageName.equals("com.google.android.talk")) {
            // google hangouts
            // appName: Hangouts
            // packageName: com.google.android.talk
            // name: title or view title (name)
            // contact: summary or view text (email)
            // message: message or view big text

            if(title != null) {
                NAME = title;
            }
            else {
                NAME = NOTIFICATION_TITLE;
            }
            if(summary != null) {
                CONTACT = summary;
            }
            else {
                CONTACT = NOTIFICATION_TEXT;
            }
            if(message != null) {
                MESSAGE = message;
            }
            else {
                MESSAGE = NOTIFICATION_BIG_TEXT;
            }
        }
        else if(packageName.equals("com.whatsapp")) {
            // whatsapp
            // appName: WhatsApp
            // packageName: com.whatsapp
            // name: message or big text (has name:message format) or title  view title (before @ format)
            // contact: n/a
            // message: message or big text (has name:message format) filter out (X new messages)

            String[] split;
            if(title != null) {
                split = title.split("(.+):(.+)");
                NAME = split[0];
            }
            else if(NOTIFICATION_BIG_TEXT != null) {
                split = NOTIFICATION_BIG_TEXT.split("(.+):(.+)");
                NAME = split[0];
            }
            else {
                NAME = title;
            }
            if(message != null) {
                MESSAGE = message;
            }
            else if(NOTIFICATION_BIG_TEXT != null) {
                MESSAGE = NOTIFICATION_BIG_TEXT;
            }
            else {
                MESSAGE = NOTIFICATION_TEXT;
            }
        }
        else {
            if(title != null) {
                NAME = title;
            }
            else {
                NAME = NOTIFICATION_TITLE;
            }
            if(NOTIFICATION_BIG_TEXT != null) {
                MESSAGE = NOTIFICATION_BIG_TEXT;
            }
            else if(message != null) {
                MESSAGE = message;
            }
            else {
                MESSAGE = NOTIFICATION_TEXT;
            }
        }

        Intent msg = new Intent(Intents.INTENT_NOTIFICATION);
        msg.putExtra(Intents.INTENT_NOTIFICATION_APP_NAME, APP_NAME);
        msg.putExtra(Intents.INTENT_NOTIFICATION_PACKAGE_NAME, PACKAGE_NAME);
        msg.putExtra(Intents.INTENT_NOTIFICATION_NAME, NAME);
        msg.putExtra(Intents.INTENT_NOTIFICATION_CONTACT, CONTACT);
        msg.putExtra(Intents.INTENT_NOTIFICATION_MESSAGE, MESSAGE);
        msg.putExtra(Intents.INTENT_NOTIFICATION_CREATED, CREATED);
        msg.putExtra(Intents.INTENT_NOTIFICATION_ID, ID);

        context.sendBroadcast(msg);
        Log.d(LOG_TAG, "Sending notification message: " + MESSAGE + " from source:" + PACKAGE_NAME);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(LOG_TAG, "onNotificationRemoved");
        String packageName = sbn.getPackageName();
        String shortMsg = "";
        try {
            shortMsg = (String) sbn.getNotification().tickerText;
        }
        catch(Exception e) {

        }
        Log.d(LOG_TAG, "Removed notification message: " + shortMsg + " from source:" + packageName);
    }

    public String getAppName(String packageName) {
        ApplicationInfo appInfo = null;
        try {
            appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(LOG_TAG, "Cannot get application info");
        }
        String appName = (String) packageManager.getApplicationLabel(appInfo);
        return appName;
    }


    public boolean getViewNotification(Notification n, String packageName) {
        Resources resources = null;
        try {
            resources = packageManager.getResourcesForApplication(packageName);
        }
        catch(Exception e){
            Log.e(LOG_TAG, "Failed to get PackageManager: " + e.getMessage());
        }
        if(resources == null) {
            Log.e(LOG_TAG, "No PackageManager resources");
            return false;
        }

        int TITLE = resources.getIdentifier("android:id/title", null, null);
        int BIG_TEXT = resources.getIdentifier("android:id/big_text", null, null);
        int TEXT = resources.getIdentifier("android:id/text", null, null);

        RemoteViews views = n.bigContentView;
        if(views == null) {
            views = n.contentView;
        }
        if(views == null) {
            return false;
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
        views.reapply(getApplicationContext(), localView);

        TextView title = (TextView) localView.findViewById(TITLE);
        if(title != null) {
            NOTIFICATION_TITLE = title.getText().toString();
        }
        else {
            NOTIFICATION_TITLE = null;
        }
        TextView big = (TextView) localView.findViewById(BIG_TEXT);
        if(big != null) {
            NOTIFICATION_BIG_TEXT = big.getText().toString();
        }
        else {
            NOTIFICATION_BIG_TEXT = null;
        }
        TextView text = (TextView) localView.findViewById(TEXT);
        if(text != null) {
            NOTIFICATION_TEXT = text.getText().toString();
        }
        else {
            NOTIFICATION_TEXT = null;
        }

        return true;
    }

    private BroadcastReceiver serviceStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "serviceStopReceiver");
            unregisterReceiver(applicationsReceiver);
            unregisterReceiver(serviceStopReceiver);
            NotificationService.this.stopSelf();
        }
    };

    private BroadcastReceiver applicationsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> applications = intent.getStringArrayListExtra(Intents.INTENT_EXTRA_DATA);
            ListPackageNames = applications;
            Log.d(LOG_TAG, "Received listeningApps: " + applications.size());
        }
    };
}
