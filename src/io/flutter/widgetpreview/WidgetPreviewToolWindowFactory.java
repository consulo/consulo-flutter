package io.flutter.widgetpreview;

import consulo.project.Project;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import io.flutter.sdk.FlutterSdk;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;

public class WidgetPreviewToolWindowFactory implements ToolWindowFactory {
  public static final String TOOL_WINDOW_ID = "Flutter Widget Preview";

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    WidgetPreviewPanel widgetPreviewPanel = new WidgetPreviewPanel(project, toolWindow);
    ContentFactory contentFactory = ContentFactory.getInstance();
    Content content = contentFactory.createContent(widgetPreviewPanel, "", false);
    toolWindow.getContentManager().addContent(content);
  }

  @Override
  public Object isApplicableAsync(@NotNull Project project, @NotNull Continuation<? super Boolean> $completion) {
    // If we know for sure the Flutter SDK version is too old, we won't show options to open this tool window.
    FlutterSdk sdk = FlutterSdk.getFlutterSdk(project);
    if (sdk != null && !sdk.getVersion().canUseWidgetPreview()) {
      return false;
    }

    // For other cases, let the panel handle SDK version issues (e.g. missing SDK version).
    return true;
  }
}
