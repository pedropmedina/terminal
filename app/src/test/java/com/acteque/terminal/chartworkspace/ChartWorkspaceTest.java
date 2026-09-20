package com.acteque.terminal.chartworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.StubInstrumentCatalog;
import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartSplitDirection;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.InstrumentLoadResult;
import com.acteque.terminal.marketdata.MarketDataSession;
import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.marketlogos.LogoSession;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.resizable.ResizableHandle;
import com.acteque.terminal.ui.resizable.ResizablePanel;
import com.acteque.terminal.ui.resizable.ResizablePanelGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAction;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

class ChartWorkspaceTest {

  private static final PseudoClass ACTIVE_PSEUDO_CLASS = PseudoClass.getPseudoClass("workspace-active");

  @Test
  void createsEveryDirectionInTheRequestedOrderAndOrientation() {
    FxTestSupport.runAndWait(() -> {
      for (ChartSplitDirection direction : ChartSplitDirection.values()) {
        Fixture fixture = new Fixture();
        Chart source = fixture.initialize();
        assertSame(source, fixture.model.getActiveChart());

        fixture.interactor.split(source, direction);

        ChartWorkspaceSplit split = assertInstanceOf(ChartWorkspaceSplit.class, fixture.model.getRoot());
        assertEquals(
          direction == ChartSplitDirection.LEFT || direction == ChartSplitDirection.RIGHT
            ? Orientation.HORIZONTAL
            : Orientation.VERTICAL,
          split.orientation()
        );
        assertEquals(0.5, split.dividerPosition());
        Chart first = assertInstanceOf(ChartWorkspaceLeaf.class, split.first()).chart();
        Chart second = assertInstanceOf(ChartWorkspaceLeaf.class, split.second()).chart();
        if (direction == ChartSplitDirection.LEFT || direction == ChartSplitDirection.TOP) {
          assertSame(source, second);
          assertFalse(first == source);
        } else {
          assertSame(source, first);
          assertFalse(second == source);
        }
        Chart created = first == source ? second : first;
        assertSame(created, fixture.model.getActiveChart());
        fixture.interactor.close();
      }
    });
  }

  @Test
  void hidesTheActiveRingUntilTheWorkspaceHasMultipleCharts() {
    FxTestSupport.runAndWait(() -> {
      Fixture fixture = new Fixture();
      Chart source = fixture.initialize();
      ChartWorkspaceViewBuilder viewBuilder = new ChartWorkspaceViewBuilder(
        fixture.model,
        fixture.interactor::activate
      );
      StackPane view = viewBuilder.build();
      new AppThemeManager(new Scene(view, 1_000.0, 600.0), AppTheme.LIGHT);
      layout(view);

      assertSame(source, fixture.model.getActiveChart());
      assertFalse(isActive(source));
      fixture.interactor.close();
    });
  }

  @Test
  void activatesAChartFromItsMousePressWithoutConsumingTheEvent() {
    FxTestSupport.runAndWait(() -> {
      Fixture fixture = new Fixture();
      Chart source = fixture.initialize();
      fixture.interactor.split(source, ChartSplitDirection.RIGHT);
      Chart created = ((ChartWorkspaceLeaf) ((ChartWorkspaceSplit) fixture.model.getRoot()).second()).chart();
      ChartWorkspaceViewBuilder viewBuilder = new ChartWorkspaceViewBuilder(
        fixture.model,
        fixture.interactor::activate
      );
      StackPane view = viewBuilder.build();
      AppThemeManager themeManager = new AppThemeManager(new Scene(view, 1_000.0, 600.0), AppTheme.LIGHT);
      AtomicInteger chartPresses = new AtomicInteger();
      source.getView().addEventHandler(MouseEvent.MOUSE_PRESSED, ignored -> chartPresses.incrementAndGet());
      layout(view);

      Region sourceContainer = assertInstanceOf(Region.class, source.getView().getParent());
      Region createdContainer = assertInstanceOf(Region.class, created.getView().getParent());

      assertSame(created, fixture.model.getActiveChart());
      assertFalse(isActive(source));
      assertTrue(isActive(created));
      assertEquals(Color.web("#f5f5f5"), view.getBackground().getFills().getFirst().getFill());
      assertEquals(2.5, view.getPadding().getTop());
      assertEquals(Color.WHITE, sourceContainer.getBackground().getFills().getFirst().getFill());
      assertEquals(Color.web("#a1a1a1"), createdContainer.getBackground().getFills().getFirst().getFill());
      assertEquals(Color.WHITE, createdContainer.getBackground().getFills().get(1).getFill());
      assertNull(createdContainer.getBorder());
      assertNull(createdContainer.getEffect());
      Region createdSlot = assertInstanceOf(Region.class, createdContainer.getParent());
      assertTrue(createdSlot.getStyleClass().contains("chart-workspace-chart-slot"));
      assertEquals(1.5, createdSlot.getPadding().getTop());
      assertEquals(2.0, created.getView().getLayoutX());
      assertEquals(2.0, created.getView().getLayoutY());

      Bounds sourceBounds = sourceContainer.localToScene(sourceContainer.getBoundsInLocal());
      Bounds createdBounds = createdContainer.localToScene(createdContainer.getBoundsInLocal());
      assertEquals(4.0, sourceBounds.getMinX(), 0.75);
      assertEquals(4.0, sourceBounds.getMinY(), 0.75);
      assertEquals(4.0, view.getWidth() - createdBounds.getMaxX(), 0.75);
      assertEquals(4.0, view.getHeight() - createdBounds.getMaxY(), 0.75);
      assertEquals(4.0, createdBounds.getMinX() - sourceBounds.getMaxX(), 0.75);

      assertNull(createdContainer.getClip());
      Rectangle clip = assertInstanceOf(Rectangle.class, created.getView().getClip());
      assertEquals(created.getView().getWidth(), clip.getWidth());
      assertEquals(created.getView().getHeight(), clip.getHeight());
      assertEquals(8.0, clip.getArcWidth());
      assertEquals(8.0, clip.getArcHeight());

      source.getView().fireEvent(primaryMousePress());
      view.applyCss();

      assertSame(source, fixture.model.getActiveChart());
      assertTrue(isActive(source));
      assertFalse(isActive(created));
      assertEquals(Color.web("#a1a1a1"), sourceContainer.getBackground().getFills().getFirst().getFill());
      assertEquals(Color.WHITE, createdContainer.getBackground().getFills().getFirst().getFill());
      assertEquals(1, chartPresses.get());

      themeManager.setTheme(AppTheme.DARK);
      view.applyCss();

      assertEquals(Color.web("#262626"), view.getBackground().getFills().getFirst().getFill());
      assertEquals(Color.web("#737373"), sourceContainer.getBackground().getFills().getFirst().getFill());
      assertEquals(Color.web("#171717"), sourceContainer.getBackground().getFills().get(1).getFill());
      assertEquals(Color.web("#171717"), createdContainer.getBackground().getFills().getFirst().getFill());

      fixture.interactor.remove(created);
      layout(view);

      assertSame(source, fixture.model.getActiveChart());
      assertFalse(isActive(source));
      fixture.interactor.close();
    });
  }

  @Test
  void clonesCurrentChartStateAndThenKeepsChartsIndependent() {
    FxTestSupport.runAndWait(() -> {
      Fixture fixture = new Fixture();
      Chart source = fixture.initialize();
      source.setInstrument("AAPL", "Apple", List.of(), Optional.empty());
      source.setChartType(ChartType.BAR);

      fixture.interactor.split(source, ChartSplitDirection.RIGHT);

      assertEquals(new ChartWorkspaceSettings("AAPL", ChartInterval.DAILY, ChartType.BAR), fixture.settings.get(1));
      ChartWorkspaceSplit split = (ChartWorkspaceSplit) fixture.model.getRoot();
      Chart created = ((ChartWorkspaceLeaf) split.second()).chart();
      created.setInstrument("MSFT", "Microsoft", List.of(), Optional.empty());
      created.setChartType(ChartType.AREA);
      assertEquals("AAPL", source.getSymbol());
      assertEquals(ChartType.BAR, source.getChartType());
      assertEquals("MSFT", created.getSymbol());
      assertEquals(ChartType.AREA, created.getChartType());
      fixture.interactor.close();
    });
  }

  @Test
  void nestsOnlyTheSelectedPaneAndPreservesExistingDividerPositions() {
    FxTestSupport.runAndWait(() -> {
      Fixture fixture = new Fixture();
      Chart source = fixture.initialize();
      ChartWorkspaceViewBuilder viewBuilder = new ChartWorkspaceViewBuilder(
        fixture.model,
        fixture.interactor::activate
      );
      StackPane view = viewBuilder.build();
      new AppThemeManager(new Scene(view, 1_000.0, 600.0), AppTheme.LIGHT);

      fixture.interactor.split(source, ChartSplitDirection.RIGHT);
      ResizablePanelGroup firstView = assertInstanceOf(ResizablePanelGroup.class, view.getChildren().getFirst());
      layout(view);
      ResizableHandle firstHandle = assertInstanceOf(ResizableHandle.class, firstView.getChildren().get(1));
      firstHandle.executeAccessibleAction(AccessibleAction.SET_VALUE, 70.0);
      layout(view);
      ChartWorkspaceSplit firstSplit = (ChartWorkspaceSplit) fixture.model.getRoot();
      assertEquals(0.7, firstSplit.dividerPosition(), 0.01);
      Chart right = ((ChartWorkspaceLeaf) firstSplit.second()).chart();
      assertEquals(8.0, assertInstanceOf(Rectangle.class, source.getView().getClip()).getArcWidth());

      fixture.interactor.split(right, ChartSplitDirection.BOTTOM);

      assertEquals(8.0, assertInstanceOf(Rectangle.class, source.getView().getClip()).getArcWidth());
      assertEquals(8.0, assertInstanceOf(Rectangle.class, right.getView().getClip()).getArcWidth());

      ChartWorkspaceSplit root = assertInstanceOf(ChartWorkspaceSplit.class, fixture.model.getRoot());
      assertEquals(Orientation.HORIZONTAL, root.orientation());
      assertEquals(0.7, root.dividerPosition());
      ChartWorkspaceSplit nested = assertInstanceOf(ChartWorkspaceSplit.class, root.second());
      assertEquals(Orientation.VERTICAL, nested.orientation());
      assertEquals(3, fixture.charts.size());
      Chart bottom = assertInstanceOf(ChartWorkspaceLeaf.class, nested.second()).chart();
      ResizablePanelGroup rebuilt = assertInstanceOf(ResizablePanelGroup.class, view.getChildren().getFirst());
      assertEquals(Orientation.HORIZONTAL, rebuilt.getOrientation());
      assertEquals(0.7, rebuilt.getDividerPositions()[0]);
      ResizablePanel second = assertInstanceOf(ResizablePanel.class, rebuilt.getChildren().get(2));
      ResizablePanelGroup nestedView = assertInstanceOf(ResizablePanelGroup.class, second.getChildren().getFirst());
      assertEquals(Orientation.VERTICAL, nestedView.getOrientation());

      layout(view);
      Bounds sourceBounds = containerBounds(source);
      Bounds rightBounds = containerBounds(right);
      Bounds bottomBounds = containerBounds(bottom);
      assertEquals(4.0, sourceBounds.getMinX(), 0.75);
      assertEquals(4.0, sourceBounds.getMinY(), 0.75);
      assertEquals(4.0, rightBounds.getMinX() - sourceBounds.getMaxX(), 0.75);
      assertEquals(4.0, bottomBounds.getMinY() - rightBounds.getMaxY(), 0.75);
      assertEquals(4.0, view.getWidth() - bottomBounds.getMaxX(), 0.75);
      assertEquals(4.0, view.getHeight() - bottomBounds.getMaxY(), 0.75);
      fixture.interactor.close();
    });
  }

  @Test
  void removesPanesCollapsesTheirParentAndNeverRemovesTheLastChart() {
    FxTestSupport.runAndWait(() -> {
      Fixture fixture = new Fixture();
      Chart source = fixture.initialize();
      assertTrue(source.getView().lookup(".chart-menu-close") == null);

      fixture.interactor.split(source, ChartSplitDirection.RIGHT);
      ChartWorkspaceSplit split = (ChartWorkspaceSplit) fixture.model.getRoot();
      Chart created = ((ChartWorkspaceLeaf) split.second()).chart();
      assertTrue(source.getView().lookup(".chart-menu-close") != null);
      assertTrue(created.getView().lookup(".chart-menu-close") != null);

      fixture.interactor.remove(created);

      assertSame(source, assertInstanceOf(ChartWorkspaceLeaf.class, fixture.model.getRoot()).chart());
      assertSame(source, fixture.model.getActiveChart());
      assertTrue(source.getView().lookup(".chart-menu-close") == null);
      assertEquals(1, fixture.resources.get(1).marketData.closes);
      assertEquals(1, fixture.resources.get(1).logos.closes);

      fixture.interactor.remove(source);
      assertSame(source, ((ChartWorkspaceLeaf) fixture.model.getRoot()).chart());
      fixture.interactor.close();
      assertEquals(1, fixture.resources.getFirst().marketData.closes);
      assertEquals(1, fixture.resources.getFirst().logos.closes);
    });
  }

  @Test
  void activatesThePromotedSiblingWhenTheActiveChartCloses() {
    FxTestSupport.runAndWait(() -> {
      Fixture fixture = new Fixture();
      Chart left = fixture.initialize();
      fixture.interactor.split(left, ChartSplitDirection.RIGHT);
      ChartWorkspaceSplit firstSplit = (ChartWorkspaceSplit) fixture.model.getRoot();
      Chart right = ((ChartWorkspaceLeaf) firstSplit.second()).chart();
      fixture.interactor.split(right, ChartSplitDirection.BOTTOM);
      ChartWorkspaceSplit root = (ChartWorkspaceSplit) fixture.model.getRoot();
      Chart bottom = ((ChartWorkspaceLeaf) ((ChartWorkspaceSplit) root.second()).second()).chart();

      fixture.interactor.remove(bottom);
      assertSame(right, fixture.model.getActiveChart());

      fixture.interactor.activate(left);
      fixture.interactor.remove(left);
      assertSame(right, fixture.model.getActiveChart());
      fixture.interactor.close();
    });
  }

  @Test
  void showsStableDistinctIdentifiersOnlyWhileTheWorkspaceHasMultipleCharts() {
    FxTestSupport.runAndWait(() -> {
      Fixture fixture = new Fixture();
      Chart source = fixture.initialize();
      Region sourceIdentifier = identifier(source);
      Color sourceColor = identifierColor(sourceIdentifier);

      assertFalse(sourceIdentifier.isVisible());
      assertFalse(sourceIdentifier.isManaged());
      assertEquals(0.72, sourceColor.getSaturation(), 0.001);
      assertEquals(0.85, sourceColor.getBrightness(), 0.001);

      fixture.interactor.split(source, ChartSplitDirection.RIGHT);
      ChartWorkspaceSplit split = (ChartWorkspaceSplit) fixture.model.getRoot();
      Chart created = ((ChartWorkspaceLeaf) split.second()).chart();
      Region createdIdentifier = identifier(created);

      assertTrue(sourceIdentifier.isVisible());
      assertTrue(sourceIdentifier.isManaged());
      assertTrue(createdIdentifier.isVisible());
      assertTrue(createdIdentifier.isManaged());
      assertEquals(sourceColor, identifierColor(sourceIdentifier));
      assertNotEquals(identifierColor(sourceIdentifier), identifierColor(createdIdentifier));

      fixture.interactor.remove(created);

      assertFalse(sourceIdentifier.isVisible());
      assertFalse(sourceIdentifier.isManaged());
      assertEquals(sourceColor, identifierColor(sourceIdentifier));
      fixture.interactor.close();
    });
  }

  @Test
  void leavesTheTreeUnchangedWhenChartCreationFails() {
    FxTestSupport.runAndWait(() -> {
      ChartWorkspaceModel model = new ChartWorkspaceModel();
      int[] calls = { 0 };
      ChartWorkspaceInteractor interactor = new ChartWorkspaceInteractor(model, settings -> {
        if (++calls[0] > 1) {
          throw new IllegalStateException("creation failed");
        }
        return chart(settings, new StubMarketData(), new StubLogos());
      });
      interactor.initialize(new ChartWorkspaceSettings("IBM", ChartInterval.DAILY, ChartType.LINE));
      ChartWorkspaceItem original = model.getRoot();
      Chart source = ((ChartWorkspaceLeaf) original).chart();

      interactor.split(source, ChartSplitDirection.LEFT);
      assertSame(original, model.getRoot());
      assertSame(source, model.getActiveChart());
      interactor.close();
    });
  }

  private static Chart chart(ChartWorkspaceSettings settings, StubMarketData marketData, StubLogos logos) {
    Chart chart = new Chart(
      List.of(),
      settings.symbol(),
      settings.interval(),
      new StubInstrumentCatalog(List::of),
      marketData,
      logos,
      Runnable::run
    );
    chart.setChartType(settings.chartType());
    return chart;
  }

  private static void layout(StackPane view) {
    view.resize(1_000.0, 600.0);
    view.applyCss();
    view.layout();
  }

  private static Region identifier(Chart chart) {
    return assertInstanceOf(Region.class, chart.getView().lookup(".chart-identifier"));
  }

  private static Color identifierColor(Region identifier) {
    return assertInstanceOf(Color.class, identifier.getBackground().getFills().getFirst().getFill());
  }

  private static Bounds containerBounds(Chart chart) {
    Region container = assertInstanceOf(Region.class, chart.getView().getParent());
    return container.localToScene(container.getBoundsInLocal());
  }

  private static boolean isActive(Chart chart) {
    return chart.getView().getParent().getPseudoClassStates().contains(ACTIVE_PSEUDO_CLASS);
  }

  private static MouseEvent primaryMousePress() {
    return new MouseEvent(
      MouseEvent.MOUSE_PRESSED,
      1.0,
      1.0,
      1.0,
      1.0,
      MouseButton.PRIMARY,
      1,
      false,
      false,
      false,
      false,
      true,
      false,
      false,
      false,
      false,
      true,
      null
    );
  }

  private static final class Fixture {

    private final ChartWorkspaceModel model = new ChartWorkspaceModel();
    private final List<ChartWorkspaceSettings> settings = new ArrayList<>();
    private final List<Resources> resources = new ArrayList<>();
    private final List<Chart> charts = new ArrayList<>();
    private final ChartWorkspaceInteractor interactor = new ChartWorkspaceInteractor(
      model,
      value -> {
        StubMarketData marketData = new StubMarketData();
        StubLogos logos = new StubLogos();
        Chart chart = chart(value, marketData, logos);
        settings.add(value);
        resources.add(new Resources(marketData, logos));
        charts.add(chart);
        return chart;
      },
      new Random(1_234L)
    );

    private Chart initialize() {
      interactor.initialize(new ChartWorkspaceSettings("IBM", ChartInterval.DAILY, ChartType.LINE));
      return ((ChartWorkspaceLeaf) model.getRoot()).chart();
    }
  }

  private record Resources(StubMarketData marketData, StubLogos logos) {}

  private static final class StubMarketData implements MarketDataSession {

    private int closes;

    @Override
    public CompletableFuture<InstrumentLoadResult> loadInitial() {
      return new CompletableFuture<>();
    }

    @Override
    public CompletableFuture<InstrumentLoadResult> loadInstrument(String symbol) {
      return new CompletableFuture<>();
    }

    @Override
    public CompletableFuture<List<CalendarData>> loadEarlier() {
      return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public void close() {
      closes++;
    }
  }

  private static final class StubLogos implements LogoSession {

    private int closes;

    @Override
    public CompletableFuture<Optional<byte[]>> load(InstrumentLogo logo) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public void close() {
      closes++;
    }
  }
}
