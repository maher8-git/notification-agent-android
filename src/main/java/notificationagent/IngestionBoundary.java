package notificationagent;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** JVM-only admission boundary; Android lifecycle and classification are not implemented. */
public final class IngestionBoundary {
    private static final int MAX_APPS = 128;
    private static final Pattern APP = Pattern.compile("[a-zA-Z0-9_.]{1,120}");
    private static final Pattern ID = Pattern.compile("[a-zA-Z0-9_-]{1,64}");
    private final PriorityEngine engine = new PriorityEngine();
    private final Set<String> allowed = new HashSet<>();
    private boolean optedIn;
    private Object lifetime = new Object();

    /** Only created by this boundary after an allowed classification. No notification content. */
    public static final class Suggestion {
        public final PriorityEngine.Priority priority;
        private final Object lifetime;
        private final String id;
        private final Object pending;

        private Suggestion(PriorityEngine.Priority priority, Object lifetime, String id, Object pending) {
            this.priority = priority;
            this.lifetime = lifetime;
            this.id = id;
            this.pending = pending;
        }
    }

    public synchronized void setOptIn(boolean enabled) {
        if (!enabled) {
            allowed.clear();
            clearHistory();
        }
        optedIn = enabled;
    }

    public synchronized boolean allowApp(String app) {
        return optedIn && app != null && APP.matcher(app).matches()
                && (allowed.contains(app) || (allowed.size() < MAX_APPS && allowed.add(app)));
    }

    public synchronized void removeApp(String app) {
        if (allowed.remove(app)) clearHistory();
    }

    private void clearHistory() {
        engine.reset();
        lifetime = new Object();
    }

    /** Called only by a trusted adapter with an opaque local ID and an explicit privacy decision. */
    public synchronized Suggestion ingest(String id, String app, PriorityEngine.Category category,
                                          PriorityEngine.Privacy privacy) {
        if (!optedIn || app == null || !allowed.contains(app) || id == null
                || !ID.matcher(id).matches()) return null;
        String scoped = app + ":" + id;
        // Keep IDs within PriorityEngine's existing opaque identifier contract.
        String opaque = java.util.UUID.nameUUIDFromBytes(
                scoped.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        PriorityEngine.Priority priority = engine.suggest(opaque, app, category, privacy);
        return priority == PriorityEngine.Priority.EXCLUDED ? null
                : new Suggestion(priority, lifetime, opaque, engine.pendingToken(opaque));
    }

    synchronized boolean markSummarized(Suggestion suggestion) {
        return optedIn && suggestion != null && suggestion.lifetime == lifetime
                && engine.markSummarizedCurrent(suggestion.id, suggestion.pending);
    }

    /** Call only in response to an explicit click in the app's own UI. */
    public synchronized boolean label(Suggestion suggestion, PriorityEngine.Feedback choice) {
        return optedIn && suggestion != null && suggestion.lifetime == lifetime
                && engine.labelCurrent(suggestion.id, suggestion.pending, choice);
    }
}
