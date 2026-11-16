package io.flutter.vmService.frame;

import consulo.application.AllIcons;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.execution.debug.frame.XStackFrame;
import org.jetbrains.annotations.NotNull;

/**
 * A XStackFrame used to render a separator between two sets of async stack frames.
 */
public class DartAsyncMarkerFrame extends XStackFrame {
  public DartAsyncMarkerFrame() {
  }

  public void customizePresentation(@NotNull ColoredTextContainer component) {
    component.append("<asynchronous gap>", SimpleTextAttributes.EXCLUDED_ATTRIBUTES);
    component.setIcon(AllIcons.General.SeparatorH);
  }
}
