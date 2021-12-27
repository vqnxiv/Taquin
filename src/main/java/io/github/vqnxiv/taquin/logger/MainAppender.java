package io.github.vqnxiv.taquin.logger;


import io.github.vqnxiv.taquin.controller.LogController;
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
import java.util.LinkedList;


@Plugin(
    name = "AppAppender",
    category = "Core",
    elementType = "appender",
    printObject = true
)
public class MainAppender extends AbstractFxAppender {
    
    private static LogController logController;
    
    private final LinkedList<LogEvent> events = new LinkedList<>();
    
    // private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    // private final Lock readLock = rwLock.readLock();

    
    @PluginFactory
    public static MainAppender createAppender(
        @PluginAttribute("name") String name,
        @PluginElement("Layout") Layout<? extends Serializable> layout,
        @PluginElement("Filter") final Filter filter
    ) {
        if (name == null) {
            LOGGER.error("No name provided for TextAreaAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new MainAppender(name, filter, layout, true, new Property[]{});
    }
    
    protected MainAppender(String name, Filter filter, Layout<? extends Serializable> layout, 
                           boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    
    public static void setLogController(LogController logController) {
        MainAppender.logController = logController;
    }
    
    @Override
    public void append(LogEvent event) {
        events.add(event.toImmutable());

        if(logController == null) {
            return;
        }
        
        while(!events.isEmpty()) {
            enqueueForGui(events.poll(), logController.getOutput());
        }
        
        //readLock.lock();
        //readLock.unlock();
    }
}
