package consulo.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.ui.ex.action.DefaultActionGroup;
import io.flutter.run.FlutterDebugProcessActions;

/**
 * Upstream plugin.xml {@code <group id="Flutter.DebugProcess.TopToolbar" popup="false"/>}: lets dependent plugins contribute
 * inline actions to Flutter run/debug content toolbars (see {@link FlutterDebugProcessActions}).
 */
@ActionImpl(id = FlutterDebugProcessActions.TOP_TOOLBAR_EXTENSION_GROUP_ID)
public class FlutterDebugProcessTopToolbarGroup extends DefaultActionGroup {
}
