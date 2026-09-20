package com.acteque.terminal.chartworkspace.inspector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.chart.ChartType;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChartWorkspaceInspectorInteractorTest {

  @Test
  void initializesAndUpdatesSettingsState() {
    ChartWorkspaceInspectorModel model = new ChartWorkspaceInspectorModel();
    ChartWorkspaceInspectorInteractor interactor = new ChartWorkspaceInspectorInteractor(model);

    interactor.initialize(ChartType.LINE);
    assertSame(ChartType.LINE, model.getChartType());
    assertFalse(model.isOpen());

    interactor.showChartTypes();
    assertTrue(model.isOpen());

    interactor.setChartType(ChartType.CANDLESTICK);
    assertSame(ChartType.CANDLESTICK, model.getChartType());

    interactor.close();
    assertFalse(model.isOpen());
  }

  @Test
  void selectingAChartTypeUpdatesStateClosesAndRoutesTheSelection() {
    ChartWorkspaceInspectorModel model = new ChartWorkspaceInspectorModel();
    ChartWorkspaceInspectorInteractor interactor = new ChartWorkspaceInspectorInteractor(model);
    AtomicReference<ChartType> selected = new AtomicReference<>();
    interactor.initialize(ChartType.LINE);
    interactor.onChartTypeSelected(selected::set);
    interactor.showChartTypes();

    interactor.selectChartType(ChartType.STEP_LINE);

    assertSame(ChartType.STEP_LINE, model.getChartType());
    assertFalse(model.isOpen());
    assertSame(ChartType.STEP_LINE, selected.get());
  }
}
