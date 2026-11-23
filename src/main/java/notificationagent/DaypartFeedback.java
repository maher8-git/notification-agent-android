package notificationagent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Optional in-memory learning from admitted, deduplicated, explicit UI feedback only. */
public final class DaypartFeedback {
    static final int MAX_GROUPS = 64;
    private static final int HISTORY = 8;
    private static final int MASK = (1 << HISTORY) - 1;
    private static final int MINIMUM = 3;

    private final Map<String, Window[]> groups = new LinkedHashMap<>(16, 0.75f, true);

    public enum Preference { NEUTRAL, PRIORITIZE, DEPRIORITIZE }

    private static final class Window {
        private int bits;
        private int count;

        void add(boolean important) {
            bits = ((bits << 1) | (important ? 1 : 0)) & MASK;
            count = Math.min(count + 1, HISTORY);
        }

        Preference preference() {
            if (count < MINIMUM) return Preference.NEUTRAL;
            int important = Integer.bitCount(bits);
            int threshold = (2 * count + 2) / 3;
            if (important >= threshold) return Preference.PRIORITIZE;
            if (count - important >= threshold) return Preference.DEPRIORITIZE;
            return Preference.NEUTRAL;
        }
    }

    private static int bucket(int hour) {
        if (hour < 0 || hour > 23) return -1;
        if (hour < 6) return 0;
        if (hour < 12) return 1;
        if (hour < 18) return 2;
        return 3;
    }

    private static String key(String app, PriorityEngine.Category category) {
        if (app == null || app.isEmpty() || app.length() > 120 || category == null) return null;
        for (int i = 0; i < app.length(); i++) {
            char c = app.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                  (c >= '0' && c <= '9') || c == '.' || c == '_')) return null;
        }
        return app + ":" + category.name();
    }

    /** Record once after the caller has admitted the event and accepted its explicit label. */
    public synchronized boolean record(String app, PriorityEngine.Category category, int localHour,
                                       PriorityEngine.Feedback choice) {
        int slot = bucket(localHour);
        String group = key(app, category);
        if (slot < 0 || group == null || choice == null) return false;
        Window[] history = groups.get(group);
        if (history == null) {
            if (groups.size() == MAX_GROUPS) groups.remove(groups.keySet().iterator().next());
            history = new Window[4];
            groups.put(group, history);
        }
        if (history[slot] == null) history[slot] = new Window();
        history[slot].add(choice == PriorityEngine.Feedback.IMPORTANT);
        return true;
    }

    /** NEUTRAL directs the caller to keep its original app/category recommendation. */
    public synchronized Preference preference(String app, PriorityEngine.Category category, int localHour) {
        int slot = bucket(localHour);
        String group = key(app, category);
        if (slot < 0 || group == null) return Preference.NEUTRAL;
        Window[] history = groups.get(group);
        return history == null || history[slot] == null ? Preference.NEUTRAL : history[slot].preference();
    }

    public synchronized void reset() { groups.clear(); }
}
