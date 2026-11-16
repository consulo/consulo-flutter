package consulo.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.flutter.localize.FlutterConsuloLocalize;
import consulo.localize.LocalizeValue;
import io.flutter.actions.FlutterGettingStartedAction;

/**
 * Upstream plugin.xml {@code <action id="Flutter.FlutterHelp" class="io.flutter.actions.FlutterGettingStartedAction">}:
 * upstream registers {@link FlutterGettingStartedAction} a second time under this id; {@code @ActionImpl} is one id per class.
 */
@ActionImpl(id = "Flutter.FlutterHelp", parents = @ActionParentRef(value = @ActionRef(id = "HelpMenu"), anchor = ActionRefAnchor.AFTER,
  relatedToAction = @ActionRef(id = "HelpTopics")))
public class FlutterHelpAction extends FlutterGettingStartedAction {
  public FlutterHelpAction() {
    getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterFlutterhelpText());
    getTemplatePresentation().setDescription(LocalizeValue.empty());
  }
}
