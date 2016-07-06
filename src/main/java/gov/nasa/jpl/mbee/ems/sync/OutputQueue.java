package gov.nasa.jpl.mbee.ems.sync;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.util.concurrent.LinkedBlockingQueue;

public class OutputQueue extends LinkedBlockingQueue<Request> {
    private Logger log = Logger.getLogger(OutputQueue.class);
    private final static OutputQueue instance = new OutputQueue();
    private volatile Request current = null;

    private OutputQueue() {
        super();
    }

    @Override
    public boolean offer(Request e) {
        boolean result = super.offer(e);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                OutputQueueStatusConfigurator.getOutputQueueStatusAction().update(true);
            }
        });
        return result;
    }

    public static OutputQueue getInstance() {
        return instance;
    }

    public void setCurrent(Request r) {
        this.current = r;
    }

    public Request getCurrent() {
        return current;
    }

    public void remove(int _rowNum) {
        //linkedQueue not contain current so the index of removing row is (_rowNum -1)
        if (this.size() >= _rowNum) {
            Request toBeRemoved = (Request) this.toArray()[_rowNum - 1];
            if (toBeRemoved != null && toBeRemoved.getJson() != null)
                log.info("[INFO] Removing a queue: " + toBeRemoved.getJson());
            super.remove(toBeRemoved);
        }
    }

}