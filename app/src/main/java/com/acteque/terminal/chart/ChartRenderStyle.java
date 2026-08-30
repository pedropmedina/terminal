package com.acteque.terminal.chart;

import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

/** CSS-resolved presentation values used by Canvas drawing helpers. */
record ChartRenderStyle(
  Paint background,
  Paint axis,
  Paint grid,
  Paint mutedForeground,
  Paint series,
  Paint crosshair,
  Paint badgeBackground,
  Paint badgeForeground,
  Paint primary,
  Paint primaryForeground,
  Font axisFont,
  Font badgeFont,
  double axisLineWidth,
  double gridLineWidth,
  double seriesLineWidth,
  double badgeHeight,
  double controlRadius
) {}
