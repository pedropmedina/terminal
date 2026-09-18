package com.acteque.terminal;

import com.acteque.terminal.reload.ReloadRegistrar;
import com.acteque.terminal.reload.ReloadTarget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import javafx.application.Platform;

/** Development hooks invoked by the project's HotswapAgent plugin. */
public final class HotReloadSupport implements ReloadRegistrar {

  private static final Set<ReloadTarget> TARGETS = Collections.newSetFromMap(new WeakHashMap<>());

  @Override
  public void register(Object candidate) {
    if (!(candidate instanceof ReloadTarget target)) {
      return;
    }

    synchronized (TARGETS) {
      TARGETS.add(target);
    }
  }

  public static void refreshViewsAfterReload() {
    Platform.runLater(() -> snapshotTargets().forEach(ReloadTarget::refreshView));
  }

  public static void refreshStylesheetsAfterReload() {
    Platform.runLater(() -> snapshotTargets().forEach(ReloadTarget::refreshStylesheets));
  }

  private static ArrayList<ReloadTarget> snapshotTargets() {
    synchronized (TARGETS) {
      return new ArrayList<>(TARGETS);
    }
  }
}
