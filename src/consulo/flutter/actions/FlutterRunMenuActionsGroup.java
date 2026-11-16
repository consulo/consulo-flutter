package consulo.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import io.flutter.actions.AttachDebuggerAction;
import io.flutter.actions.ReloadAllFlutterAppsRetarget;
import io.flutter.actions.ReloadFlutterAppRetarget;
import io.flutter.actions.RestartAllFlutterAppsRetarget;
import io.flutter.actions.RestartFlutterAppRetarget;
import io.flutter.actions.RunProfileFlutterApp;
import io.flutter.actions.RunReleaseFlutterApp;

/**
 * Upstream plugin.xml {@code <group id="Flutter.MenuActions.Run">}.
 */
@ActionImpl(id = "Flutter.MenuActions.Run", children = {
  @ActionRef(type = AnSeparator.class),
  @ActionRef(type = ReloadFlutterAppRetarget.class),
  @ActionRef(type = RestartFlutterAppRetarget.class),
  @ActionRef(type = ReloadAllFlutterAppsRetarget.class),
  @ActionRef(type = RestartAllFlutterAppsRetarget.class),
  @ActionRef(type = AnSeparator.class),
  @ActionRef(type = RunProfileFlutterApp.class),
  @ActionRef(type = RunReleaseFlutterApp.class),
  @ActionRef(type = AttachDebuggerAction.class),
  @ActionRef(type = AnSeparator.class)
}, parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_RUN), anchor = ActionRefAnchor.AFTER,
  relatedToAction = @ActionRef(id = IdeActions.ACTION_STOP_PROGRAM)))
public class FlutterRunMenuActionsGroup extends DefaultActionGroup {
}
