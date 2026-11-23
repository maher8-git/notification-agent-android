package notificationagent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Coordinates session consent and metadata routing. Does not call Android APIs. */
public final class SessionIntake {
    private static final int MAX_PAIRS = 64;
    private final IngestionBoundary boundary = new IngestionBoundary();
    private final DaypartFeedback dayparts = new DaypartFeedback();
    private final LowPrioritySummary lowPriority = new LowPrioritySummary();
    private final Map<Key, PriorityEngine.Category> approved = new HashMap<>();
    private boolean connected;
    private boolean localConsent;

    public enum Reason { GENERAL, LOCAL_DAYPART }

    /** Only minted for an admitted notification; contains no notification content. */
    public static final class TimedSuggestion {
        public final PriorityEngine.Priority priority;
        public final Reason reason;
        private final IngestionBoundary.Suggestion handle;
        private final String app;
        private final PriorityEngine.Category category;
        private final int hour;

        private TimedSuggestion(IngestionBoundary.Suggestion handle, String app,
                                PriorityEngine.Category category, int hour,
                                DaypartFeedback.Preference preference) {
            this.handle = handle;
            this.app = app;
            this.category = category;
            this.hour = hour >= 0 && hour < 24 ? hour : -1;
            reason = preference == DaypartFeedback.Preference.NEUTRAL ? Reason.GENERAL : Reason.LOCAL_DAYPART;
            priority = reason == Reason.GENERAL ? handle.priority
                    : preference == DaypartFeedback.Preference.PRIORITIZE ? PriorityEngine.Priority.HIGH
                    : PriorityEngine.Priority.LOW;
        }
    }

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
        dayparts.reset();
        lowPriority.clear();
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

    /** The trusted adapter supplies the notification's local arrival hour (not label time). */
    public synchronized TimedSuggestion postAtLocalHour(String opaqueId, String app, String channel,
                                                         PriorityEngine.Privacy privacy, int localHour) {
        IngestionBoundary.Suggestion base = post(opaqueId, app, channel, privacy);
        if (base == null) return null;
        PriorityEngine.Category category = approved.get(new Key(app, channel));
        TimedSuggestion suggestion = new TimedSuggestion(
                base, app, category, localHour, dayparts.preference(app, category, localHour));
        if (suggestion.hour != -1 && suggestion.priority == PriorityEngine.Priority.LOW
                && boundary.markSummarized(base)) {
            lowPriority.recordApproved(app, category, suggestion.hour, privacy, suggestion.priority);
        }
        return suggestion;
    }

    /** Read-only aggregate as of now; callers should not cache it beyond this session. */
    public synchronized java.util.List<LowPrioritySummary.Group> lowPrioritySnapshot() {
        return lowPriority.snapshot();
    }

    /** Daypart learning happens only after the existing admission handle accepts the UI label. */
    public synchronized boolean feedbackAtLocalHour(TimedSuggestion item, PriorityEngine.Feedback choice) {
        if (item == null || !feedback(item.handle, choice)) return false;
        if (item.hour != -1) dayparts.record(item.app, item.category, item.hour, choice);
        return true;
    }

    public synchronized boolean feedback(IngestionBoundary.Suggestion item, PriorityEngine.Feedback feedback) {
        return connected && localConsent && boundary.label(item, feedback);
    }
}
