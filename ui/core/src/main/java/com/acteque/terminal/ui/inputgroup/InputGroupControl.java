package com.acteque.terminal.ui.inputgroup;

import javafx.beans.property.BooleanProperty;

interface InputGroupControl {
  boolean isInvalid();

  BooleanProperty invalidProperty();
}
