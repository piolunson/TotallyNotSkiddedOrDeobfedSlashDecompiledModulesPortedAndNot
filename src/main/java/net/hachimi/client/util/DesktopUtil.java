package net.hachimi.client.util;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class DesktopUtil {
   public static boolean browse(URI uri) {
      if (openSystemSpecific(uri.toString())) {
         return true;
      } else {
         return browseDesktop(uri);
      }
   }

   public static boolean open(File file) {
      if (openSystemSpecific(file.getPath())) {
         return true;
      } else {
         return openDesktop(file);
      }
   }

   public static boolean edit(File file) {
      if (openSystemSpecific(file.getPath())) {
         return true;
      } else {
         return editDesktop(file);
      }
   }

   private static boolean openSystemSpecific(String what) {
      EnumOS os = getOs();
      if (os.isLinux()) {
         if (runCommand("kde-open", "%s", what)) {
            return true;
         }

         if (runCommand("gnome-open", "%s", what)) {
            return true;
         }

         if (runCommand("xdg-open", "%s", what)) {
            return true;
         }
      }

      if (os.isMac() && runCommand("open", "%s", what)) {
         return true;
      } else {
         return os.isWindows() && runCommand("explorer", "%s", what);
      }
   }

   private static boolean browseDesktop(URI uri) {
      try {
         if (!Desktop.isDesktopSupported()) {
            return false;
         } else if (!Desktop.getDesktop().isSupported(Action.BROWSE)) {
            return false;
         } else {
            Desktop.getDesktop().browse(uri);
            return true;
         }
      } catch (Throwable var2) {
         return false;
      }
   }

   private static boolean openDesktop(File file) {
      try {
         if (!Desktop.isDesktopSupported()) {
            return false;
         } else if (!Desktop.getDesktop().isSupported(Action.OPEN)) {
            return false;
         } else {
            Desktop.getDesktop().open(file);
            return true;
         }
      } catch (Throwable var2) {
         return false;
      }
   }

   private static boolean editDesktop(File file) {
      try {
         if (!Desktop.isDesktopSupported()) {
            return false;
         } else if (!Desktop.getDesktop().isSupported(Action.EDIT)) {
            return false;
         } else {
            Desktop.getDesktop().edit(file);
            return true;
         }
      } catch (Throwable t) {
         t.printStackTrace();
         return false;
      }
   }

   private static boolean runCommand(String command, String args, String file) {
      String[] parts = prepareCommand(command, args, file);

      try {
         Process p = Runtime.getRuntime().exec(parts);
         if (p == null) {
            return false;
         } else {
            try {
               int retval = p.exitValue();
               return retval == 0 ? false : false;
            } catch (IllegalThreadStateException var6) {
               return true;
            }
         }
      } catch (IOException var7) {
         return false;
      }
   }

   private static String[] prepareCommand(String command, String args, String file) {
      List<String> parts = new ArrayList<>();
      parts.add(command);
      if (args != null) {
         for(String s : args.split(" ")) {
            s = String.format(s, file);
            parts.add(s.trim());
         }
      }

      return (String[])parts.toArray(new String[parts.size()]);
   }

   public static EnumOS getOs() {
      String s = System.getProperty("os.name").toLowerCase();
      if (s.contains("win")) {
         return DesktopUtil.EnumOS.WINDOWS;
      } else if (s.contains("mac")) {
         return DesktopUtil.EnumOS.MAC_OS;
      } else if (s.contains("solaris")) {
         return DesktopUtil.EnumOS.LINUX_SOLARIS;
      } else if (s.contains("sunos")) {
         return DesktopUtil.EnumOS.LINUX_SOLARIS;
      } else if (s.contains("linux")) {
         return DesktopUtil.EnumOS.LINUX;
      } else {
         return s.contains("unix") ? DesktopUtil.EnumOS.LINUX : DesktopUtil.EnumOS.OTHER;
      }
   }

   public static enum EnumOS {
      WINDOWS,
      LINUX,
      MAC_OS,
      LINUX_SOLARIS,
      OTHER;

      public boolean isLinux() {
         return this == LINUX || this == LINUX_SOLARIS;
      }

      public boolean isMac() {
         return this == MAC_OS;
      }

      public boolean isWindows() {
         return this == WINDOWS;
      }

      // $FF: synthetic method
      private static EnumOS[] $values() {
         return new EnumOS[]{WINDOWS, LINUX, MAC_OS, LINUX_SOLARIS, OTHER};
      }
   }
}
