package com.acteque.terminal.ui.icons;

import java.util.Objects;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Affine;

/** A CSS-stylable JavaFX rendering of generated Lucide vector geometry. */
public final class LucideIcon extends Region {

  public static final double DEFAULT_SIZE = 16.0;

  private static final String ROOT_STYLE_CLASS = "lucide-icon";
  private static final String PATH_STYLE_CLASS = "lucide-icon-path";

  private final LucideGlyph glyph;
  private final SVGPath path = new SVGPath();
  private final Affine artworkTransform = new Affine();

  private final DoubleProperty iconSize = new DoublePropertyBase(DEFAULT_SIZE) {
    @Override
    public void set(double value) {
      super.set(requirePositiveFinite(value, "iconSize"));
    }

    @Override
    protected void invalidated() {
      requestLayout();
    }

    @Override
    public Object getBean() {
      return LucideIcon.this;
    }

    @Override
    public String getName() {
      return "iconSize";
    }
  };

  public LucideIcon(LucideGlyph glyph) {
    this(glyph, DEFAULT_SIZE);
  }

  public LucideIcon(LucideGlyph glyph, double iconSize) {
    this.glyph = Objects.requireNonNull(glyph, "glyph");
    requirePositiveFinite(glyph.width(), "glyph.width");
    requirePositiveFinite(glyph.height(), "glyph.height");
    path.setContent(Objects.requireNonNull(glyph.pathData(), "glyph.pathData"));
    if (glyph.pathData().isBlank()) {
      throw new IllegalArgumentException("glyph.pathData must not be blank");
    }

    getStyleClass().add(ROOT_STYLE_CLASS);
    path.getStyleClass().add(PATH_STYLE_CLASS);
    path.setManaged(false);
    path.setMouseTransparent(true);
    path.getTransforms().add(artworkTransform);
    getChildren().add(path);

    setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
    setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    setMouseTransparent(true);
    setIconSize(iconSize);
  }

  public LucideGlyph getGlyph() {
    return glyph;
  }

  public DoubleProperty iconSizeProperty() {
    return iconSize;
  }

  public double getIconSize() {
    return iconSize.get();
  }

  public void setIconSize(double value) {
    iconSize.set(requirePositiveFinite(value, "iconSize"));
  }

  @Override
  protected double computePrefWidth(double height) {
    return scaledWidth();
  }

  @Override
  protected double computePrefHeight(double width) {
    return scaledHeight();
  }

  @Override
  protected void layoutChildren() {
    double scale = Math.min(getWidth() / glyph.width(), getHeight() / glyph.height());
    double translateX = (getWidth() - glyph.width() * scale) / 2.0;
    double translateY = (getHeight() - glyph.height() * scale) / 2.0;
    artworkTransform.setToTransform(scale, 0.0, translateX, 0.0, scale, translateY);
  }

  private double scaledWidth() {
    return (getIconSize() * glyph.width()) / Math.max(glyph.width(), glyph.height());
  }

  private double scaledHeight() {
    return (getIconSize() * glyph.height()) / Math.max(glyph.width(), glyph.height());
  }

  private static double requirePositiveFinite(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be positive and finite");
    }
    return value;
  }
}
