package notificationagent;

import static notificationagent.PriorityEngine.Category.*;
import static notificationagent.PriorityEngine.Feedback.*;
import static notificationagent.PriorityEngine.Privacy.*;
import static notificationagent.PriorityEngine.Priority.*;

public final class PriorityEngineTest {
    private static int checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check #" + checks);
    }
    public static void main(String[] args) {
        PriorityEngine model = new PriorityEngine();
        String a = "demo.mail", b = "demo.calendar";
        check(model.suggest("mail1",a,MESSAGE,ALLOWED) == NORMAL);
        check(model.label("mail1", IMPORTANT));
        check(!model.label("mail1", IMPORTANT));
        check(model.suggest("mail2",a,MESSAGE,ALLOWED) == NORMAL);
        check(model.label("mail2", IMPORTANT));
        check(model.suggest("mail3",a,MESSAGE,ALLOWED) == HIGH);
        check(model.suggest("cal1",b,MESSAGE,ALLOWED) == NORMAL);
        check(model.suggest("mailReminder",a,REMINDER,ALLOWED) == NORMAL);
        check(model.suggest("otp1",a,MESSAGE,OTP) == EXCLUDED);
        check(model.suggest("secret1",a,MESSAGE,SENSITIVE) == EXCLUDED);
        check(model.suggest("unclear1",a,MESSAGE,UNKNOWN) == EXCLUDED);
        check(!model.label("otp1", IMPORTANT));
        check(!model.label("secret1", IMPORTANT));
        check(!model.label("unclear1", IMPORTANT));
        check(model.suggest("mail1",b,MESSAGE,ALLOWED) == EXCLUDED);
        check(model.label("cal1", LESS_IMPORTANT));
        check(model.suggest("cal2",b,MESSAGE,ALLOWED) == NORMAL);
        check(model.label("cal2", LESS_IMPORTANT));
        check(model.suggest("cal3",b,MESSAGE,ALLOWED) == LOW);
        check(model.label("mail3", LESS_IMPORTANT));
        check(model.suggest("mail4",a,MESSAGE,ALLOWED) == NORMAL);
        model.reset();
        check(!model.label("mail4", IMPORTANT));
        check(model.suggest("mail5",a,MESSAGE,ALLOWED) == NORMAL);
        check(model.suggest("cal4",b,MESSAGE,ALLOWED) == NORMAL);
        check(model.suggest("bad id",a,OTHER,ALLOWED) == EXCLUDED);
        check(model.suggest("reclassified",a,DELIVERY,ALLOWED) == NORMAL);
        check(model.suggest("reclassified",a,DELIVERY,SENSITIVE) == EXCLUDED);
        check(!model.label("reclassified", IMPORTANT));
        System.out.println(checks + " vérifications synthétiques réussies.");
    }
}
