package notificationagent.android;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private TextView status;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        final LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        final int pad = (int) (20 * getResources().getDisplayMetrics().density);
        controls.setPadding(pad,pad,pad,pad);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(controls);
        setContentView(scroll);
        TextView about = new TextView(this);
        about.setText("Essai local seulement. Android accorde un accès général, mais cet essai ignore toutes les autres applications et ne consulte aucun contenu. Aucun envoi réseau.");
        controls.addView(about);
        button(controls, "Ouvrir les réglages d’accès Android", () -> {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (ActivityNotFoundException error) {
                status.setText("Réglages d’accès introuvables sur cet appareil.");
            }
        });
        button(controls, "Activer le test pour cette session", () -> {
            SmokeListener.enable(this);
            refresh();
        });
        button(controls, "Afficher une notification fictive", this::postSynthetic);
        button(controls, "Actualiser l’état", this::refresh);
        button(controls, "Arrêter et effacer le test local", () -> {
            SmokeListener.disable();
            refresh();
        });
        status = new TextView(this);
        status.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
        controls.addView(status);
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void button(LinearLayout controls, String title, Runnable action) {
        Button b = new Button(this);
        b.setText(title);
        b.setOnClickListener(view -> action.run());
        controls.addView(b);
    }

    private void refresh() { status.setText(SmokeListener.status(this)); }

    private void postSynthetic() {
        if (!SmokeListener.canPost(this)) { refresh(); return; }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            status.setText("Autorise l’affichage, puis retouche le bouton. Aucun envoi automatique.");
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (!manager.areNotificationsEnabled()) {
            status.setText("L’affichage des notifications de test est bloqué dans Android.");
            return;
        }
        manager.createNotificationChannel(new NotificationChannel(SmokeListener.CHANNEL,
                "Essai fictif uniquement", NotificationManager.IMPORTANCE_LOW));
        Notification notification = new Notification.Builder(this, SmokeListener.CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Notification fictive")
                .setContentText("Essai local déclenché manuellement.")
                .build();
        try {
            SmokeListener.post(this, notification);
            refresh();
        } catch (SecurityException error) {
            status.setText("Android a refusé la publication du test.");
        }
    }
}
