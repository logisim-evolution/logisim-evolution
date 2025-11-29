/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global debug/logging utility for Logisim-evolution.
 * 
 * This is a new feature and not all parts of the program will output
 * log level based information yet.
 * 
 * Log level can be controlled via:
 * - Environment variable: LOGISIM_LOG_LEVEL (e.g., DEBUG, INFO, WARN, ERROR)
 * - System property: logisim.log.level
 * 
 * Default log level is INFO if not specified.
 */
public class Debug {
  
  /**
   * Log levels in order of verbosity (most to least verbose).
   */
  public enum Level {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    CRITICAL(5);
    
    private final int priority;
    
    Level(int priority) {
      this.priority = priority;
    }
    
    public int getPriority() {
      return priority;
    }
    
    /**
     * Returns true if this level is at least as verbose as the other level.
     */
    public boolean isAtLeast(Level other) {
      return this.priority <= other.priority;
    }
  }
  
  private static final Logger logger = LoggerFactory.getLogger(Debug.class);
  private static final Debug instance = new Debug();
  private final Level currentLevel;
  
  private Debug() {
    // Read log level from environment variable or system property
    String levelStr = System.getenv("LOGISIM_LOG_LEVEL");
    if (levelStr == null || levelStr.isEmpty()) {
      levelStr = System.getProperty("logisim.log.level");
    }
    
    // Default to INFO if not specified or invalid
    Level level = Level.INFO;
    if (levelStr != null && !levelStr.isEmpty()) {
      try {
        level = Level.valueOf(levelStr.toUpperCase());
      } catch (IllegalArgumentException e) {
        // Invalid level, use default
        System.err.println("Warning: Invalid log level '" + levelStr + "', using INFO");
      }
    }
    
    this.currentLevel = level;
  }
  
  /**
   * Get the singleton Debug instance.
   */
  public static Debug func() {
    return instance;
  }
  
  /**
   * Get the current log level.
   */
  public Level getLevel() {
    return currentLevel;
  }
  
  /**
   * Check if the current log level is at least as verbose as the given level.
   * Returns true if messages at the given level should be logged.
   * 
   * @param level The level to check
   * @return true if the current level is at least as verbose as the given level
   */
  public static boolean isLevel(Level level) {
    return instance.currentLevel.isAtLeast(level);
  }
  
  /**
   * Log a message at the specified level.
   * The message will only be logged if the current log level is at least as verbose
   * as the specified level.
   * 
   * @param level The log level
   * @param message The message to log
   */
  public static void log(Level level, String message) {
    if (!isLevel(level)) {
      return;
    }
    
    // Also print to System.out for immediate visibility, especially for DEBUG level
    // This ensures messages are visible even if SLF4J logging isn't configured properly
    if (level == Level.DEBUG || level == Level.TRACE) {
      System.out.println("[DEBUG] " + message);
    }
    
    switch (level) {
      case TRACE:
        logger.trace(message);
        break;
      case DEBUG:
        logger.debug(message);
        break;
      case INFO:
        logger.info(message);
        break;
      case WARN:
        logger.warn(message);
        break;
      case ERROR:
        logger.error(message);
        break;
      case CRITICAL:
        logger.error("[CRITICAL] " + message);
        break;
    }
  }
  
  /**
   * Log a message with format string and arguments at the specified level.
   * Supports both SLF4J-style {} placeholders and Java String.format %s/%d style.
   * 
   * @param level The log level
   * @param format The format string (can use {} or %s/%d style)
   * @param args The arguments for the format string
   */
  public static void log(Level level, String format, Object... args) {
    if (!isLevel(level)) {
      return;
    }
    
    String message;
    // Check if format uses SLF4J-style {} placeholders
    if (format.contains("{}") && args.length > 0) {
      // Convert {} to appropriate format specifiers based on argument types
      StringBuilder javaFormat = new StringBuilder();
      int argIndex = 0;
      int formatIndex = 0;
      
      while (formatIndex < format.length()) {
        if (formatIndex < format.length() - 1 && 
            format.charAt(formatIndex) == '{' && 
            format.charAt(formatIndex + 1) == '}') {
          // Replace {} with appropriate format specifier
          if (argIndex < args.length) {
            Object arg = args[argIndex];
            if (arg instanceof Integer || arg instanceof Long || 
                arg instanceof Byte || arg instanceof Short) {
              javaFormat.append("%d");
            } else if (arg instanceof Double || arg instanceof Float) {
              javaFormat.append("%f");
            } else {
              javaFormat.append("%s");
            }
            argIndex++;
          } else {
            javaFormat.append("{}"); // Keep if no more args
          }
          formatIndex += 2;
        } else {
          javaFormat.append(format.charAt(formatIndex));
          formatIndex++;
        }
      }
      
      // Only use String.format if we have arguments
      if (argIndex > 0 && argIndex <= args.length) {
        try {
          message = String.format(javaFormat.toString(), args);
        } catch (Exception e) {
          // Fallback: just use the format string as-is
          message = format + " [format error: " + e.getMessage() + "]";
        }
      } else {
        message = format;
      }
    } else {
      // Use as-is (already Java format or no placeholders)
      if (args.length > 0) {
        try {
          message = String.format(format, args);
        } catch (Exception e) {
          message = format + " [format error: " + e.getMessage() + "]";
        }
      } else {
        message = format;
      }
    }
    log(level, message);
  }
  
  /**
   * Print startup message with current log level.
   * This should be called early in the application startup.
   */
  public static void printStartupMessage() {
    System.out.println("Logisim-evolution Debug/Log System");
    System.out.println("Current log level: " + instance.currentLevel);
    System.out.println("(This is a new feature - not all parts of the program output log level based info yet)");
    System.out.println("Set LOGISIM_LOG_LEVEL environment variable or -Dlogisim.log.level system property to change");
    System.out.println("Valid levels: TRACE, DEBUG, INFO, WARN, ERROR, CRITICAL");
    System.out.println();
  }
}

