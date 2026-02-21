package com.autoparkour.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ModLogger {

    private final Logger logger;
    private final String modId;
    private boolean debugEnabled = false;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public ModLogger(String modId) {
        this.modId = modId;
        this.logger = LoggerFactory.getLogger(modId);
    }

    public void info(String message) {
        logger.info(formatMessage(message));
    }

    public void warn(String message) {
        logger.warn(formatMessage(message));
    }

    public void error(String message) {
        logger.error(formatMessage(message));
    }

    public void error(String message, Throwable throwable) {
        logger.error(formatMessage(message), throwable);
    }

    public void debug(String message) {
        if (debugEnabled) {
            logger.debug(formatMessage(message));
        }
    }

    public void debug(String message, Object... args) {
        if (debugEnabled) {
            logger.debug(formatMessage(String.format(message, args)));
        }
    }

    public void trace(String message) {
        if (debugEnabled) {
            logger.trace(formatMessage(message));
        }
    }

    private String formatMessage(String message) {
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        return String.format("[%s] [%s] %s", time, modId, message);
    }

    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
        info("Debug logging " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    // Логирование производительности
    public void logPerformance(String operation, long startTime) {
        if (debugEnabled) {
            long duration = System.currentTimeMillis() - startTime;
            debug("Performance - {} took {} ms", operation, duration);
        }
    }

    // Логирование с уровнем
    public void log(LogLevel level, String message) {
        switch (level) {
            case INFO:
                info(message);
                break;
            case WARN:
                warn(message);
                break;
            case ERROR:
                error(message);
                break;
            case DEBUG:
                debug(message);
                break;
            case TRACE:
                trace(message);
                break;
        }
    }

    public enum LogLevel {
        INFO,
        WARN,
        ERROR,
        DEBUG,
        TRACE
    }
}
