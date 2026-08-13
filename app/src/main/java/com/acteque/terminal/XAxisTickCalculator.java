package com.acteque.terminal;

import java.util.ArrayList;
import java.util.List;

final class XAxisTickCalculator {

  private static final int DEFAULT_TICK_COUNT = 7;

  private XAxisTickCalculator() {}

  static List<XAxisTick> calculate(
    int totalPointCount,
    int firstVisibleDataIndex,
    int visibleSlotCount,
    double chartWidth,
    ChartInterval interval
  ) {
    if (totalPointCount <= 0 || visibleSlotCount <= 0 || chartWidth <= 0) {
      return List.of();
    }

    if (visibleSlotCount == 1) {
      return List.of(new XAxisTick(0, firstVisibleDataIndex));
    }

    double pointSpacing = chartWidth / (visibleSlotCount - 1);
    int spacingStride = Math.max(1, (int) Math.ceil(interval.minimumLabelSpacing() / pointSpacing));
    int collisionSafeTickCount = Math.max(2, (int) Math.floor(chartWidth / interval.minimumLabelSpacing()) + 1);
    double zoomFactor = (double) totalPointCount / visibleSlotCount;
    int zoomTickCount = Math.max(DEFAULT_TICK_COUNT, (int) Math.ceil(DEFAULT_TICK_COUNT * zoomFactor));
    int targetTickCount = Math.min(visibleSlotCount, Math.min(collisionSafeTickCount, zoomTickCount));
    int densityStride =
      targetTickCount <= 1
        ? visibleSlotCount
        : (int) Math.ceil((double) (visibleSlotCount - 1) / (targetTickCount - 1));
    int stride = Math.max(spacingStride, densityStride);

    int lastVisibleDataIndex = firstVisibleDataIndex + visibleSlotCount - 1;
    int firstTickDataIndex = Math.floorDiv(firstVisibleDataIndex + stride - 1, stride) * stride;
    List<XAxisTick> ticks = new ArrayList<>();
    for (int dataIndex = firstTickDataIndex; dataIndex <= lastVisibleDataIndex; dataIndex += stride) {
      ticks.add(new XAxisTick(dataIndex - firstVisibleDataIndex, dataIndex));
    }
    return List.copyOf(ticks);
  }

  record XAxisTick(int slotIndex, int dataIndex) {}
}
