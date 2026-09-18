package com.acteque.terminal.chart.canvas;

import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

/** CSS-resolved presentation values used by Canvas drawing helpers. */
record RenderStyle(
  Paint background,
  Paint axis,
  Paint grid,
  Paint mutedForeground,
  Paint line,
  Paint candleUp,
  Paint candleDown,
  Paint candleBorder,
  Paint crosshair,
  Paint badgeBackground,
  Paint badgeForeground,
  Paint primary,
  Paint primaryForeground,
  Font axisFont,
  Font badgeFont,
  double axisLineWidth,
  double gridLineWidth,
  double lineWidth,
  double areaOpacity,
  double candleBodyMaxWidth,
  double candleStrokeWidth,
  double badgeHeight,
  double controlRadius,
  double axisLabelSpacing
) {}
