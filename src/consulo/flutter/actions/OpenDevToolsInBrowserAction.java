package consulo.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.flutter.localize.FlutterConsuloLocalize;
import io.flutter.icons.FlutterIcons;
import io.flutter.run.OpenDevToolsAction;

/**
 * Upstream plugin.xml {@code <action id="io.flutter.OpenDevToolsAction" class="io.flutter.run.OpenDevToolsAction">}:
 * upstream registers {@link OpenDevToolsAction} a second time under this id (with an icon); {@code @ActionImpl} is one id per class.
 */
@ActionImpl(id = "io.flutter.OpenDevToolsAction")
public class OpenDevToolsInBrowserAction extends OpenDevToolsAction {
  public OpenDevToolsInBrowserAction() {
    getTemplatePresentation().setText(FlutterConsuloLocalize.actionIoFlutterOpendevtoolsactionText());
    getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionIoFlutterOpendevtoolsactionDescription());
    getTemplatePresentation().setIcon(FlutterIcons.Dart_16);
  }
}
