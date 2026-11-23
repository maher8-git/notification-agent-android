package notificationagent;

import static notificationagent.PriorityEngine.Category.DELIVERY;
import static notificationagent.PriorityEngine.Feedback.IMPORTANT;
import static notificationagent.PriorityEngine.Privacy.ALLOWED;
import static notificationagent.PriorityEngine.Privacy.OTP;
import static notificationagent.PriorityEngine.Privacy.UNKNOWN;

public final class SessionIntakeTest {
    private static int checks;
    private static void check(boolean b) { if (!b) throw new AssertionError(checks); checks++; }
    public static void main(String[] args) {
        SessionIntake x = new SessionIntake();
        check(!x.enableThisSession());
        x.platformConnection(true);
        check(!x.approveChannel("org.example.test", "dispatch", DELIVERY));
        check(x.enableThisSession());
        check(x.post("one", "org.example.test", "dispatch", ALLOWED)==null);
        check(x.approveChannel("org.example.test", "dispatch", DELIVERY));
        check(x.post("one", "org.example.test", "other", ALLOWED)==null);
        check(x.post("one", "org.example.other", "dispatch", ALLOWED)==null);
        check(x.post("one", "org.example.test", "dispatch", UNKNOWN)==null);
        check(x.post("one", "org.example.test", "dispatch", OTP)==null);
        IngestionBoundary.Suggestion item=x.post("one", "org.example.test", "dispatch", ALLOWED);
        check(item!=null);
        check(x.feedback(item,IMPORTANT));
        x.platformConnection(false);
        x.platformConnection(true);
        check(!x.feedback(item,IMPORTANT));
        check(x.post("two", "org.example.test", "dispatch", ALLOWED)==null);
        check(x.enableThisSession());
        check(x.post("two", "org.example.test", "dispatch", ALLOWED)==null);
        check(x.approveChannel("org.example.test", "dispatch", DELIVERY));
        x.disableThisSession();
        check(x.post("three", "org.example.test", "dispatch", ALLOWED)==null);
        timedFeedback();
        System.out.println(checks + " vérifications de consentement et canal réussies.");
    }

    private static void timedFeedback() {
        String app = "org.example.test", channel = "dispatch";
        SessionIntake x = new SessionIntake();
        check(x.postAtLocalHour("blocked", app, channel, ALLOWED, 7) == null);
        x.platformConnection(true);
        check(x.enableThisSession());
        check(x.approveChannel(app, channel, DELIVERY));
        for (int i = 0; i < 3; i++) {
            SessionIntake.TimedSuggestion item = x.postAtLocalHour("morning" + i, app, channel, ALLOWED, 7);
            check(item.reason == SessionIntake.Reason.GENERAL);
            check(x.feedbackAtLocalHour(item, IMPORTANT));
            check(!x.feedbackAtLocalHour(item, IMPORTANT));
        }
        SessionIntake.TimedSuggestion morning = x.postAtLocalHour("am", app, channel, ALLOWED, 11);
        check(morning.reason == SessionIntake.Reason.LOCAL_DAYPART);
        check(morning.priority == PriorityEngine.Priority.HIGH);
        check(x.postAtLocalHour("pm", app, channel, ALLOWED, 19).reason == SessionIntake.Reason.GENERAL);
        for (int i = 0; i < 3; i++) {
            SessionIntake.TimedSuggestion item = x.postAtLocalHour("evening" + i, app, channel, ALLOWED, 20);
            check(x.feedbackAtLocalHour(item, PriorityEngine.Feedback.LESS_IMPORTANT));
        }
        SessionIntake.TimedSuggestion evening = x.postAtLocalHour("evening-check", app, channel, ALLOWED, 18);
        check(evening.reason == SessionIntake.Reason.LOCAL_DAYPART);
        check(evening.priority == PriorityEngine.Priority.LOW);
        check(x.postAtLocalHour("am-check", app, channel, ALLOWED, 6).priority == PriorityEngine.Priority.HIGH);
        check(x.postAtLocalHour("bad-hour", app, channel, ALLOWED, 24).reason == SessionIntake.Reason.GENERAL);

        SessionIntake.TimedSuggestion old = x.postAtLocalHour("change", app, channel, ALLOWED, 6);
        check(x.postAtLocalHour("change", app, channel, OTP, 6) == null);
        check(!x.feedbackAtLocalHour(old, IMPORTANT));
        SessionIntake.TimedSuggestion replacement = x.postAtLocalHour("change", app, channel, ALLOWED, 19);
        check(!x.feedbackAtLocalHour(old, IMPORTANT));
        check(x.feedbackAtLocalHour(replacement, IMPORTANT));
        check(x.postAtLocalHour("other-channel", app, "unapproved", ALLOWED, 19) == null);

        x.disableThisSession();
        check(!x.feedbackAtLocalHour(morning, IMPORTANT));
        check(x.enableThisSession());
        check(x.approveChannel(app, channel, DELIVERY));
        check(x.postAtLocalHour("after-disable", app, channel, ALLOWED, 7).reason == SessionIntake.Reason.GENERAL);
        SessionIntake.TimedSuggestion pending = x.postAtLocalHour("disconnect", app, channel, ALLOWED, 10);
        x.platformConnection(false);
        x.platformConnection(true);
        check(!x.feedbackAtLocalHour(pending, IMPORTANT));
        check(x.enableThisSession());
        check(x.approveChannel(app, channel, DELIVERY));
        check(x.postAtLocalHour("after-disconnect", app, channel, ALLOWED, 19).reason == SessionIntake.Reason.GENERAL);
    }
}
