package com.acteque.terminal.chart;

import java.time.LocalDate;

public record PricePoint(LocalDate date, double open, double high, double low, double close, long volume) {

  double price() {
    return close;
  }
}
