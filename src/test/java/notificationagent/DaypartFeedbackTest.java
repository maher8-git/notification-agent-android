package notificationagent;

import static notificationagent.DaypartFeedback.Preference.DEPRIORITIZE;
import static notificationagent.DaypartFeedback.Preference.NEUTRAL;
import static notificationagent.DaypartFeedback.Preference.PRIORITIZE;
import static notificationagent.PriorityEngine.Category.DELIVERY;
import static notificationagent.PriorityEngine.Category.MESSAGE;
import static notificationagent.PriorityEngine.Feedback.IMPORTANT;
import static notificationagent.PriorityEngine.Feedback.LESS_IMPORTANT;

public final class DaypartFeedbackTest {
    private static int checks;
    private static void check(boolean value) {
        if (!value) throw new AssertionError("daypart assertion " + (checks + 1));
        checks++;
    }

    public static void main(String[] args) {
        DaypartFeedback m = new DaypartFeedback();
        String app = "org.example";
        check(m.preference(app, DELIVERY, 6) == NEUTRAL);
        check(m.record(app, DELIVERY, 6, IMPORTANT));
        check(m.record(app, DELIVERY, 11, IMPORTANT));
        check(m.preference(app, DELIVERY, 10) == NEUTRAL);
        check(m.record(app, DELIVERY, 9, LESS_IMPORTANT));
        check(m.preference(app, DELIVERY, 11) == PRIORITIZE);
        check(m.preference(app, DELIVERY, 5) == NEUTRAL);
        check(m.preference(app, DELIVERY, 12) == NEUTRAL);
        check(m.preference(app, MESSAGE, 9) == NEUTRAL);
        check(m.preference("org.other", DELIVERY, 9) == NEUTRAL);
        m.record(app, DELIVERY, 7, LESS_IMPORTANT);
        check(m.preference(app, DELIVERY, 9) == NEUTRAL);
        for (int i = 0; i < 8; i++) m.record(app, DELIVERY, 8, LESS_IMPORTANT);
        check(m.preference(app, DELIVERY, 10) == DEPRIORITIZE);
        check(!m.record(app, DELIVERY, 24, IMPORTANT));
        check(!m.record(app, DELIVERY, -1, IMPORTANT));
        check(!m.record(app, DELIVERY, 6, null));
        check(!m.record("org.example:", DELIVERY, 6, IMPORTANT));
        check(!m.record(app, null, 6, IMPORTANT));
        check(m.preference(app, DELIVERY, 24) == NEUTRAL);
        for (int i = 0; i < DaypartFeedback.MAX_GROUPS; i++) {
            String group = "org.group" + i;
            for (int j = 0; j < 3; j++) m.record(group, DELIVERY, 23, IMPORTANT);
        }
        check(m.preference(app, DELIVERY, 10) == NEUTRAL);
        check(m.preference("org.group0", DELIVERY, 23) == PRIORITIZE);
        check(m.preference("org.group63", DELIVERY, 23) == PRIORITIZE);
        m.reset();
        check(m.preference("org.group63", DELIVERY, 23) == NEUTRAL);
        System.out.println(checks + " vérifications des habitudes horaires réussies.");
    }
}
