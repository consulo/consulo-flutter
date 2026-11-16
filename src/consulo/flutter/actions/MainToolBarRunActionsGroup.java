package consulo.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import io.flutter.actions.AttachDebuggerAction;
import io.flutter.actions.ReloadFlutterAppRetarget;

/**
 * Upstream plugin.xml {@code <group id="MainToolBarRunActionsGroup">}.
 * <p>
 * Upstream adds the group to the new UI {@code MainToolbarRight} before {@code RedesignedRunConfigurationSelector}, and adds its
 * actions one by one to the old UI {@code ToolbarRunGroup} after {@code RunnerActions}. Consulo has only {@code ToolbarRunGroup},
 * so the group goes there and its actions have no parents of their own (they would show twice).
 */
@ActionImpl(id = "MainToolBarRunActionsGroup", children = {
  @ActionRef(type = AttachDebuggerAction.class),
  @ActionRef(type = ReloadFlutterAppRetarget.class)
}, parents = @ActionParentRef(value = @ActionRef(id = "ToolbarRunGroup"), anchor = ActionRefAnchor.AFTER,
  relatedToAction = @ActionRef(id = IdeActions.GROUP_RUNNER_ACTIONS)))
public class MainToolBarRunActionsGroup extends DefaultActionGroup {
}
