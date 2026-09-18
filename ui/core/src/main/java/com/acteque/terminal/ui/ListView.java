package com.acteque.terminal.ui;

import com.acteque.terminal.ui.behavior.KineticScroll;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.ListViewSkin;

/** A ListView with velocity-based scrolling for pixel scroll gestures. */
public final class ListView<T> extends javafx.scene.control.ListView<T> {

  private final ReadOnlyBooleanWrapper gliding = new ReadOnlyBooleanWrapper(this, "gliding");

  public ReadOnlyBooleanProperty glidingProperty() {
    return gliding.getReadOnlyProperty();
  }

  public boolean isGliding() {
    return gliding.get();
  }

  @Override
  protected Skin<?> createDefaultSkin() {
    return new KineticListViewSkin<>(this, gliding);
  }

  private static final class KineticListViewSkin<T> extends ListViewSkin<T> {

    private final KineticScroll kineticScroll;

    private KineticListViewSkin(javafx.scene.control.ListView<T> listView, ReadOnlyBooleanWrapper gliding) {
      super(listView);
      kineticScroll = new KineticScroll(
        listView,
        requestedPixels -> getVirtualFlow().scrollPixels(requestedPixels),
        gliding
      );
    }

    @Override
    public void dispose() {
      kineticScroll.close();
      super.dispose();
    }
  }
}
