package consulo.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.flutter.localize.FlutterConsuloLocalize;
import consulo.ui.ex.action.DefaultActionGroup;
import io.flutter.actions.FlutterBuildActionGroup;

/**
 * Upstream plugin.xml {@code <group id="FlutterBuildGroup">} (inside {@code FlutterBuildActionGroup}).
 */
@ActionImpl(id = "FlutterBuildGroup", children = {
  @ActionRef(type = FlutterBuildActionGroup.AAR.class),
  @ActionRef(type = FlutterBuildActionGroup.APK.class),
  @ActionRef(type = FlutterBuildActionGroup.AppBundle.class),
  @ActionRef(type = FlutterBuildActionGroup.Bundle.class),
  @ActionRef(type = FlutterBuildActionGroup.Ios.class),
  @ActionRef(type = FlutterBuildActionGroup.Linux.class),
  @ActionRef(type = FlutterBuildActionGroup.Macos.class),
  @ActionRef(type = FlutterBuildActionGroup.Web.class),
  @ActionRef(type = FlutterBuildActionGroup.Windows.class)
})
public class FlutterBuildGroup extends DefaultActionGroup {
  public FlutterBuildGroup() {
    getTemplatePresentation().setText(FlutterConsuloLocalize.groupFlutterbuildgroupText());
    setPopup(true);
  }
}
