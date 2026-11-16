package consulo.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.ui.ex.action.DefaultActionGroup;
import io.flutter.actions.DeviceSelectorAction;
import io.flutter.actions.DeviceSelectorRefresherAction;

/**
 * Upstream plugin.xml {@code <group id="Flutter.MainToolbarActions">}.
 * <p>
 * Upstream adds it to the new UI {@code RunToolbarMainActionGroup} before {@code RedesignedRunConfigurationSelector}, and a copy
 * ({@code Flutter.MainToolbarActionsLegacy}) to the old UI {@code ToolbarRunGroup} before {@code RunConfiguration}.
 * Consulo has only {@code ToolbarRunGroup} (main toolbar and navigation bar), so this group goes there and the legacy copy is not registered.
 */
@ActionImpl(id = "Flutter.MainToolbarActions", children = {
  @ActionRef(type = DeviceSelectorAction.class),
  @ActionRef(type = DeviceSelectorRefresherAction.class)
}, parents = @ActionParentRef(value = @ActionRef(id = "ToolbarRunGroup"), anchor = ActionRefAnchor.BEFORE,
  relatedToAction = @ActionRef(id = "RunConfiguration")))
public class FlutterMainToolbarActionsGroup extends DefaultActionGroup {
}
