package com.acteque.terminal.ui.tooltip;

import java.util.Locale;
import java.util.Objects;
import javafx.css.PseudoClass;
import javafx.scene.layout.Region;

/** Internal popup surface that lays out tooltip content and its directional arrow. */
final class TooltipPopup extends Region {

  private static final double ARROW_SIZE = 10.0;
  private static final double ARROW_OVERLAP = 1.0;
  private static final double ARROW_RADIUS = ARROW_SIZE / Math.sqrt(2.0);
  private static final double ARROW_EXTENSION = ARROW_RADIUS - ARROW_OVERLAP;
  private static final PseudoClass OPEN = PseudoClass.getPseudoClass("open");
  private static final PseudoClass CLOSED = PseudoClass.getPseudoClass("closed");
  private static final String SIDE_STYLE_PREFIX = "tooltip-side-";

  private final TooltipContent content;
  private final Region arrow = new Region();
  private Tooltip.Side resolvedSide = Tooltip.Side.TOP;
  private String appliedSideStyleClass;

  TooltipPopup(TooltipContent content) {
    this.content = Objects.requireNonNull(content, "content cannot be null");
    getStyleClass().add("core-tooltip-content");
    arrow.getStyleClass().add("core-tooltip-arrow");
    arrow.setManaged(false);
    getChildren().addAll(content, arrow);
    setMouseTransparent(true);
    setResolvedSide(Tooltip.Side.TOP);
    setOpen(false);
  }

  TooltipContent getContent() {
    return content;
  }

  Region getArrow() {
    return arrow;
  }

  void setOpen(boolean open) {
    content.pseudoClassStateChanged(OPEN, open);
    content.pseudoClassStateChanged(CLOSED, !open);
    arrow.pseudoClassStateChanged(OPEN, open);
    arrow.pseudoClassStateChanged(CLOSED, !open);
  }

  void setResolvedSide(Tooltip.Side side) {
    resolvedSide = Objects.requireNonNull(side, "side cannot be null");
    if (appliedSideStyleClass != null) {
      getStyleClass().remove(appliedSideStyleClass);
    }
    appliedSideStyleClass = SIDE_STYLE_PREFIX + side.name().toLowerCase(Locale.ROOT).replace('_', '-');
    getStyleClass().add(appliedSideStyleClass);
    requestLayout();
  }

  @Override
  protected double computeMinWidth(double height) {
    return computePrefWidth(height);
  }

  @Override
  protected double computeMinHeight(double width) {
    return computePrefHeight(width);
  }

  @Override
  protected double computePrefWidth(double height) {
    double contentWidth = content.prefWidth(-1.0);
    return isVerticalSide() ? Math.max(ARROW_SIZE, contentWidth) : contentWidth + ARROW_EXTENSION;
  }

  @Override
  protected double computePrefHeight(double width) {
    double contentWidth = content.prefWidth(-1.0);
    double contentHeight = content.prefHeight(contentWidth);
    return isVerticalSide() ? contentHeight + ARROW_EXTENSION : Math.max(ARROW_SIZE, contentHeight);
  }

  @Override
  protected void layoutChildren() {
    double width = getWidth();
    double height = getHeight();
    double surfaceWidth = isVerticalSide() ? width : Math.max(0.0, width - ARROW_EXTENSION);
    double surfaceHeight = isVerticalSide() ? Math.max(0.0, height - ARROW_EXTENSION) : height;
    double surfaceX = resolvedSide == Tooltip.Side.RIGHT ? ARROW_EXTENSION : 0.0;
    double surfaceY = resolvedSide == Tooltip.Side.BOTTOM ? ARROW_EXTENSION : 0.0;
    content.resizeRelocate(surfaceX, surfaceY, surfaceWidth, surfaceHeight);

    if (isVerticalSide()) {
      layoutVerticalArrow(width, surfaceHeight, surfaceY);
    } else {
      layoutHorizontalArrow(height, surfaceWidth, surfaceX);
    }
  }

  private void layoutVerticalArrow(double width, double surfaceHeight, double surfaceY) {
    double centerX = content.getLayoutX() + content.getWidth() / 2.0;
    double arrowX = clamp(centerX - ARROW_SIZE / 2.0, 2.0, width - ARROW_SIZE - 2.0);
    double centerY = resolvedSide == Tooltip.Side.TOP ? surfaceHeight - ARROW_OVERLAP : surfaceY + ARROW_OVERLAP;
    arrow.resizeRelocate(arrowX, centerY - ARROW_SIZE / 2.0, ARROW_SIZE, ARROW_SIZE);
  }

  private void layoutHorizontalArrow(double height, double surfaceWidth, double surfaceX) {
    double centerX = resolvedSide == Tooltip.Side.LEFT ? surfaceWidth - ARROW_OVERLAP : surfaceX + ARROW_OVERLAP;
    double centerY = content.getLayoutY() + content.getHeight() / 2.0;
    double arrowY = clamp(centerY - ARROW_SIZE / 2.0, 2.0, height - ARROW_SIZE - 2.0);
    arrow.resizeRelocate(centerX - ARROW_SIZE / 2.0, arrowY, ARROW_SIZE, ARROW_SIZE);
  }

  private boolean isVerticalSide() {
    return resolvedSide == Tooltip.Side.TOP || resolvedSide == Tooltip.Side.BOTTOM;
  }

  private static double clamp(double value, double minimum, double maximum) {
    return Math.max(minimum, Math.min(value, Math.max(minimum, maximum)));
  }
}
