package notificationagent;

import static notificationagent.PriorityEngine.Category.DELIVERY;
import static notificationagent.PriorityEngine.Category.REMINDER;
import static notificationagent.PriorityEngine.Priority.HIGH;
import static notificationagent.PriorityEngine.Priority.LOW;
import static notificationagent.PriorityEngine.Privacy.ALLOWED;
import static notificationagent.PriorityEngine.Privacy.OTP;
import static notificationagent.PriorityEngine.Privacy.UNKNOWN;

public final class LowPrioritySummaryTest {
    private static int checks;
    private static void check(boolean value) {
        if (!value) throw new AssertionError("summary assertion " + (checks + 1));
        checks++;
    }

    public static void main(String[] args) {
        LowPrioritySummary model = new LowPrioritySummary();
        check(!model.recordApproved("org.one", DELIVERY, 6, OTP, LOW));
        check(!model.recordApproved("org.one", DELIVERY, 6, UNKNOWN, LOW));
        check(!model.recordApproved("org.one", DELIVERY, 6, ALLOWED, HIGH));
        check(!model.recordApproved("org.one", DELIVERY, -1, ALLOWED, LOW));
        check(!model.recordApproved("org.one", DELIVERY, 24, ALLOWED, LOW));
        check(!model.recordApproved("org:one", DELIVERY, 6, ALLOWED, LOW));
        check(!model.recordApproved("org.one", null, 6, ALLOWED, LOW));
        check(model.snapshot().isEmpty());

        check(model.recordApproved("org.one", DELIVERY, 6, ALLOWED, LOW));
        check(model.recordApproved("org.one", DELIVERY, 11, ALLOWED, LOW));
        check(model.recordApproved("org.one", DELIVERY, 12, ALLOWED, LOW));
        check(model.recordApproved("org.one", REMINDER, 6, ALLOWED, LOW));
        check(model.recordApproved("org.two", DELIVERY, 6, ALLOWED, LOW));
        java.util.List<LowPrioritySummary.Group> original = model.snapshot();
        check(original.size() == 4);
        check(original.get(0).count == 2 && original.get(0).daypart == LowPrioritySummary.Daypart.MORNING);
        check(original.get(1).count == 1 && original.get(1).daypart == LowPrioritySummary.Daypart.AFTERNOON);
        check(original.get(2).category == REMINDER);
        check("org.two".equals(original.get(3).application));
        try {
            original.clear();
            throw new AssertionError("mutable snapshot");
        } catch (UnsupportedOperationException expected) {
            check(true);
        }

        for (int i = 0; i < 300; i++) model.recordApproved("org.one", DELIVERY, 6, ALLOWED, LOW);
        check(model.snapshot().get(3).count == LowPrioritySummary.MAX_COUNT);
        check(original.get(0).count == 2);
        for (int i = 0; i < LowPrioritySummary.MAX_GROUPS; i++) {
            model.recordApproved("org.synthetic" + i, DELIVERY, 23, ALLOWED, LOW);
        }
        check(model.snapshot().size() == LowPrioritySummary.MAX_GROUPS);
        check("org.synthetic0".equals(model.snapshot().get(0).application));
        check(model.snapshot().get(0).daypart == LowPrioritySummary.Daypart.EVENING);
        model.clear();
        check(model.snapshot().isEmpty());
        System.out.println(checks + " vérifications de la synthèse locale réussies.");
    }
}
