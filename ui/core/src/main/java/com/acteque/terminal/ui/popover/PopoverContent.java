package com.acteque.terminal.ui.popover;

import java.util.Locale;
import java.util.Objects;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** The positioned popover surface. */
public final class PopoverContent extends VBox {

  public enum Align {
    START,
    CENTER,
    END,
  }

  public enum Side {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    INLINE_START,
    INLINE_END,
  }

  enum Phase {
    OPENING,
    OPEN,
    CLOSING,
    CLOSED,
  }

  private static final PseudoClass OPENING = PseudoClass.getPseudoClass("opening");
  private static final PseudoClass OPEN = PseudoClass.getPseudoClass("open");
  private static final PseudoClass CLOSING = PseudoClass.getPseudoClass("closing");
  private static final PseudoClass CLOSED = PseudoClass.getPseudoClass("closed");
  private static final String SIDE_STYLE_PREFIX = "popover-side-";

  private final ObjectProperty<Align> align = requiredProperty("align", Align.CENTER);
  private final ObjectProperty<Side> side = requiredProperty("side", Side.BOTTOM);
  private final DoubleProperty alignOffset = new SimpleDoubleProperty(this, "alignOffset", 0.0);
  private final DoubleProperty sideOffset = new SimpleDoubleProperty(this, "sideOffset", 4.0);
  private Popover popover;
  private String appliedSideStyleClass;

  public PopoverContent(Node... children) {
    getStyleClass().add("core-popover-content");
    setAccessibleRole(AccessibleRole.PARENT);
    setFocusTraversable(true);
    getChildren().addAll(children);
    setResolvedSide(Side.BOTTOM);
    setPhase(Phase.CLOSED);
    align.addListener(ignored -> requestReposition());
    side.addListener(ignored -> requestReposition());
    alignOffset.addListener(ignored -> requestReposition());
    sideOffset.addListener(ignored -> requestReposition());
  }

  public Align getAlign() {
    return align.get();
  }

  public void setAlign(Align value) {
    align.set(Objects.requireNonNull(value, "align cannot be null"));
  }

  public ObjectProperty<Align> alignProperty() {
    return align;
  }

  public Side getSide() {
    return side.get();
  }

  public void setSide(Side value) {
    side.set(Objects.requireNonNull(value, "side cannot be null"));
  }

  public ObjectProperty<Side> sideProperty() {
    return side;
  }

  public double getAlignOffset() {
    return alignOffset.get();
  }

  public void setAlignOffset(double value) {
    alignOffset.set(value);
  }

  public DoubleProperty alignOffsetProperty() {
    return alignOffset;
  }

  public double getSideOffset() {
    return sideOffset.get();
  }

  public void setSideOffset(double value) {
    sideOffset.set(value);
  }

  public DoubleProperty sideOffsetProperty() {
    return sideOffset;
  }

  void setPopover(Popover value) {
    popover = value;
  }

  void setPhase(Phase phase) {
    pseudoClassStateChanged(OPENING, phase == Phase.OPENING);
    pseudoClassStateChanged(OPEN, phase == Phase.OPEN);
    pseudoClassStateChanged(CLOSING, phase == Phase.CLOSING);
    pseudoClassStateChanged(CLOSED, phase == Phase.CLOSED);
  }

  void setResolvedSide(Side resolvedSide) {
    if (appliedSideStyleClass != null) {
      getStyleClass().remove(appliedSideStyleClass);
    }
    appliedSideStyleClass = SIDE_STYLE_PREFIX + resolvedSide.name().toLowerCase(Locale.ROOT).replace('_', '-');
    getStyleClass().add(appliedSideStyleClass);
  }

  private void requestReposition() {
    if (popover != null) {
      popover.reposition();
    }
  }

  private <T> ObjectProperty<T> requiredProperty(String name, T initialValue) {
    return new ObjectPropertyBase<>(initialValue) {
      @Override
      public void set(T value) {
        super.set(Objects.requireNonNull(value, name + " cannot be null"));
      }

      @Override
      public Object getBean() {
        return PopoverContent.this;
      }

      @Override
      public String getName() {
        return name;
      }
    };
  }
}
