package com.acteque.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import com.acteque.terminal.ui.ChartReloadRegistrar;
import com.acteque.terminal.ui.RefreshableView;
import javafx.application.Platform;

/** Development hooks invoked by the project's HotswapAgent plugin. */
public final class HotReloadSupport implements ChartReloadRegistrar {

  private static final Set<RefreshableView> VIEWS = Collections.newSetFromMap(new WeakHashMap<>());

  @Override
  public void register(Object candidate) {
    if (!(candidate instanceof RefreshableView view)) {
      return;
    }

    synchronized (VIEWS) {
      VIEWS.add(view);
    }
  }

  public static void refreshViewsAfterReload() {
    Platform.runLater(() -> {
      ArrayList<RefreshableView> views;
      synchronized (VIEWS) {
        views = new ArrayList<>(VIEWS);
      }
      views.forEach(RefreshableView::refreshView);
    });
  }
}
