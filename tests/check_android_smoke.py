"""Source and manifest smoke assertions, not an Android build or runtime test."""
from pathlib import Path
import re
from xml.etree import ElementTree

root = Path(__file__).resolve().parents[1]
android = root / "app/src/main"
manifest = ElementTree.parse(android / "AndroidManifest.xml")
ns = "{http://schemas.android.com/apk/res/android}"
services = manifest.findall("application/service")
assert len(services) == 1
service = services[0]
assert service.get(ns + "name") == ".SmokeListener"
assert service.get(ns + "exported") == "false"
assert service.get(ns + "permission") == "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
assert service.find("intent-filter/action").get(ns + "name") == "android.service.notification.NotificationListenerService"
assert [perm.get(ns + "name") for perm in manifest.findall("uses-permission")] == [
    "android.permission.POST_NOTIFICATIONS"
]
listener = (android / "java/notificationagent/android/SmokeListener.java").read_text()
assert not re.search(r"\b(extras|getActiveNotifications|cancelNotification|cancelAllNotifications|getActions|sendBroadcast|getSharedPreferences)\b", listener)
assert listener.index("sbn.getPackageName()") < listener.index("sbn.getNotification()")
assert listener.index("sbn.getTag()") < listener.index("sbn.getNotification()")
assert "CHANNEL.equals(own.getChannelId())" in listener
assert "onListenerDisconnected() { connection(false); }" in listener
print("Android smoke: manifest and source guard checks passed (not a build).")
