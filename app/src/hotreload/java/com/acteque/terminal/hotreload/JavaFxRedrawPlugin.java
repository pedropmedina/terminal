package com.acteque.terminal.hotreload;

import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.command.ReflectionCommand;
import org.hotswap.agent.config.PluginManager;

@Plugin(
    name = "JavaFX chart redraw",
    description = "Redraws live application charts after bytecode is redefined.",
    testedVersions = "2.0.3")
public final class JavaFxRedrawPlugin {

  private static final String SUPPORT_CLASS = "com.acteque.terminal.HotReloadSupport";

  @OnClassLoadEvent(
      classNameRegexp = "com[./]acteque[./]terminal[./](?!hotreload[./]).*",
      events = LoadEvent.REDEFINE)
  public static void redrawCharts(ClassLoader applicationClassLoader) {
    PluginManager.getInstance()
        .getScheduler()
        .scheduleCommand(
            new ReflectionCommand(
                null,
                SUPPORT_CLASS,
                "redrawChartsAfterReload",
                applicationClassLoader,
                new Object[0]),
            100);
  }
}
