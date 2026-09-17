package com.acteque.terminal.ui.icons;

/** Immutable vector geometry used to create a Lucide icon node. */
public interface LucideGlyph {
  String sourceName();

  double width();

  double height();

  String pathData();
}
