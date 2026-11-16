package consulo.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.flutter.localize.FlutterConsuloLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import io.flutter.actions.FlutterDoctorAction;
import io.flutter.actions.FlutterPackagesGetAction;
import io.flutter.actions.FlutterPackagesUpgradeAction;
import io.flutter.actions.FlutterUpgradeAction;
import io.flutter.actions.OpenInXcodeAction;
import io.flutter.icons.FlutterIcons;

/**
 * Upstream plugin.xml {@code <group id="FlutterToolsGroup">} (inside {@code FlutterPackagesExplorerActionGroup}).
 */
@ActionImpl(id = "FlutterToolsGroup", children = {
  @ActionRef(type = AnSeparator.class),
  @ActionRef(type = FlutterPackagesGetAction.class),
  @ActionRef(type = FlutterPackagesUpgradeAction.class),
  @ActionRef(type = AnSeparator.class),
  // Consulo: flutter.androidstudio.open is commented out upstream, flutter.appcode.open is dropped (AppCode is discontinued)
  @ActionRef(type = OpenInXcodeAction.class),
  @ActionRef(type = AnSeparator.class),
  @ActionRef(type = FlutterUpgradeAction.class),
  @ActionRef(type = FlutterDoctorAction.class)
})
public class FlutterToolsGroup extends DefaultActionGroup {
  public FlutterToolsGroup() {
    getTemplatePresentation().setText(FlutterConsuloLocalize.groupFluttertoolsgroupText());
    getTemplatePresentation().setDescription(FlutterConsuloLocalize.groupFluttertoolsgroupDescription());
    getTemplatePresentation().setIcon(FlutterIcons.Flutter);
    setPopup(true);
  }
}
