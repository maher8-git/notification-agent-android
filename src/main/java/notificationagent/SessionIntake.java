package notificationagent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Coordinates session consent and metadata routing. Does not call Android APIs. */
public final class SessionIntake {
    private static final int MAX_PAIRS = 64;
    private final IngestionBoundary boundary = new IngestionBoundary();
    private final Map<Key, PriorityEngine.Category> approved = new HashMap<>();
    private boolean connected;
    private boolean localConsent;

    private static final class Key {
        final String app;
        final String channel;
        Key(String app, String channel) { this.app = app; this.channel = channel; }
        @Override public boolean equals(Object obj) {
            if (!(obj instanceof Key)) return false;
            Key k = (Key) obj;
            return app.equals(k.app) && channel.equals(k.channel);
        }
        @Override public int hashCode() { return Objects.hash(app, channel); }
    }

    public synchronized void platformConnection(boolean connected) {
        this.connected = connected;
        if (!connected) clearConsent();
    }

    public synchronized boolean enableThisSession() {
        if (!connected) return false;
        localConsent = true;
        boundary.setOptIn(true);
        return true;
    }

    public synchronized void disableThisSession() { clearConsent(); }

    private void clearConsent() {
        localConsent = false;
        approved.clear();
        boundary.setOptIn(false);
    }

    /** Only call from a positive, explicit in-app approval for a known safe test channel. */
    public synchronized boolean approveChannel(String app, String channel, PriorityEngine.Category category) {
        if (!connected || !localConsent || app == null || channel == null || channel.isEmpty()
                || channel.length() > 120 || category == null) return false;
        Key key = new Key(app, channel);
        if (approved.containsKey(key)) return approved.get(key) == category;
        if (approved.size() >= MAX_PAIRS || !boundary.allowApp(app)) return false;
        approved.put(key, category);
        return true;
    }

    public synchronized IngestionBoundary.Suggestion post(String opaqueId, String app, String channel,
                                                           PriorityEngine.Privacy privacy) {
        if (!connected || !localConsent || app == null || channel == null) return null;
        PriorityEngine.Category category = approved.get(new Key(app, channel));
        if (category == null) return null;
        return boundary.ingest(opaqueId, app, category, privacy);
    }

    public synchronized boolean feedback(IngestionBoundary.Suggestion item, PriorityEngine.Feedback feedback) {
        return connected && localConsent && boundary.label(item, feedback);
    }
}
