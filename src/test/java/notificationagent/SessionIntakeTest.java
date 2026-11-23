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
        System.out.println(checks + " vérifications de consentement et canal réussies.");
    }
}
