package com.acteque.terminal.ui;

import com.acteque.terminal.ui.behavior.KineticScroll;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.TableViewSkin;

/** A TableView with velocity-based scrolling for pixel scroll gestures. */
public final class TableView<T> extends javafx.scene.control.TableView<T> {

  private final ReadOnlyBooleanWrapper gliding = new ReadOnlyBooleanWrapper(this, "gliding");

  public ReadOnlyBooleanProperty glidingProperty() {
    return gliding.getReadOnlyProperty();
  }

  public boolean isGliding() {
    return gliding.get();
  }

  @Override
  protected Skin<?> createDefaultSkin() {
    return new KineticTableViewSkin<>(this, gliding);
  }

  private static final class KineticTableViewSkin<T> extends TableViewSkin<T> {

    private final KineticScroll kineticScroll;

    private KineticTableViewSkin(javafx.scene.control.TableView<T> tableView, ReadOnlyBooleanWrapper gliding) {
      super(tableView);
      kineticScroll = new KineticScroll(
        tableView,
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
