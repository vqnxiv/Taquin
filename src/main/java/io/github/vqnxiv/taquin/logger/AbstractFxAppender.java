package io.github.vqnxiv.taquin.logger;


import io.github.vqnxiv.taquin.controller.MainController;
import javafx.application.Platform;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
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
     * {@link MainController} from which the {@link InlineCssTextArea} outputs are retrieved.
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
     * Buffers the actual event messages with {@link StringBuilder}s so several messages
     * can be added at once.
     * <p>
     * Buffering the messages before appending them allows to greatly reduce the number
     * of {@link Platform#runLater(Runnable)} calls. E.g in the same conditions as above,
     * (1500 states, 5 events / step = 7500 total events), we go from 7500 
     * {@link InlineCssTextArea#appendText(String)} calls passed to 
     * {@link Platform#runLater(Runnable)} to less than 25 (highest i've seen was 24, 
     * most in the 10-20 range) which allows the 7500 messages to be displayed on the screen 
     * almost instantly.
     * <p>
     * Internally implemented with a {@link LinkedHashMap} so that events that came first
     * are added first to the UI.
     */
    private final Map<InlineCssTextArea, StringBuilder> buffer;
    
    
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
        buffer = new LinkedHashMap<>();
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
     * @param message The event message to display.
     * @param output Where it should be displayed.
     */
    protected void enqueueForGui(String message, InlineCssTextArea output) {
        
        if(message.isBlank() || output == null) {
            return;
        }

        synchronized(buffer) {
            if(buffer.containsKey(output)) {
                buffer.get(output).append(message);
            } else {
                buffer.put(output, new StringBuilder(message));
            }

        }

        tryProcessEvent();
    }

    /**
     * Callback method to notify this appender the GUI is done displaying
     * the event sent through {@link #addFirstMessageToGui()} and {@link Platform#runLater(Runnable)},
     * and it can attempt to queue the next event, if {@link #buffer} is not empty.
     */
    public void notifyForNext() {
        tryProcessEvent();
    }

    /**
     * Attempts to process the next message in {@link #buffer},
     * depending on {@link #throttle}.
     * <p>
     * Queues a GUI update through {@link Platform#runLater(Runnable)} if 
     * {@link #buffer} is not empty and {@link #throttle} is {@code true}.
     */
    private void tryProcessEvent() {
        if(!buffer.isEmpty() && throttle.getAndSet(false)) {
            addFirstMessageToGui();
        }
    }
    
    
    /**
     * Adds the content of the first entry from {@link #buffer} to the UI.
     */
    private void addFirstMessageToGui() {
        var e = buffer.entrySet().iterator().next();
        var out = e.getKey();
        var str = e.getValue().toString();
        
        buffer.remove(out);
        var down = out.getTotalHeightEstimate();

        Platform.runLater(
            () -> {
                out.appendText(str);
                out.scrollYBy(down);
                
                throttle.set(true);
                this.notifyForNext();
            }
        );
        
    }
}