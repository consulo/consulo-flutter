/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.flutter.notification;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;
import io.flutter.FlutterMessages;

import java.util.function.Consumer;

/**
 * Notification groups used by the Flutter SDK settings ({@code io.flutter.sdk}).
 */
@ExtensionImpl
public class FlutterSdkNotificationGroups implements NotificationGroupContributor {
  public static final NotificationGroup FLUTTER_LOGGING =
    new NotificationGroup(FlutterMessages.FLUTTER_LOGGING_NOTIFICATION_GROUP_ID, LocalizeValue.localizeTODO("Flutter Notifications"),
                          NotificationDisplayType.BALLOON, true);

  @Override
  public void contribute(Consumer<NotificationGroup> registrator) {
    registrator.accept(FLUTTER_LOGGING);
  }
}
