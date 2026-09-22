package com.acteque.terminal.chart;

import com.acteque.terminal.marketdata.CalendarInterval;
import com.acteque.terminal.marketdata.HistoricalInterval;
import java.time.Duration;
import java.util.Optional;

/** Maps selectable chart intervals to the provider-neutral historical requests. */
public final class ChartIntervalHistoryMapper {

  private ChartIntervalHistoryMapper() {}

  /**
   * Maps a chart selection to a directly requestable historical interval.
   *
   * @param interval the selected chart interval
   * @return the matching historical interval, or empty when no direct request exists
   */
  public static Optional<HistoricalInterval> map(ChartInterval interval) {
    return switch (interval.classification()) {
      case MINUTES -> Optional.of(new HistoricalInterval.Intraday(Duration.ofMinutes(interval.amount())));
      case HOURS -> Optional.of(new HistoricalInterval.Intraday(Duration.ofHours(interval.amount())));
      case DAYS -> interval.amount() == 1
        ? Optional.of(new HistoricalInterval.Calendar(CalendarInterval.DAILY))
        : Optional.empty();
      case WEEKS -> interval.amount() == 1
        ? Optional.of(new HistoricalInterval.Calendar(CalendarInterval.WEEKLY))
        : Optional.empty();
      case MONTHS -> switch (interval.amount()) {
        case 1 -> Optional.of(new HistoricalInterval.Calendar(CalendarInterval.MONTHLY));
        case 12 -> Optional.of(new HistoricalInterval.Calendar(CalendarInterval.YEARLY));
        default -> Optional.empty();
      };
      default -> Optional.empty();
    };
  }
}
