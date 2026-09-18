package com.acteque.terminal.hotreload;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.OnResourceFileEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.command.ReflectionCommand;
import org.hotswap.agent.config.PluginManager;

@Plugin(
  name = "JavaFX view refresh",
  description = "Refreshes live application views after bytecode is redefined.",
  testedVersions = "2.0.3"
)
public final class JavaFxRedrawPlugin {

  private static final String SUPPORT_CLASS = "com.acteque.terminal.HotReloadSupport";

  @Init
  public static void initialize(PluginManager pluginManager, ClassLoader applicationClassLoader) {
    if (applicationClassLoader != null) {
      pluginManager.getPluginRegistry().initializePlugin(JavaFxRedrawPlugin.class.getName(), applicationClassLoader);
    }
  }

  @OnClassLoadEvent(classNameRegexp = "com[./]acteque[./]terminal[./](?!hotreload[./]).*", events = LoadEvent.REDEFINE)
  public static void refreshViews(ClassLoader applicationClassLoader) {
    scheduleRefresh(applicationClassLoader, "refreshViewsAfterReload");
  }

  @OnResourceFileEvent(
    path = "com/acteque/terminal",
    filter = ".*\\.css$",
    events = { FileEvent.CREATE, FileEvent.MODIFY },
    timeout = 150
  )
  public void refreshStylesheets(ClassLoader applicationClassLoader) {
    scheduleRefresh(applicationClassLoader, "refreshStylesheetsAfterReload");
  }

  private static void scheduleRefresh(ClassLoader applicationClassLoader, String methodName) {
    PluginManager.getInstance()
      .getScheduler()
      .scheduleCommand(
        new ReflectionCommand(null, SUPPORT_CLASS, methodName, applicationClassLoader, new Object[0]),
        100
      );
  }
}
