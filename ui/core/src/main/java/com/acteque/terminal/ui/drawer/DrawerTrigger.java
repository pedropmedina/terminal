package com.acteque.terminal.ui.drawer;

import com.acteque.terminal.ui.Button;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

/** A core button that opens its associated drawer. */
public final class DrawerTrigger extends Button {

  private final ObjectProperty<Drawer> drawer = new SimpleObjectProperty<>(this, "drawer");

  public DrawerTrigger(String text, Drawer drawer) {
    this(text, null, Variant.DEFAULT, Size.DEFAULT, drawer);
  }

  public DrawerTrigger(String text, Node graphic, Variant variant, Size size, Drawer drawer) {
    super(text, graphic, variant, size);
    getStyleClass().add("core-drawer-trigger");
    setDrawer(drawer);
    setOnAction(event -> getDrawer().show());
  }

  public Drawer getDrawer() {
    return drawer.get();
  }

  public void setDrawer(Drawer value) {
    drawer.set(Objects.requireNonNull(value, "drawer cannot be null"));
  }

  public ObjectProperty<Drawer> drawerProperty() {
    return drawer;
  }
}
