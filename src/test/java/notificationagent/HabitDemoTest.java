package notificationagent;

public final class HabitDemoTest {
    public static void main(String[] args) {
        String[] lines = HabitDemo.run().split("\n");
        String[] expected = {
            "Démo locale : notifications fictives uniquement.",
            "08 h, avant les retours : standard (règle générale)",
            "Retours explicites : 3 « Important » le matin, 3 « Moins important » le soir.",
            "08 h, après les retours : prioritaire (habitudes de cette tranche)",
            "20 h, après les retours : moins prioritaire (habitudes de cette tranche)",
            "14 h, sans retours : standard (règle générale)",
            "Notification sensible : ignorée",
            "Après révocation du consentement local : ignorée"
        };
        if (lines.length != expected.length) throw new AssertionError("unexpected number of demo lines");
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equals(lines[i])) throw new AssertionError("demo output line " + (i + 1));
        }
        System.out.println(expected.length + " lignes de démonstration vérifiées.");
    }
}
