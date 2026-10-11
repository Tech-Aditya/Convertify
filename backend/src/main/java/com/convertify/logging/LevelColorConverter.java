package com.convertify.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

/**
 * Logback converter ({@code %levelColor(...)}) that colours its content by log level:
 * ERROR bold red, WARN yellow, INFO green, DEBUG cyan, TRACE faint grey.
 * Uses Spring Boot's {@link AnsiOutput}, so colours are emitted only when ANSI output is
 * enabled ({@code spring.output.ansi.enabled}) and never leak into files or plain consoles.
 */
public class LevelColorConverter extends CompositeConverter<ILoggingEvent> {

    @Override
    protected String transform(ILoggingEvent event, String in) {
        Level level = event.getLevel();
        if (level.isGreaterOrEqual(Level.ERROR)) {
            return AnsiOutput.toString(AnsiStyle.BOLD, AnsiColor.RED, in, AnsiStyle.NORMAL);
        }
        if (level.isGreaterOrEqual(Level.WARN)) {
            return AnsiOutput.toString(AnsiColor.YELLOW, in);
        }
        if (level.isGreaterOrEqual(Level.INFO)) {
            return AnsiOutput.toString(AnsiColor.GREEN, in);
        }
        if (level.isGreaterOrEqual(Level.DEBUG)) {
            return AnsiOutput.toString(AnsiColor.CYAN, in);
        }
        return AnsiOutput.toString(AnsiStyle.FAINT, in, AnsiStyle.NORMAL);
    }
}
