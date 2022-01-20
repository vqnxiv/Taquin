package io.github.vqnxiv.taquin.logger;


import io.github.vqnxiv.taquin.controller.MainController;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;


/**
 * The main log appender for the app log.
 * 
 * @see AbstractFxAppender
 */
@Plugin(
    name = "AppAppender",
    category = "Core",
    elementType = "appender",
    printObject = true
)
public class MainAppender extends AbstractFxAppender {

    /**
     * A buffer for events that are logged before {@link #mainController}
     * or {@link MainController#getMainLogOutput()} are initialized.
     */
    private final LinkedList<LogEvent> events = new LinkedList<>();

    
    /**
     * Factory method.
     * <p>
     * {@inheritDoc}
     * 
     * @param name name
     * @param layout layout
     * @param filter filter
     * @return new {@link MainAppender}
     */
    @PluginFactory
    public static MainAppender createAppender(
        @PluginAttribute("name") String name,
        @PluginElement("Layout") Layout<? extends Serializable> layout,
        @PluginElement("Filter") final Filter filter
    ) {
        if (name == null) {
            LOGGER.error("No name provided for MainAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new MainAppender(name, filter, layout, true, new Property[]{});
    }

    /**
     * Constructor.
     * <p>
     * {@inheritDoc}
     *
     * @param name name
     * @param filter filter
     * @param layout layout
     * @param ignoreExceptions boolean
     * @param properties array
     */
    protected MainAppender(String name, Filter filter, Layout<? extends Serializable> layout, 
                           boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }


    /**
     * Appends a log event from the main app log to {@link MainController#getMainLogOutput()}.
     * 
     * @param event {@link LogEvent} to be appended.
     */
    @Override
    public void append(LogEvent event) {
        events.add(event.toImmutable());

        if(mainController == null || mainController.getMainLogOutput() == null) {
            return;
        }
        
        while(!events.isEmpty()) {
            var e = events.poll();
            enqueueForGui(
                new String(getLayout().toByteArray(e), StandardCharsets.UTF_8), 
                mainController.getMainLogOutput()
            );
        }
    }
}
