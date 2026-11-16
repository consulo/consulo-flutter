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

import com.jetbrains.lang.dart.ide.errorTreeView.DartProblemsView;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;
import consulo.project.ui.wm.ToolWindowId;
import io.flutter.FlutterMessages;

import java.util.List;
import java.util.function.Consumer;

/**
 * Notification groups of the run area of upstream plugin.xml ({@code flutter-run}, {@code flutter-debug}, {@code flutter-analysis}).
 * {@link #get(String)} replaces {@code NotificationGroupManager.getInstance().getNotificationGroup(id)} at upstream call sites.
 */
@ExtensionImpl
public class FlutterRunNotificationGroups implements NotificationGroupContributor {
  public static final NotificationGroup FLUTTER_RUN =
    NotificationGroup.toolWindowGroup("flutter-run", LocalizeValue.localizeTODO("Flutter Run"), ToolWindowId.RUN);
  public static final NotificationGroup FLUTTER_DEBUG =
    NotificationGroup.toolWindowGroup("flutter-debug", LocalizeValue.localizeTODO("Flutter Debug"), ToolWindowId.DEBUG);
  public static final NotificationGroup FLUTTER_ANALYSIS =
    NotificationGroup.toolWindowGroup("flutter-analysis", LocalizeValue.localizeTODO("Flutter Analysis"), DartProblemsView.TOOLWINDOW_ID);

  /**
   * The "Flutter Messages" group ({@link FlutterMessages#FLUTTER_NOTIFICATION_GROUP_ID}), registered by {@link FlutterNotificationGroups}.
   */
  public static final NotificationGroup FLUTTER_MESSAGES = FlutterNotificationGroups.FLUTTER_MESSAGES;

  private static final List<NotificationGroup> ALL = List.of(FLUTTER_RUN, FLUTTER_DEBUG, FLUTTER_ANALYSIS);

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
