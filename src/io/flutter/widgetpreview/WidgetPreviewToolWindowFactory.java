package io.flutter.widgetpreview;

import consulo.annotation.component.ExtensionImpl;

import consulo.project.Project;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import io.flutter.sdk.FlutterSdk;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import io.flutter.icons.FlutterIcons;
import org.jetbrains.annotations.NotNull;

@ExtensionImpl
public class WidgetPreviewToolWindowFactory implements ToolWindowFactory {
  public static final String TOOL_WINDOW_ID = "Flutter Widget Preview";

  // Consulo: the tool window id, anchor, icon and display name of upstream plugin.xml are provided by the factory
  @NotNull
  @Override
  public String getId() {
    return TOOL_WINDOW_ID;
  }

  @NotNull
  @Override
  public ToolWindowAnchor getAnchor() {
    return ToolWindowAnchor.RIGHT;
  }

  @NotNull
  @Override
  public Image getIcon() {
    return FlutterIcons.WidgetPreview;
  }

  @NotNull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("Flutter Widget Preview");
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    WidgetPreviewPanel widgetPreviewPanel = new WidgetPreviewPanel(project, toolWindow);
    ContentFactory contentFactory = ContentFactory.getInstance();
    Content content = contentFactory.createContent(widgetPreviewPanel, "", false);
    toolWindow.getContentManager().addContent(content);
  }

  // Consulo: shouldBeAvailable instead of isApplicableAsync
  @Override
  public boolean shouldBeAvailable(@NotNull Project project) {
    // If we know for sure the Flutter SDK version is too old, we won't show options to open this tool window.
    FlutterSdk sdk = FlutterSdk.getFlutterSdk(project);
    if (sdk != null && !sdk.getVersion().canUseWidgetPreview()) {
      return false;
    }

    // For other cases, let the panel handle SDK version issues (e.g. missing SDK version).
    return true;
  }
}
