package gov.nasa.jpl.mbee.ems.sync;

import gov.nasa.jpl.mbee.ems.ExportUtility;

public class OutputSyncRunner implements Runnable {

    @Override
    public void run() {
        OutputQueue q = OutputQueue.getInstance();
        while(true) {
            Request r;
            try {
                r = q.take();
                ExportUtility.send(r.getUrl(), r.getJson(), null, false);
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
        }
        
    }

}
