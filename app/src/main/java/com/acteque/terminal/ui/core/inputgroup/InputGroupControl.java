package com.acteque.terminal.ui.core.inputgroup;

import javafx.beans.property.BooleanProperty;

interface InputGroupControl {
  boolean isInvalid();

  BooleanProperty invalidProperty();
}
