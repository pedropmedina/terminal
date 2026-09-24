package com.acteque.terminal.ui.combobox;

import java.util.Locale;
import java.util.Objects;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

/** Positioned popup surface for a {@link Combobox}. */
public final class ComboboxContent<T> extends VBox {

  /** Popup alignment relative to its anchor. */
  public enum Align {
    START,
    CENTER,
    END,
  }

  /** Preferred side on which the popup is placed. */
  public enum Side {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    INLINE_START,
    INLINE_END,
  }

  /** Popup lifecycle phase used by CSS transitions. */
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
  private static final PseudoClass EMPTY = PseudoClass.getPseudoClass("empty");
  private static final PseudoClass CHIPS = PseudoClass.getPseudoClass("chips");

  private final Combobox<T> combobox;
  private final ObjectProperty<Align> align = requiredProperty("align", Align.START);
  private final ObjectProperty<Side> side = requiredProperty("side", Side.BOTTOM);
  private final DoubleProperty sideOffset = new SimpleDoubleProperty(this, "sideOffset", 6.0);
  private final DoubleProperty alignOffset = new SimpleDoubleProperty(this, "alignOffset", 0.0);
  private final ObjectProperty<Node> anchor = new SimpleObjectProperty<>(this, "anchor") {
    @Override
    protected void invalidated() {
      pseudoClassStateChanged(CHIPS, get() instanceof ComboboxChips<?>);
      combobox.reposition();
    }
  };
  private String sideStyleClass;

  /**
   * Creates popup content and installs it on the combobox root.
   *
   * @param combobox the owning combobox
   * @param children the popup children
   */
  public ComboboxContent(Combobox<T> combobox, Node... children) {
    this.combobox = Objects.requireNonNull(combobox, "combobox cannot be null");
    getStyleClass().add("core-combobox-content");
    setAccessibleRole(AccessibleRole.PARENT);
    setFocusTraversable(false);
    getChildren().addAll(children);
    setResolvedSide(Side.BOTTOM);
    setPhase(Phase.CLOSED);
    align.addListener(ignored -> this.combobox.reposition());
    side.addListener(ignored -> this.combobox.reposition());
    sideOffset.addListener(ignored -> this.combobox.reposition());
    alignOffset.addListener(ignored -> this.combobox.reposition());
    this.combobox.setContent(this);
  }

  /** Returns the popup alignment. */
  public Align getAlign() {
    return align.get();
  }

  /** Sets the popup alignment. */
  public void setAlign(Align next) {
    align.set(Objects.requireNonNull(next, "align cannot be null"));
  }

  /** Returns the alignment property. */
  public ObjectProperty<Align> alignProperty() {
    return align;
  }

  /** Returns the preferred popup side. */
  public Side getSide() {
    return side.get();
  }

  /** Sets the preferred popup side. */
  public void setSide(Side next) {
    side.set(Objects.requireNonNull(next, "side cannot be null"));
  }

  /** Returns the side property. */
  public ObjectProperty<Side> sideProperty() {
    return side;
  }

  /** Returns the gap between anchor and popup. */
  public double getSideOffset() {
    return sideOffset.get();
  }

  /** Sets the gap between anchor and popup. */
  public void setSideOffset(double next) {
    requireFinite(next, "sideOffset");
    sideOffset.set(next);
  }

  /** Returns the side-offset property. */
  public DoubleProperty sideOffsetProperty() {
    return sideOffset;
  }

  /** Returns the offset along the aligned edge. */
  public double getAlignOffset() {
    return alignOffset.get();
  }

  /** Sets the offset along the aligned edge. */
  public void setAlignOffset(double next) {
    requireFinite(next, "alignOffset");
    alignOffset.set(next);
  }

  /** Returns the alignment-offset property. */
  public DoubleProperty alignOffsetProperty() {
    return alignOffset;
  }

  /** Returns the explicit anchor, or {@code null} to use the registered input. */
  public Node getAnchor() {
    return anchor.get();
  }

  /** Sets an explicit popup anchor, commonly a {@link ComboboxChips}. */
  public void setAnchor(Node next) {
    anchor.set(next);
  }

  /** Returns the explicit-anchor property. */
  public ObjectProperty<Node> anchorProperty() {
    return anchor;
  }

  /** Applies empty state to the surface and its empty/list descendants. */
  void setEmpty(boolean value) {
    pseudoClassStateChanged(EMPTY, value);
    updateEmptyState(this, value);
  }

  /** Applies screen-derived height limits to the content and list. */
  void setAvailableHeight(double value) {
    setMaxHeight(Math.max(0.0, value));
    for (Node child : getChildren()) {
      if (child instanceof ComboboxList<?> list) {
        list.setAvailableHeight(value);
      }
    }
  }

  /** Applies lifecycle pseudo-classes for popup animation. */
  void setPhase(Phase phase) {
    pseudoClassStateChanged(OPENING, phase == Phase.OPENING);
    pseudoClassStateChanged(OPEN, phase == Phase.OPEN);
    pseudoClassStateChanged(CLOSING, phase == Phase.CLOSING);
    pseudoClassStateChanged(CLOSED, phase == Phase.CLOSED);
  }

  /** Applies the physical side class used by directional animation. */
  void setResolvedSide(Side resolvedSide) {
    if (sideStyleClass != null) {
      getStyleClass().remove(sideStyleClass);
    }
    sideStyleClass = "combobox-side-" + resolvedSide.name().toLowerCase(Locale.ROOT).replace('_', '-');
    getStyleClass().add(sideStyleClass);
  }

  /** Creates a property that rejects null assignments. */
  private <V> ObjectProperty<V> requiredProperty(String name, V initial) {
    return new ObjectPropertyBase<>(initial) {
      @Override
      public void set(V next) {
        super.set(Objects.requireNonNull(next, name + " cannot be null"));
      }

      @Override
      public Object getBean() {
        return ComboboxContent.this;
      }

      @Override
      public String getName() {
        return name;
      }
    };
  }

  /** Rejects non-finite positioning offsets. */
  private static void requireFinite(double value, String name) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be finite");
    }
  }

  /** Recursively updates empty and list descendants in composed content. */
  private static void updateEmptyState(Parent parent, boolean value) {
    for (Node child : parent.getChildrenUnmodifiable()) {
      if (child instanceof ComboboxEmpty empty) {
        empty.setEmpty(value);
      } else if (child instanceof ComboboxList<?> list) {
        list.setEmpty(value);
      }
      if (child instanceof Parent nested) {
        updateEmptyState(nested, value);
      }
    }
  }
}
