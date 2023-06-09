package fi.internetix.updater.cmd;

import java.io.Console;
import java.io.File;
import java.io.FileFilter;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.hibernate.dialect.Dialect;

import fi.internetix.updater.core.Database;
import fi.internetix.updater.core.DatabaseDriverUtils;
import fi.internetix.updater.core.Settings;
import fi.internetix.updater.core.UpdaterAgent;
import fi.internetix.updater.core.UpdaterException;

public class Updater {
  
  private static Logger logger = Logger.getLogger(Updater.class);
  
  private static final int HELP_OPTION_COLUMN_WIDTH = 30;

  public static void main(String[] args) {
    // TODO: Support for outputting sql into stdout
    Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout()));
    
    boolean printHelp = false;
    String helpError = null;
    Map<Option, String> options = null;
    try {
      try {
        options = parseArgs(args);
        printHelp = "true".equals(options.get(Option.HELP));
      } catch (RuntimeException e) {
        helpError = e.getMessage();
        printHelp = true;
      }
    } catch (Exception e) {
      logger.error("Arguments parsing failed", e);
      printHelp = true;
    }
    
    if (printHelp) {
       printCommandLineHelp(helpError);
    } else {
      for (Option requiredOption : REQUIRED_OPTIONS) {
        if (options.get(requiredOption) == null) {
          printCommandLineHelp("Required option " + COMMAND_LINE_ARGUMENTS.get(requiredOption)[0] + " missing");
          System.exit(-1);
        }
      }

      if (options.get(Option.UPDATES_FOLDER) == null) {
        printCommandLineHelp("Updates folder is missing");
        System.exit(-1);
      }

      String driverFolder = options.get(Option.DATABASE_DRIVERS_FOLDER);
      if (!DatabaseDriverUtils.loadDrivers(driverFolder)) {
        System.err.println("Could not find any database drivers");
        System.exit(-1);
      }
      
      File updatesFolder = new File(options.get(Option.UPDATES_FOLDER));
      Class<Dialect> dialect = Database.valueOf(options.get(Option.DATABASE_VENDOR)).getDialect();
      String databaseUrl = options.get(Option.DATABASE_URL);
      String databaseUsername = options.get(Option.DATABASE_USERNAME);
      String databasePassword = options.get(Option.DATABASE_PASSWORD);
      
      Settings settings = new Settings(updatesFolder, dialect, databaseUrl, databaseUsername, databasePassword);
      
      fi.internetix.updater.core.Updater updater = new fi.internetix.updater.core.Updater(settings);

      logger.info("Updater started");
      logger.info("Current database version: " + updater.getCurrentVersion());
      
      List<String> updates = updater.checkForUpdates();
      if (updates.size() > 0) {
        boolean proceed = false;
        
        if ("true".equals(options.get(Option.FORCE))) {
          proceed = true;
        } else {
          System.out.println("Do you wish to proceed [Y/n]?");
          Console console = System.console();
          String answer = console.readLine();
          proceed = StringUtils.isNotBlank(answer) ? "y".equalsIgnoreCase(answer) : true;
        }

        // TODO: Support for simulation mode
        if (proceed) {
          updater.performUpgrade(true);
        } else {
          System.exit(0);
        }
      }
    }
  }

  private static Map<Option, String> parseArgs(String[] args) {
    // TODO: Is there a generic Java way to do this?
    Map<Option, String> options = new HashMap<Updater.Option, String>();
    
    for (int i = 0, l = args.length - 1; i < l; i++) {
      String arg = args[i];
      
      if (StringUtils.isNotBlank(arg)) {

        Option option = getOption(arg);
        
        if (option == null) {
          throw new RuntimeException("Invalid argument: " + arg);
        }
        
        if (option.isBooleanOption()) {
          options.put(option, "true");
        } else {
          if (i < l) {
            i++;
            String value = args[i];
            if (StringUtils.isBlank(value)||(value.startsWith("-"))) {
              throw new RuntimeException("Argument: " + arg + "requires a value");
            } else {
              options.put(option, value);
            }
          } else {
            throw new RuntimeException("Argument: " + arg + "requires a value");
          }
        }
      }
    }
    
    if (args.length > 0) {
      options.put(Option.UPDATES_FOLDER, args[args.length - 1]);
    }
    
    return options;
  }

  private static void printCommandLineHelp(String error) {
    PrintStream out = System.err;
    
    out.println("java-xmldb-updater - Database independent database updating tool\n");
    
    if (StringUtils.isNotBlank(error)) {
      out.println(error);
      out.println();
    }
    
    out.println("Usage: java-xmldb-updater [options] updates_folder");
    for (Option argument : Option.values()) {
      String[] options = COMMAND_LINE_ARGUMENTS.get(argument);
      if ((options != null) && (options.length > 0)) { 
        StringBuilder commandHelpBuilder = new StringBuilder(" ");
        commandHelpBuilder.append(options[0]);
        
        if (options.length > 1) {
          commandHelpBuilder.append(" (");
          
          for (int i = 1, l = options.length; i < l; i++) {
            if (i > 1) {
              commandHelpBuilder.append(", ");
            }
  
            commandHelpBuilder.append(options[i]);
          }
          
          commandHelpBuilder.append(')');
        }
        
        int pad = HELP_OPTION_COLUMN_WIDTH - commandHelpBuilder.length();
        while (pad >= 0) {
          commandHelpBuilder.append(' ');
          pad--;
        }
        
        commandHelpBuilder.append(COMMAND_LINE_ARGUMENTS_HELP.get(argument));
        
        out.println(commandHelpBuilder.toString());
      }
    }
  }

  private static Option getOption(String optionName) {
    for (Option option : COMMAND_LINE_ARGUMENTS.keySet()) {
      String[] names = COMMAND_LINE_ARGUMENTS.get(option);
      for (String name : names) {
        if (name.equals(optionName))
          return option;
      }
    }
    
    return null;
  }
  
  private static enum Option {
    HELP (true),
    FORCE (true),
    UPDATES_FOLDER (false),
    DATABASE_DRIVERS_FOLDER (false),
    DATABASE_URL (false),
    DATABASE_USERNAME (false),
    DATABASE_PASSWORD (false),
    DATABASE_VENDOR (false);
    
    private Option(boolean booleanOption) {
      this.booleanOption = booleanOption;
    }
    
    public boolean isBooleanOption() {
      return booleanOption;
    }
    
    private boolean booleanOption;
  }
  
  private static final Map<Option, String[]> COMMAND_LINE_ARGUMENTS = new HashMap<Option, String[]>();
  private static final Map<Option, String> COMMAND_LINE_ARGUMENTS_HELP = new HashMap<Option, String>();
  private static final Option[] REQUIRED_OPTIONS = new Option[]{
    Option.DATABASE_DRIVERS_FOLDER,
    Option.DATABASE_URL,
    Option.DATABASE_USERNAME,
    Option.DATABASE_PASSWORD,
    Option.DATABASE_VENDOR
  };
  
  private static void addArgument(Option argument, String... options) {
    COMMAND_LINE_ARGUMENTS.put(argument, options);
  }

  static {
    // Arguments
    
    addArgument(Option.HELP, "--help", "-?");
    addArgument(Option.FORCE, "--force", "-f");
    addArgument(Option.DATABASE_DRIVERS_FOLDER, "--databaseDriversFolder", "-d");
    addArgument(Option.DATABASE_VENDOR, "--databaseVendor", "-m");
    addArgument(Option.DATABASE_URL, "--databaseUrl", "-h");
    addArgument(Option.DATABASE_USERNAME, "--databaseUsername", "-u");
    addArgument(Option.DATABASE_PASSWORD, "--databasePassword", "-p");
    
    // Help
    
    StringBuilder vendorPadBuilder = new StringBuilder();
    for (int i = 0; i <= HELP_OPTION_COLUMN_WIDTH; i++) {
      vendorPadBuilder.append(' ');
    }
    
    StringBuilder vendorsHelpBuilder = new StringBuilder();
    for (Database database : Database.values()) {
      if (database.getDialect() != null) {
        vendorsHelpBuilder.append(vendorPadBuilder);
        vendorsHelpBuilder.append(database.name() + " for " + database.toString() + "\n");
      }
    }
    
    COMMAND_LINE_ARGUMENTS_HELP.put(Option.HELP, "Prints this help.");
    COMMAND_LINE_ARGUMENTS_HELP.put(Option.FORCE, "Ignores questions.");
    COMMAND_LINE_ARGUMENTS_HELP.put(Option.DATABASE_DRIVERS_FOLDER, "Path to database driver jars folder."); 
    COMMAND_LINE_ARGUMENTS_HELP.put(Option.UPDATES_FOLDER, "Folder containing updates.");
    COMMAND_LINE_ARGUMENTS_HELP.put(Option.DATABASE_URL, "Database URL.");
    COMMAND_LINE_ARGUMENTS_HELP.put(Option.DATABASE_USERNAME, "Database username.");
    COMMAND_LINE_ARGUMENTS_HELP.put(Option.DATABASE_PASSWORD, "Database password.");
    COMMAND_LINE_ARGUMENTS_HELP.put(Option.DATABASE_VENDOR, "Database vendor.\n" + vendorsHelpBuilder.toString());
  }
}
