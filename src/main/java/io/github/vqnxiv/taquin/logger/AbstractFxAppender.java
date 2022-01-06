package io.github.vqnxiv.taquin.logger;


import io.github.vqnxiv.taquin.controller.MainController;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Base appender class which displays logging events on the GUI.
 * 
 * @see AbstractAppender
 * @see MainAppender
 * @see SearchAppender
 */
public abstract class AbstractFxAppender extends AbstractAppender {

    /**
     * {@link Record} which is used to keep a {@link LogEvent} message
     * with the {@link TextArea} it should be displayed in.
     */
    private record Pair(String text, TextArea output) {}

    
    /**
     * {@link MainController} from which the {@link Pair#output} are retrieved.
     */
    protected static MainController mainController;

    /**
     * {@link AtomicBoolean} which is used to throttle the display of events
     * so that the JFX thread doesn't get swarmed with updates.
     * <p>
     * For example, logging a search fires 4 to 6 events per explored state,
     * aka {@code step()} calls. So for a small search of 1.500 explored states,
     * which should be completed in about 30ms to 100ms depending on the collections
     * and warmup, this ranges from 60 (4 events, 100ms) to 300 (6 events, 30ms)
     * {@link Platform#runLater(Runnable)} calls <u>per ms</u>.
     */
    private final AtomicBoolean throttle;

    /**
     * {@link Queue} buffer to store the events until the JFX thread could add them 
     * to their output {@link TextArea}.
     */
    private final Queue<Pair> eventBuffer;

    
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
    protected AbstractFxAppender(String name, Filter filter, Layout<? extends Serializable> layout, 
                                 boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        
        throttle = new AtomicBoolean(true);
        eventBuffer = new ArrayDeque<>();
    }

    /**
     * Setter for {@link #mainController}.
     * 
     * @param mainController the {@link MainController} for this class.
     */
    public static void injectMainController(MainController mainController) {
        AbstractFxAppender.mainController = mainController;
    }


    /**
     * Method which is called to add an event to the GUI. Extending classes
     * should call this in {@link #append(LogEvent)}.
     * 
     * @param event The event to display.
     * @param output Where it should be displayed.
     */
    protected void enqueueForGui(LogEvent event, TextArea output) {
        
        if(event == null || output == null) {
            return;
        }
        
        synchronized(eventBuffer) {
            /*
            getLayout().toByteArray() returns an empty string when logging from search
            and .getMessage().getFormattedMessage() removes parts of the pattern
            so we keep the layout way for the root logger
            */
            String s = new String(getLayout().toByteArray(event));
            
            if(s.isBlank()) {
                eventBuffer.add(new Pair(event.getMessage().getFormattedMessage() + '\n', output));
            }
            else {
                eventBuffer.add(new Pair(s, output));
            }
        }

        tryProcessEvent();
    }

    /**
     * Callback method to notify this appender the GUI is done displaying
     * the event sent through {@link #addFirstToGui()} and {@link Platform#runLater(Runnable)},
     * and it can attempt to queue the next event, if {@link #eventBuffer} is not empty.
     */
    private void notifyForNext() {
        tryProcessEvent();
    }

    /**
     * Attempts to process the next event in {@link #eventBuffer}. 
     * Queues a GUI update through {@link Platform#runLater(Runnable)} if 
     * {@link #eventBuffer} is not empty and {@link #throttle} is {@code true}.
     */
    private void tryProcessEvent() {
        if(eventBuffer.isEmpty()) {
            return;
        }

        if(throttle.getAndSet(false)) {
            Platform.runLater(this::addFirstToGui);
        }
    }

    /**
     * Updates the content of a {@link Pair#output} with {@link Pair#text}. 
     * The {@link Pair} is the one which is retrieved from calling {@link Queue#poll()}
     * on {@link #eventBuffer}.
     */
    private void addFirstToGui() {
        var p = eventBuffer.poll();
        p.output().appendText(p.text());
        p.output().setScrollTop(-15);
        throttle.set(true);
        this.notifyForNext();
    }
}