package io.github.vqnxiv.taquin.logger;

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


public abstract class AbstractFxAppender extends AbstractAppender {
    
    private record Pair(String text, TextArea output) {}

    // private static final int MAX_EVENT_BUFFER_SIZE = 50;

    private final AtomicBoolean throttle;
    // private final BlockingQueue<Pair> eventBuffer;
    private final Queue<Pair> eventBuffer;


    protected AbstractFxAppender(String name, Filter filter, Layout<? extends Serializable> layout, 
                                 boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        
        throttle = new AtomicBoolean(true);
        //eventBuffer = new ArrayBlockingQueue<>(MAX_EVENT_BUFFER_SIZE);
        eventBuffer = new ArrayDeque<>();
    }


    protected void enqueueForGui(LogEvent event, TextArea output) {
        
        if(event == null || output == null) {
            return;
        }
        
        synchronized(eventBuffer) {
            eventBuffer.add(new Pair(new String(getLayout().toByteArray(event)), output));
        }

        tryProcessEvent();
    }

    private void notifyForNext() {
        tryProcessEvent();
    }
    
    private void tryProcessEvent() {
        if(eventBuffer.isEmpty()) {
            return;
        }
        
        processEvent();
    }
    
    private void processEvent() {
        if(throttle.getAndSet(false)) {
            Platform.runLater(this::addFirstToGui);
        }
    }
    
    private void addFirstToGui() {
        var p = eventBuffer.poll();
        p.output().appendText(p.text());
        p.output().setScrollTop(-15);
        throttle.set(true);
        this.notifyForNext();
    }
}