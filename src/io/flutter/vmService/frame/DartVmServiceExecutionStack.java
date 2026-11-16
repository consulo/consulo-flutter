package io.flutter.vmService.frame;

import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.application.AllIcons;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XStackFrame;
import io.flutter.vmService.DartVmServiceDebugProcess;
import org.dartlang.vm.service.element.Frame;
import org.dartlang.vm.service.element.InstanceRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class DartVmServiceExecutionStack extends XExecutionStack {
  private final DartVmServiceDebugProcess myDebugProcess;
  private final String myIsolateId;
  @Nullable private final XStackFrame myTopFrame;
  @Nullable private final InstanceRef myException;

  public DartVmServiceExecutionStack(@NotNull final DartVmServiceDebugProcess debugProcess,
                                     @NotNull final String isolateId,
                                     @NotNull final String isolateName,
                                     @Nullable final Frame topFrame,
                                     @Nullable final InstanceRef exception) {
    // topFrame is not null for (and only for) the active execution stack
    super(debugProcess.isIsolateSuspended(isolateId) ? beautify(isolateName)
                                                     : beautify(isolateName) + " (running)",
          topFrame != null ? ExecutionDebugIconGroup.threadThreadcurrent()
                           : debugProcess.isIsolateSuspended(isolateId) ? ExecutionDebugIconGroup.threadThreadatbreakpoint()
                                                                        : ExecutionDebugIconGroup.threadThreadrunning());
    myDebugProcess = debugProcess;
    myIsolateId = isolateId;
    myException = exception;
    myTopFrame = topFrame == null ? null : new DartVmServiceStackFrame(debugProcess, isolateId, topFrame, null, exception);
  }

  @NotNull
  private static String beautify(@NotNull final String isolateName) {
    // in tests it is "foo_test.dart%22%20as%20test;%0A%0A%20%20%20%20%20%20%20%20void%20main(_,%20SendPort%20message)..."
    final int index = isolateName.indexOf(".dart%22%20as%20test;");
    return index > 0 ? isolateName.substring(0, index + ".dart".length()) : isolateName;
  }

  @Nullable
  @Override
  public XStackFrame getTopFrame() {
    // engine calls getTopFrame for active execution stack only, for which myTopFrame is calculated in constructor
    return myTopFrame;
  }

  // Consulo: computeStackFrames has no firstFrameIndex, the frames view does not add the top frame itself
  @Override
  public void computeStackFrames(@NotNull final XStackFrameContainer container) {
    computeStackFrames(0, container);
  }

  public void computeStackFrames(final int firstFrameIndex, @NotNull final XStackFrameContainer container) {
    if (myDebugProcess.isIsolateSuspended(myIsolateId)) {
      myDebugProcess.getVmServiceWrapper().computeStackFrames(myIsolateId, firstFrameIndex, container, myException);
    }
    else {
      container.addStackFrames(Collections.emptyList(), true);
    }
  }

  public String getIsolateId() {
    return myIsolateId;
  }
}
