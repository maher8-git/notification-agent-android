package notificationagent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Offline model only. It returns UI suggestions and never operates on notifications. */
public final class PriorityEngine {
    public enum Category { MESSAGE, REMINDER, DELIVERY, OTHER }
    public enum Privacy { ALLOWED, SENSITIVE, OTP, UNKNOWN }
    public enum Priority { HIGH, NORMAL, LOW, EXCLUDED }
    public enum Feedback { IMPORTANT(2), LESS_IMPORTANT(-2);
        final int step;
        Feedback(int step) { this.step = step; }
    }

    private static final int MAX_GROUPS = 128;
    private static final int MAX_RECENT_IDS = 256;
    private static final Pattern APP = Pattern.compile("[a-zA-Z0-9_.]{1,120}");
    private static final Pattern ID = Pattern.compile("[a-zA-Z0-9_-]{1,64}");
    private final Map<String, Integer> scores = new LinkedHashMap<>();
    // An event can be labeled once while its opaque event ID is retained.
    private final Map<String, Pending> recent = new LinkedHashMap<>();

    private static final class Pending {
        final String group;
        boolean labeled;
        boolean summarized;
        Pending(String group) { this.group = group; }
    }

    /**
     * The local adapter must explicitly justify ALLOWED. Unclassified inputs never enter
     * the model. No subject, title, message, sender, or Android action is accepted.
     */
    public synchronized Priority suggest(String id, String app, Category category, Privacy privacy) {
        if (privacy != Privacy.ALLOWED) {
            if (id != null) recent.remove(id);
            return Priority.EXCLUDED;
        }
        if (category == null || id == null || app == null
                || !ID.matcher(id).matches() || !APP.matcher(app).matches()) return Priority.EXCLUDED;
        final String group = app + ":" + category.name();
        Pending previous = recent.get(id);
        if (previous != null && !previous.group.equals(group)) return Priority.EXCLUDED;
        if (previous == null) {
            evictAtLimit(recent, MAX_RECENT_IDS);
            recent.put(id, new Pending(group));
        }
        Integer score = scores.get(group);
        return score == null ? Priority.NORMAL : score >= 3 ? Priority.HIGH
                : score <= -3 ? Priority.LOW : Priority.NORMAL;
    }

    synchronized Object pendingToken(String id) {
        return recent.get(id);
    }

    synchronized boolean markSummarizedCurrent(String id, Object token) {
        Pending pending = recent.get(id);
        if (pending == null || pending != token || pending.summarized) return false;
        pending.summarized = true;
        return true;
    }

    synchronized boolean labelCurrent(String id, Object token, Feedback feedback) {
        return token != null && recent.get(id) == token && label(id, feedback);
    }

    /** Explicit in-app feedback only. Never infer engagement from open/clear events. */
    public synchronized boolean label(String id, Feedback feedback) {
        Pending pending = recent.get(id);
        if (pending == null || pending.labeled || feedback == null) return false;
        pending.labeled = true;
        Integer prev = scores.remove(pending.group);
        evictAtLimit(scores, MAX_GROUPS);
        scores.put(pending.group, Math.max(-6, Math.min(6, (prev == null ? 0 : prev) + feedback.step)));
        return true;
    }

    public synchronized void reset() {
        scores.clear();
        recent.clear();
    }

    private static <T> void evictAtLimit(Map<String,T> map, int limit) {
        if (map.size() >= limit) map.remove(map.keySet().iterator().next());
    }
}
