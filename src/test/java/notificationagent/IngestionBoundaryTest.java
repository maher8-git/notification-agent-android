package notificationagent;

import static notificationagent.PriorityEngine.Category.*;
import static notificationagent.PriorityEngine.Feedback.*;
import static notificationagent.PriorityEngine.Privacy.*;
import static notificationagent.PriorityEngine.Priority.*;

public final class IngestionBoundaryTest {
    private static int checks;
    private static void check(boolean value) {
        if (!value) throw new AssertionError("check #" + (checks + 1));
        checks++;
    }

    public static void main(String[] args) {
        IngestionBoundary gate = new IngestionBoundary();
        String a = "demo.first", b = "demo.second";
        check(!gate.allowApp(a));
        check(gate.ingest("1",a,MESSAGE,ALLOWED) == null);
        gate.setOptIn(true);
        check(gate.ingest("1",a,MESSAGE,ALLOWED) == null);
        check(gate.allowApp(a));
        check(gate.ingest("1",b,MESSAGE,ALLOWED) == null);
        check(gate.ingest("1",a,MESSAGE,OTP) == null);
        check(gate.ingest("1",a,MESSAGE,UNKNOWN) == null);
        check(gate.ingest("1",a,MESSAGE,SENSITIVE) == null);
        check(gate.ingest("1",a,MESSAGE,null) == null);
        check(gate.ingest("bad id",a,MESSAGE,ALLOWED) == null);
        IngestionBoundary.Suggestion first = gate.ingest("1",a,MESSAGE,ALLOWED);
        check(first.priority == NORMAL && gate.label(first,IMPORTANT));
        check(!gate.label(first,IMPORTANT));
        check(gate.label(gate.ingest("2",a,MESSAGE,ALLOWED),IMPORTANT));
        check(gate.ingest("3",a,MESSAGE,ALLOWED).priority == HIGH);
        check(gate.allowApp(b) && gate.ingest("3",b,MESSAGE,ALLOWED).priority == NORMAL);
        IngestionBoundary.Suggestion second = gate.ingest("4",a,MESSAGE,ALLOWED);
        check(gate.ingest("4",a,MESSAGE,OTP) == null && !gate.label(second,IMPORTANT));
        gate.removeApp(a);
        check(gate.ingest("3",a,MESSAGE,ALLOWED) == null && !gate.label(first,IMPORTANT));
        gate.setOptIn(false);
        check(gate.ingest("4",b,MESSAGE,ALLOWED) == null);
        gate.setOptIn(true);
        check(gate.ingest("4",b,MESSAGE,ALLOWED) == null);
        check(!new IngestionBoundary().label(first, IMPORTANT));
        System.out.println(checks + " vérifications de la frontière d’admission réussies.");
    }
}
