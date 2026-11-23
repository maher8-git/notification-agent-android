package notificationagent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Local, optional summary of already-admitted low-priority events.
 * A trusted caller supplies one event per admission after privacy and consent checks.
 * The counter cannot read, modify, send or suppress notifications, and retains no event ID.
 */
public final class LowPrioritySummary {
    static final int MAX_GROUPS = 32;
    static final int MAX_COUNT = 255;

    public enum Daypart {
        NIGHT, MORNING, AFTERNOON, EVENING;

        private static Daypart fromHour(int hour) {
            if (hour < 0 || hour > 23) return null;
            if (hour < 6) return NIGHT;
            if (hour < 12) return MORNING;
            if (hour < 18) return AFTERNOON;
            return EVENING;
        }
    }

    private static final class Key {
        private final String app;
        private final PriorityEngine.Category category;
        private final Daypart part;

        Key(String app, PriorityEngine.Category category, Daypart part) {
            this.app = app;
            this.category = category;
            this.part = part;
        }

        @Override public boolean equals(Object obj) {
            if (!(obj instanceof Key)) return false;
            Key k = (Key) obj;
            return app.equals(k.app) && category == k.category && part == k.part;
        }

        @Override public int hashCode() { return Objects.hash(app, category, part); }
    }

    public static final class Group {
        public final String application;
        public final PriorityEngine.Category category;
        public final Daypart daypart;
        public final int count;

        private Group(Key key, int count) {
            this.application = key.app;
            this.category = key.category;
            this.daypart = key.part;
            this.count = count;
        }
    }

    private final Map<Key, Integer> counts = new LinkedHashMap<>(16, 0.75f, true);

    public synchronized boolean recordApproved(String application, PriorityEngine.Category category, int localHour,
                                                PriorityEngine.Privacy privacy, PriorityEngine.Priority priority) {
        if (privacy != PriorityEngine.Privacy.ALLOWED || priority != PriorityEngine.Priority.LOW
                || category == null || !validApp(application)) return false;
        Daypart part = Daypart.fromHour(localHour);
        if (part == null) return false;
        Key key = new Key(application, category, part);
        Integer previous = counts.get(key);
        if (previous == null && counts.size() == MAX_GROUPS) counts.remove(counts.keySet().iterator().next());
        counts.put(key, previous == null ? 1 : Math.min(MAX_COUNT, previous + 1));
        return true;
    }

    /** Snapshot in least-recently-updated order. Inspecting it does not mutate retention order. */
    public synchronized List<Group> snapshot() {
        List<Group> copy = new ArrayList<>(counts.size());
        for (Map.Entry<Key, Integer> entry : counts.entrySet()) {
            copy.add(new Group(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableList(copy);
    }

    public synchronized void clear() { counts.clear(); }

    private static boolean validApp(String app) {
        if (app == null || app.isEmpty() || app.length() > 120) return false;
        for (int i = 0; i < app.length(); i++) {
            char ch = app.charAt(i);
            if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9') || ch == '.' || ch == '_')) return false;
        }
        return true;
    }
}
