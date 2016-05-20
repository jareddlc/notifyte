package com.solderbyte.notifyte;

import android.app.PendingIntent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;

import java.util.ArrayList;
import java.util.UUID;

public class NotifyteNotification {
    public String uuid = UUID.randomUUID().toString();
    public PendingIntent pendingIntent;
    public ArrayList<RemoteInput> remoteInputs = new ArrayList<>();
    public ArrayList<NotifyteNotification> pages = new ArrayList<>();
    public Bundle bundle;
    public String tag;

    public String appName = null;
    public String packageName = null;
    public String name = null;
    public String contact = null;
    public String group = null;
    public String message = null;
    public long created = 0;
    public int id = 0;
}
