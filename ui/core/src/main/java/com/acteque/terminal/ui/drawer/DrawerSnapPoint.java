package com.acteque.terminal.ui.drawer;

/** A vertical drawer height expressed as a viewport fraction, pixels, or rem units. */
public record DrawerSnapPoint(Unit unit, double value) {
  public enum Unit {
    FRACTION,
    PIXELS,
    REM,
  }

  public DrawerSnapPoint {
    if (unit == null || !Double.isFinite(value) || value <= 0 || (unit == Unit.FRACTION && value > 1)) {
      throw new IllegalArgumentException("Invalid drawer snap point");
    }
  }

  public static DrawerSnapPoint fraction(double value) {
    return new DrawerSnapPoint(Unit.FRACTION, value);
  }

  public static DrawerSnapPoint pixels(double value) {
    return new DrawerSnapPoint(Unit.PIXELS, value);
  }

  public static DrawerSnapPoint rem(double value) {
    return new DrawerSnapPoint(Unit.REM, value);
  }

  double resolve(double viewportHeight) {
    return switch (unit) {
      case FRACTION -> value * viewportHeight;
      case PIXELS -> value;
      case REM -> value * 16.0;
    };
  }
}
