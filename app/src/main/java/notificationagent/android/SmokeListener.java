package notificationagent.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Process;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.UUID;
import notificationagent.PriorityEngine;
import notificationagent.SessionIntake;

/** Smoke-only callback. Deliberately does not access content, notification history or actions. */
public final class SmokeListener extends NotificationListenerService {
    static final String CHANNEL = "synthetic_smoke_v1";
    static final int ID = 72001;
    private static final SessionIntake SESSION = new SessionIntake();
    private static boolean connected;
    private static boolean consent;
    private static boolean received;
    private static String expectedTag;
    private static long deadline;

    static boolean systemApproved(Context context) {
        return context.getSystemService(NotificationManager.class).isNotificationListenerAccessGranted(
                new ComponentName(context, SmokeListener.class));
    }

    static synchronized boolean enable(Context context) {
        if (!connected || !systemApproved(context)) {
            disable();
            return false;
        }
        consent = SESSION.enableThisSession() && SESSION.approveChannel(
                context.getPackageName(), CHANNEL, PriorityEngine.Category.OTHER);
        return consent;
    }

    static synchronized void disable() {
        consent = false;
        expectedTag = null;
        deadline = 0;
        received = false;
        SESSION.disableThisSession();
    }

    static synchronized String status(Context context) {
        if (!systemApproved(context)) {
            disable();
            return "Accès Android désactivé.";
        }
        if (!connected) return "Accès accordé. En attente de la connexion Android.";
        if (!consent) return "Accès accordé. Session de test non activée.";
        if (received) return "Notification fictive reçue.";
        if (expectedTag == null) return "Session activée. Prête pour une notification fictive.";
        return SystemClock.elapsedRealtime() <= deadline ? "En attente du rappel Android."
                : "Aucun rappel reçu dans le délai. Vérifie que le canal est autorisé.";
    }

    static synchronized boolean canPost(Context context) {
        if (!systemApproved(context)) {
            disable();
            return false;
        }
        return connected && consent;
    }

    static synchronized void post(Context context, Notification note) {
        if (!canPost(context)) return;
        final String tag = "demo-" + UUID.randomUUID();
        expectedTag = tag;
        deadline = SystemClock.elapsedRealtime() + 10_000;
        received = false;
        try {
            context.getSystemService(NotificationManager.class).notify(tag, ID, note);
        } catch (RuntimeException error) {
            expectedTag = null;
            throw error;
        }
    }

    private static synchronized void connection(boolean value) {
        disable(); // Rebinding also requires fresh local consent.
        connected = value;
        SESSION.platformConnection(value);
    }

    @Override public void onListenerConnected() { connection(true); }
    @Override public void onListenerDisconnected() { connection(false); }
    @Override public void onDestroy() {
        connection(false);
        super.onDestroy();
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        accept(sbn);
    }

    private void accept(StatusBarNotification sbn) {
        synchronized (SmokeListener.class) {
            if (!connected || !consent || expectedTag == null || sbn == null
                    || SystemClock.elapsedRealtime() > deadline
                    || !systemApproved(this)
                    || !getPackageName().equals(sbn.getPackageName())
                    || !Process.myUserHandle().equals(sbn.getUser())
                    || sbn.getId() != ID || !expectedTag.equals(sbn.getTag())) return;
            Notification own = sbn.getNotification();
            if (own == null || !CHANNEL.equals(own.getChannelId())) return;
            String accepted = expectedTag;
            expectedTag = null;
            received = SESSION.post(accepted, getPackageName(), CHANNEL,
                    PriorityEngine.Privacy.ALLOWED) != null;
        }
    }
}
