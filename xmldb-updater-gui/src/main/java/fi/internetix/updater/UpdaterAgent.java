package fi.internetix.updater;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

public class UpdaterAgent {

  private static Instrumentation inst = null;

  public static void agentmain(final String a, final Instrumentation inst) {
    UpdaterAgent.inst = inst;
  }

  public static boolean addClassPath(File f) {
    ClassLoader cl = ClassLoader.getSystemClassLoader();

    try {
      // If Java 9 or higher use Instrumentation
      if (!(cl instanceof URLClassLoader)) {
        inst.appendToSystemClassLoaderSearch(new JarFile(f));
        return false;
      }

      // If Java 8 or below fallback to old method
      Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
      m.setAccessible(true);
      m.invoke(cl, (Object) f.toURI().toURL());
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return false;
  }

}