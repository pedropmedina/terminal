package com.acteque.terminal.chart.canvas;

import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

/**
 * CSS-resolved presentation values used by canvas drawing helpers.
 *
 * @param background canvas background paint
 * @param axis axis paint
 * @param grid grid paint
 * @param mutedForeground secondary label paint
 * @param line line-series paint
 * @param bar OHLC-bar paint
 * @param candleUp rising-candle fill
 * @param candleDown falling-candle fill
 * @param candleBorder candle outline paint
 * @param crosshair crosshair stroke paint
 * @param badgeBackground crosshair badge background
 * @param badgeForeground crosshair badge text paint
 * @param primary active-control paint
 * @param primaryForeground active-control foreground paint
 * @param axisFont axis label font
 * @param badgeFont badge label font
 * @param axisLineWidth axis stroke width
 * @param gridLineWidth grid and crosshair stroke width
 * @param lineWidth line-series stroke width
 * @param barStrokeWidth OHLC-bar stroke width
 * @param barTickMaxWidth maximum OHLC tick width
 * @param markerDiameter point-marker diameter
 * @param areaOpacity area-series fill opacity
 * @param candleBodyMaxWidth maximum candle-body width
 * @param candleStrokeWidth candle stroke width
 * @param badgeHeight axis badge height
 * @param controlRadius control corner radius
 * @param axisLabelSpacing minimum horizontal axis-label spacing
 * @param leftMargin chart-area left margin
 * @param rightMargin chart-area right margin
 * @param topMargin chart-area top margin
 * @param bottomMargin chart-area bottom margin
 * @param currentPriceTextOffset current-price label inset
 * @param autoscaleButtonSize autoscale button width and height
 * @param autoscaleButtonXOffset autoscale button horizontal offset
 * @param autoscaleButtonYOffset autoscale button vertical offset
 * @param axisTickLength axis tick length
 * @param axisTextOffset axis label inset
 * @param calendarBadgeWidth calendar crosshair badge width
 * @param intradayBadgeWidth intraday crosshair badge width
 * @param badgeTextOffset badge text inset
 * @param crosshairDashLength crosshair dash and gap length
 */
record RenderStyle(
  Paint background,
  Paint axis,
  Paint grid,
  Paint mutedForeground,
  Paint line,
  Paint bar,
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
  double barStrokeWidth,
  double barTickMaxWidth,
  double markerDiameter,
  double areaOpacity,
  double candleBodyMaxWidth,
  double candleStrokeWidth,
  double badgeHeight,
  double controlRadius,
  double axisLabelSpacing,
  double leftMargin,
  double rightMargin,
  double topMargin,
  double bottomMargin,
  double currentPriceTextOffset,
  double autoscaleButtonSize,
  double autoscaleButtonXOffset,
  double autoscaleButtonYOffset,
  double axisTickLength,
  double axisTextOffset,
  double calendarBadgeWidth,
  double intradayBadgeWidth,
  double badgeTextOffset,
  double crosshairDashLength
) {}
