package com.solderbyte.notifyte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationService extends NotificationListenerService {
    private static final String LOG_TAG = "Notifyte:Notification";

    private ArrayList<String> ListPackageNames = new ArrayList<String>();
    private ArrayList<NotifyteNotification> listNotifyte = new ArrayList<NotifyteNotification>();

    // view data
    private String NOTIFICATION_TITLE = null;
    private String NOTIFICATION_TEXT = null;
    private String NOTIFICATION_BIG_TEXT = null;

    // applications
    private String APP_FB_MESSENGER = "com.facebook.orca";
    private String APP_WHATSAPP = "com.whatsapp";
    private String APP_G_HANGOUTS = "com.google.android.talk";

    // global
    private PackageManager packageManager = null;
    private Context context = null;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "Created NotificationService");
        this.registerReceiver(serviceStopReceiver, new IntentFilter(Intents.INTENT_SERVICE_STOP));
        this.registerReceiver(applicationsReceiver, new IntentFilter(Intents.INTENT_APPLICATION));
        this.registerReceiver(bluetoothLeReceiver, new IntentFilter(Intents.INTENT_BLUETOOTH));

        context = NotificationService.this.getApplicationContext();
        packageManager = this.getPackageManager();
        this.checkNotificationListenerService();

        Intent msg = new Intent(Intents.INTENT_NOTIFICATION_START);
        context.sendBroadcast(msg);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        unregisterReceiver(serviceStopReceiver);
        unregisterReceiver(applicationsReceiver);
        unregisterReceiver(bluetoothLeReceiver);
        NotificationService.this.stopSelf();
        super.onDestroy();
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
        String GROUP = null;
        String MESSAGE = null;
        int ID = id;
        long CREATED = time;

        if(packageName.equals(APP_FB_MESSENGER)) {
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
        else if(packageName.equals(APP_G_HANGOUTS)) {
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
        else if(packageName.equals(APP_WHATSAPP)) {
            // whatsapp
            // appName: WhatsApp
            // packageName: com.whatsapp
            // name: title or view title (group: name @ group)
            // contact: n/a
            // message: message or view text big text

            try {
                if(message.matches(".*(\\d+).new messages.*") || NOTIFICATION_TEXT.matches(".*(\\d+).new messages.*")) {
                    Log.d(LOG_TAG, "ignoring message");
                    return;
                }
            }
            catch(Exception e) {
                Log.w(LOG_TAG, "regex error: " + e.getMessage());
            }

            String[] split;
            if(title != null) {
                // group message
                if(title.contains("@")) {
                    split = title.split("(.+)@(.+)");
                    NAME = split[0];
                    GROUP = split[1];
                }
                else {
                    NAME = title;
                }
            }
            else if(NOTIFICATION_TITLE != null) {
                if(title.contains("@")) {
                    split = NOTIFICATION_TITLE.split("(.+)@(.+)");
                    NAME = split[0];
                    GROUP = split[1];
                }
                else {
                    NAME = NOTIFICATION_TITLE;
                }
            }
            else {
                NAME = title;
            }
            if(NOTIFICATION_BIG_TEXT != null) {
                MESSAGE = NOTIFICATION_BIG_TEXT;
            }
            else if(NOTIFICATION_TEXT != null) {
                MESSAGE = NOTIFICATION_TEXT;
            }
            else {
                MESSAGE = message;
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

        RemoteInput[] remoteInputs = this.getRemoteInputs(notification);
        NotificationCompat.Action action = this.getAction(notification);
        if(remoteInputs != null || action != null) {
            NotifyteNotification notifyte = new NotifyteNotification();
            notifyte.appName = APP_NAME;
            notifyte.packageName = PACKAGE_NAME;
            notifyte.name = NAME;
            notifyte.contact = CONTACT;
            notifyte.group = GROUP;
            notifyte.message = MESSAGE;
            notifyte.created = CREATED;
            notifyte.id = ID;
            notifyte.tag = tag;
            notifyte.bundle = extras;
            notifyte.action = action;
            notifyte.pendingIntent = notification.contentIntent;
            notifyte.remoteInputs.addAll(Arrays.asList(remoteInputs));
            listNotifyte.add(notifyte);
            Log.d(LOG_TAG, "remoteInputs: " + remoteInputs.length);
        }

        Intent msg = new Intent(Intents.INTENT_NOTIFICATION);
        msg.putExtra(Intents.INTENT_NOTIFICATION_APP_NAME, APP_NAME);
        msg.putExtra(Intents.INTENT_NOTIFICATION_PACKAGE_NAME, PACKAGE_NAME);
        msg.putExtra(Intents.INTENT_NOTIFICATION_NAME, NAME);
        msg.putExtra(Intents.INTENT_NOTIFICATION_CONTACT, CONTACT);
        msg.putExtra(Intents.INTENT_NOTIFICATION_GROUP, GROUP);
        msg.putExtra(Intents.INTENT_NOTIFICATION_MESSAGE, MESSAGE);
        msg.putExtra(Intents.INTENT_NOTIFICATION_CREATED, CREATED);
        msg.putExtra(Intents.INTENT_NOTIFICATION_ID, ID);
        if(action != null) {
            msg.putExtra(Intents.INTENT_NOTIFICATION_REPLY, true);
        }

        Log.d(LOG_TAG, "sendBroadcast");
        context.sendBroadcast(msg);
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

    public void checkNotificationListenerService() {
        Log.d(LOG_TAG, "checkNotificationListenerService");
        boolean isNotificationListenerRunning = false;
        ComponentName thisComponent = new ComponentName(this, NotificationService.class);
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if(runningServices == null) {
            Log.d(LOG_TAG, "running services is null");
            return;
        }
        for(ActivityManager.RunningServiceInfo service : runningServices) {
            if(service.service.equals(thisComponent)) {
                Log.d(LOG_TAG, "checkNotificationListenerService service - pid: " + service.pid + ", currentPID: " + Process.myPid() + ", clientPackage: " + service.clientPackage + ", clientCount: " + service.clientCount + ", clientLabel: " + ((service.clientLabel == 0) ? "0" : "(" + getResources().getString(service.clientLabel) + ")"));
                if(service.pid == Process.myPid() /*&& service.clientCount > 0 && !TextUtils.isEmpty(service.clientPackage)*/) {
                    isNotificationListenerRunning = true;
                }
            }
        }
        if(isNotificationListenerRunning) {
            Log.d(LOG_TAG, "NotificationListenerService is running");
            return;
        }
        Log.d(LOG_TAG, "NotificationListenerService is not running, trying to start");
        this.toggleNotificationListenerService();
    }

    public void toggleNotificationListenerService() {
        Log.d(LOG_TAG, "toggleNotificationListenerService");
        // adb shell dumpsys notification
        // force start of notification service
        ComponentName thisComponent = new ComponentName(this, NotificationService.class);
        packageManager.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        packageManager.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    public String getAppName(String packageName) {
        Log.d(LOG_TAG, "getAppName");
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
        Log.d(LOG_TAG, "getViewNotification");
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

    public RemoteInput[] getRemoteInputs(Notification notification) {
        Log.d(LOG_TAG, "getRemoteInputs");
        RemoteInput[] remoteInputs = null;
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        Log.d(LOG_TAG, "wearableExtender: " + wearableExtender);
        List<NotificationCompat.Action> actions = wearableExtender.getActions();
        Log.d(LOG_TAG, "actions: " + actions);
        for(NotificationCompat.Action act : actions) {
            if(act != null && act.getRemoteInputs() != null) {
                Log.d(LOG_TAG, "act: " + act.getTitle());
                remoteInputs = act.getRemoteInputs();
                Log.d(LOG_TAG, "remoteInputs: " + remoteInputs.toString());
            }
        }
        return remoteInputs;
    }

    public NotificationCompat.Action getAction(Notification notification) {
        Log.d(LOG_TAG, "getAction");
        RemoteInput[] remoteInputs = null;
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        Log.d(LOG_TAG, "wearableExtender: " + wearableExtender);
        if(wearableExtender.getActions().size() > 0) {
            for(NotificationCompat.Action action : wearableExtender.getActions()) {
                if(action.title.toString().toLowerCase().contains("reply")) {
                    return action;
                }
            }
        }
        return null;
    }

    public android.app.RemoteInput[] getRemoteInputsFromBundle(Bundle bundle) {
        Log.d(LOG_TAG, "getRemoteInputsFromBundle");
        android.app.RemoteInput[] remoteInputs = null;

        for(String key : bundle.keySet()) {
            Object value = bundle.get(key);

            if("android.wearable.EXTENSIONS".equals(key)) {
                Bundle wearBundle = ((Bundle) value);
                for(String keyInner : wearBundle.keySet()) {
                    Object valueInner = wearBundle.get(keyInner);

                    if(keyInner != null && valueInner != null) {
                        if("actions".equals(keyInner) && valueInner instanceof ArrayList) {
                            ArrayList<Notification.Action> actions = new ArrayList<>();
                            actions.addAll((ArrayList) valueInner);
                            for(Notification.Action act : actions) {
                                if(Build.VERSION.SDK_INT >= 20) {
                                    if(act.getRemoteInputs() != null) {
                                        remoteInputs = act.getRemoteInputs();
                                    }
                                }
                                else {
                                    Log.e(LOG_TAG, "Failed to get remoteInput. API 20 required");
                                }
                            }
                        }
                    }
                }
            }
        }

        return remoteInputs;
    }

    public NotifyteNotification getNotifyte(JSONObject json) {
        Log.d(LOG_TAG, "getNotifyte");
        String replyPackageName = null;
        String replyName = null;
        try {
            replyPackageName = json.getString(Intents.JSON_REPLY_PACKAGE_NAME);
            replyName = json.getString(Intents.JSON_REPLY_NAME);
        }
        catch(JSONException e) {
            Log.e(LOG_TAG, "Error: getNotifyte could not get: " + Intents.JSON_REPLY_PACKAGE_NAME);
            e.printStackTrace();
            return null;
        }

        for(int i = 0; i < listNotifyte.size(); i++) {
            if(listNotifyte.get(i).packageName.equals(replyPackageName) && listNotifyte.get(i).name.equals(replyName)) {
                return listNotifyte.get(i);
            }
        }
        return null;
    }

    public void replyToNotification(JSONObject json) {
        Log.d(LOG_TAG, "replyToNotification");
        Log.d(LOG_TAG, json.toString());
        NotifyteNotification notifyte = getNotifyte(json);
        RemoteInput[] remoteInputs = null;
        String message = null;
        try {
            remoteInputs = new RemoteInput[notifyte.remoteInputs.size()];
            Log.d(LOG_TAG, "RemoteInput: " + notifyte.remoteInputs.size());
        }
        catch(NullPointerException e) {
            Log.e(LOG_TAG, "Error: no remoteInputs");
            return;
        }

        Log.d(LOG_TAG, "notifyte: " + notifyte.appName);

        try {
            message = json.getString(Intents.JSON_MESSAGE);
        }
        catch(JSONException e) {
            Log.e(LOG_TAG, "Error: replyToNotification could not get: " + Intents.JSON_MESSAGE);
            return;
        }

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // method 1
        //Bundle bundle = notifyte.bundle;
        // method 2
        Bundle bundle = new Bundle();
        int i = 0;

        // method 1
        /*for(RemoteInput remoteInput : notifyte.remoteInputs){
            this.getRemoteInputInfo(remoteInput);
            remoteInputs[i] = remoteInput;
            Log.d(LOG_TAG, "remoteInput: " + remoteInput.getResultKey());
            bundle.putCharSequence(remoteInputs[i].getResultKey(), message);
            i++;
        }
        RemoteInput.addResultsToIntent(remoteInputs, intent, bundle);*/
        // method 2
        for(RemoteInput remoteIn : notifyte.action.getRemoteInputs()) {
            Log.d(LOG_TAG, "remoteInput: " + remoteIn.getLabel());
            bundle.putCharSequence(remoteIn.getResultKey(), message);
        }
        RemoteInput.addResultsToIntent(notifyte.action.getRemoteInputs(), intent, bundle);

        // method 1
        /*try {
            Log.d(LOG_TAG, "trying to send");
            notifyte.pendingIntent.send(this, 0, intent);
        }
        catch (PendingIntent.CanceledException e) {
            Log.e(LOG_TAG, "Error: replyToNotification " + e);
            e.printStackTrace();
        }*/
        // method 2
        try {
            notifyte.action.actionIntent.send(this, 0, intent);
        }
        catch(PendingIntent.CanceledException e) {
            Log.e(LOG_TAG, "Error: replyToNotification " + e);
            e.printStackTrace();
        }
    }

    private void getRemoteInputInfo(RemoteInput remoteInput) {
        Log.d(LOG_TAG, "getRemoteInputInfo");
        String resultKey = remoteInput.getResultKey();
        String label = remoteInput.getLabel().toString();
        Log.d(LOG_TAG, "resultKey: " + resultKey);
        Log.d(LOG_TAG, "label: " + label);
        Boolean canFreeForm = remoteInput.getAllowFreeFormInput();
        if(remoteInput.getChoices() != null && remoteInput.getChoices().length > 0) {
            String[] possibleChoices = new String[remoteInput.getChoices().length];
            for(int i = 0; i < remoteInput.getChoices().length; i++){
                possibleChoices[i] = remoteInput.getChoices()[i].toString();
            }
        }
    }

    private BroadcastReceiver serviceStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "serviceStopReceiver");
            unregisterReceiver(applicationsReceiver);
            unregisterReceiver(serviceStopReceiver);
            unregisterReceiver(bluetoothLeReceiver);
            NotificationService.this.stopSelf();
        }
    };

    private BroadcastReceiver applicationsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "applicationsReceiver");
            ArrayList<String> applications = intent.getStringArrayListExtra(Intents.INTENT_EXTRA_DATA);
            ListPackageNames = applications;
            Log.d(LOG_TAG, "Received listeningApps: " + applications.size());
        }
    };

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "bluetoothLeReceiver");

            String message = intent.getStringExtra(Intents.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, message);

            if(message.equals(Intents.INTENT_BLUETOOTH_NOTIFICATION)) {
                Log.d(LOG_TAG, "Bluetooth notification");
                String data = intent.getStringExtra(Intents.INTENT_EXTRA_DATA);
                JSONObject json = null;
                try {
                    json = new JSONObject(data);
                    NotificationService.this.replyToNotification(json);
                }
                catch(JSONException e) {
                    Log.e(LOG_TAG, "Error: converting string to json" + e);
                    e.printStackTrace();
                }
            }
        }
    };
}
