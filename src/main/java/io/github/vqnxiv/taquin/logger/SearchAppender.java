package io.github.vqnxiv.taquin.logger;


import io.github.vqnxiv.taquin.controller.BuilderController;
import org.apache.logging.log4j.Marker;
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


/**
 * Log appender for search log.
 *
 * @see AbstractFxAppender
 * @see io.github.vqnxiv.taquin.solver.Search
 */
@Plugin(
    name = "SearchAppender",
    category = "Core",
    elementType = "appender",
    printObject = true
)
public class SearchAppender extends AbstractFxAppender {
    
    /**
     * Factory method.
     * <p>
     * {@inheritDoc}
     *
     * @param name name
     * @param layout layout
     * @param filter filter
     * @return new {@link SearchAppender}
     */
    @PluginFactory
    public static SearchAppender createAppender(
        @PluginAttribute("name") String name,
        @PluginElement("Layout") Layout<? extends Serializable> layout,
        @PluginElement("Filter") final Filter filter
    ) {
        if (name == null) {
            LOGGER.error("No name provided for SearchAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new SearchAppender(name, filter, layout, true, new Property[]{});
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
    protected SearchAppender(String name, Filter filter, Layout<? extends Serializable> layout, 
                             boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }


    /**
     * Appends a log event from a search log to the {@link BuilderController#getLogOutput()} 
     * from the corresponding {@link io.github.vqnxiv.taquin.solver.Search}.
     * <p>
     * The {@link io.github.vqnxiv.taquin.solver.Search} from which the event was fired is retrieved
     * from the event's {@link Marker} ({@link Marker#getName()}).
     *
     * @param event {@link LogEvent} to be appended.
     */
    @Override
    public void append(LogEvent event) {
        event = event.toImmutable();

        int id = Integer.parseInt(event.getMarker().getName());
        
        LogEvent finalEvent = event;
        mainController.getBuilderLogOutputFromSearchID(id).ifPresent(
            output -> enqueueForGui(finalEvent, output)
        );
    }
}
