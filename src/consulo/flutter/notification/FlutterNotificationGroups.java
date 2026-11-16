package consulo.flutter.notification;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;
import io.flutter.FlutterMessages;

import java.util.List;
import java.util.function.Consumer;

/**
 * Notification groups of the Flutter core (upstream registers them implicitly by id via {@code new Notification(id, ...)}).
 * {@link #get(String)} replaces {@code NotificationGroupManager.getInstance().getNotificationGroup(id)} at upstream call sites.
 */
@ExtensionImpl
public class FlutterNotificationGroups implements NotificationGroupContributor {
  public static final NotificationGroup FLUTTER_MESSAGES =
    new NotificationGroup(FlutterMessages.FLUTTER_NOTIFICATION_GROUP_ID, LocalizeValue.localizeTODO("Flutter Messages"),
                          NotificationDisplayType.BALLOON, true);
  public static final NotificationGroup FLUTTER_NOTIFICATIONS =
    new NotificationGroup(FlutterMessages.FLUTTER_LOGGING_NOTIFICATION_GROUP_ID, LocalizeValue.localizeTODO("Flutter Notifications"),
                          NotificationDisplayType.BALLOON, true);

  public static final NotificationGroup FLUTTER_PACKAGES =
    new NotificationGroup("Flutter Packages", LocalizeValue.localizeTODO("Flutter Packages"), NotificationDisplayType.BALLOON, true);

  private static final List<NotificationGroup> ALL = List.of(FLUTTER_MESSAGES, FLUTTER_NOTIFICATIONS, FLUTTER_PACKAGES);

  public static NotificationGroup get(String id) {
    for (NotificationGroup group : ALL) {
      if (group.getId().equals(id)) {
        return group;
      }
    }
    throw new IllegalArgumentException("Unknown notification group: " + id);
  }

  @Override
  public void contribute(Consumer<NotificationGroup> registrator) {
    ALL.forEach(registrator);
  }
}
