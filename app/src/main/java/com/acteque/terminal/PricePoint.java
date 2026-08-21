package com.acteque.terminal;

import java.time.LocalDate;

record PricePoint(LocalDate date, double open, double high, double low, double close, long volume) {

  double price() {
    return close;
  }
}
