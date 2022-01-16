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
import java.util.LinkedHashMap;
import java.util.Map;
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
     * {@link Queue} buffer to store the events until they could be buffed into {@link #textBuffers}.
     */
    private final Queue<Pair> eventBuffer;

    /**
     * Buffers the actual event messages with {@link StringBuilder}s so several messages
     * can be added at once.
     * <p>
     * Adding this on top of {@link #eventBuffer} allows to greatly reduce the number
     * of {@link Platform#runLater(Runnable)} calls. E.g in the same conditions as above,
     * (1500 states, 5 events / step = 7500 total events), we go from 7500 
     * {@link TextArea#appendText(String)} calls passed to {@link Platform#runLater(Runnable)}
     * to less than 25 (highest i've seen was 24, most in the 10-20 range) which allows the
     * 7500 messages to be displayed on the screen almost instantly.
     * <p>
     * Internally implemented with a {@link LinkedHashMap} so that events that came first
     * are added first to the UI.
     */
    private final Map<TextArea, StringBuffer> textBuffers;
    
    
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
        
        textBuffers = new LinkedHashMap<>();
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
                // formats the timestamp number
                var strTab = event.getMessage().getFormattedMessage().split("\t", 2);
                var str = String.format("%10d", Long.parseLong(strTab[0])) + strTab[1] + '\n';                
                eventBuffer.add(new Pair(str, output));
            }
            else {
                eventBuffer.add(new Pair(s, output));
            }
        }

        tryProcessEvent();
    }

    /**
     * Callback method to notify this appender the GUI is done displaying
     * the event sent through {@link #addFirstBufferToGui()} and {@link Platform#runLater(Runnable)},
     * and it can attempt to queue the next event, if {@link #eventBuffer} is not empty.
     */
    public void notifyForNext() {
        tryProcessEvent();
    }

    /**
     * Attempts to process the next event in {@link #eventBuffer} or text in {@link #textBuffers},
     * depending on {@link #throttle}.
     * <p>
     * Queues a GUI update through {@link Platform#runLater(Runnable)} if 
     * {@link #textBuffers} is not empty and {@link #throttle} is {@code true}.
     * <p>
     * Otherwise, buffers the next event message by calling {@link #bufferNextPair()}
     * if {@link #eventBuffer} is not empty.
     */
    private void tryProcessEvent() {
        if(!textBuffers.isEmpty() && throttle.getAndSet(false)) {
           addFirstBufferToGui();
        }
        else if(!eventBuffer.isEmpty()) {
            bufferNextPair();
        }
    }
    
    
    /**
     * Adds the content of the first entry from {@link #textBuffers} to the UI.
     */
    private void addFirstBufferToGui() {
        var e = textBuffers.entrySet().iterator().next();
        textBuffers.remove(e.getKey());
        var str = e.getValue().toString();

        // textArea is responsible for shit performance
        Platform.runLater(
            () -> {
                e.getKey().appendText(str);
                e.getKey().setScrollTop(-15);
                throttle.set(true);
                this.notifyForNext();
            }
        );
        
    }

    /**
     * Adds the next {@link Pair} from {@link #eventBuffer} to {@link #textBuffers}. 
     * Calls {@link #tryProcessEvent()} once it's done adding it to the map.
     */
    // todo: investigate potential NPEs here
    private void bufferNextPair() {
        var p = eventBuffer.poll();
        if(textBuffers.containsKey(p.output)) {
            textBuffers.get(p.output).append(p.text);
        }
        else {
            textBuffers.put(p.output, new StringBuffer(p.text));
        }

        tryProcessEvent();
    }
}