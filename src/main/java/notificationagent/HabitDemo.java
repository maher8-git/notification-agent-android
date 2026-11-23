package notificationagent;

import static notificationagent.PriorityEngine.Category.DELIVERY;
import static notificationagent.PriorityEngine.Feedback.IMPORTANT;
import static notificationagent.PriorityEngine.Feedback.LESS_IMPORTANT;
import static notificationagent.PriorityEngine.Privacy.ALLOWED;
import static notificationagent.PriorityEngine.Privacy.OTP;

/** Deterministic, standalone demo. Its inputs are fixed fictional metadata. */
public final class HabitDemo {
    private static final String APP = "org.example.fiction";
    private static final String CHANNEL = "avancement-fictif";

    private HabitDemo() {}

    public static void main(String[] args) {
        System.out.print(run());
    }

    static String run() {
        SessionIntake session = new SessionIntake();
        session.platformConnection(true);
        check(session.enableThisSession());
        check(session.approveChannel(APP, CHANNEL, DELIVERY));
        StringBuilder out = new StringBuilder("Démo locale : notifications fictives uniquement.\n");
        line(out, "08 h, avant les retours", session.postAtLocalHour("avant", APP, CHANNEL, ALLOWED, 8));

        out.append("Retours explicites : 3 « Important » le matin, 3 « Moins important » le soir.\n");
        for (int i = 0; i < 3; i++) {
            check(session.feedbackAtLocalHour(
                    session.postAtLocalHour("matin-" + i, APP, CHANNEL, ALLOWED, 8), IMPORTANT));
            check(session.feedbackAtLocalHour(
                    session.postAtLocalHour("soir-" + i, APP, CHANNEL, ALLOWED, 20), LESS_IMPORTANT));
        }
        line(out, "08 h, après les retours", session.postAtLocalHour("bilan-matin", APP, CHANNEL, ALLOWED, 8));
        line(out, "20 h, après les retours", session.postAtLocalHour("bilan-soir", APP, CHANNEL, ALLOWED, 20));
        line(out, "14 h, sans retours", session.postAtLocalHour("bilan-jour", APP, CHANNEL, ALLOWED, 14));
        line(out, "Notification sensible", session.postAtLocalHour("sensible", APP, CHANNEL, OTP, 8));
        session.postAtLocalHour("bilan-soir", APP, CHANNEL, ALLOWED, 21); // Same admitted event.
        session.postAtLocalHour("deuxieme-soir", APP, CHANNEL, ALLOWED, 22);
        summary(out, "Résumé du soir (dont un doublon ignoré)", session);
        session.disableThisSession();
        line(out, "Après révocation du consentement local", session.postAtLocalHour("apres", APP, CHANNEL, ALLOWED, 8));
        summary(out, "Résumé après révocation", session);
        return out.toString();
    }

    private static void summary(StringBuilder out, String text, SessionIntake session) {
        int count = 0;
        for (LowPrioritySummary.Group group : session.lowPrioritySnapshot()) {
            if (group.daypart == LowPrioritySummary.Daypart.EVENING) count += group.count;
        }
        out.append(text).append(" : ").append(count).append(" alerte");
        if (count != 1) out.append('s');
        out.append('\n');
    }

    private static void check(boolean ok) {
        if (!ok) throw new IllegalStateException("La démo ne peut pas démarrer ou apprendre.");
    }

    private static void line(StringBuilder out, String text, SessionIntake.TimedSuggestion item) {
        out.append(text).append(" : ");
        if (item == null) {
            out.append("ignorée\n");
            return;
        }
        switch (item.priority) {
            case HIGH: out.append("prioritaire"); break;
            case LOW: out.append("moins prioritaire"); break;
            default: out.append("standard");
        }
        out.append(item.reason == SessionIntake.Reason.LOCAL_DAYPART
                ? " (habitudes de cette tranche)\n" : " (règle générale)\n");
    }
}
