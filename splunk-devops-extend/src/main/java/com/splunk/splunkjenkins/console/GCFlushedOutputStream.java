package com.splunk.splunkjenkins.console;

import jenkins.util.Timer;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
Code copied over from
https://github.com/jenkinsci/workflow-api-plugin/blob/master/src/main/java/org/jenkinsci/plugins/workflow/log/GCFlushedOutputStream.java
 */
final class GCFlushedOutputStream extends FilterOutputStream {
    private static final Logger LOGGER = Logger.getLogger(GCFlushedOutputStream.class.getName());

    GCFlushedOutputStream(OutputStream out) {
        super(out);
        GCFlushedOutputStream.FlushRef.register(this, out);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        this.out.write(b, off, len);
    }

    public String toString() {
        return "GCFlushedOutputStream[" + this.out + "]";
    }

    private static final class FlushRef extends PhantomReference<GCFlushedOutputStream> {
        private static final ReferenceQueue<GCFlushedOutputStream> rq = new ReferenceQueue();
        private final OutputStream out;

        static void register(GCFlushedOutputStream fos, OutputStream out) {
            (new GCFlushedOutputStream.FlushRef(fos, out, rq)).enqueue();
        }

        private FlushRef(GCFlushedOutputStream fos, OutputStream out, ReferenceQueue<GCFlushedOutputStream> rq) {
            super(fos, rq);
            this.out = out;
        }

        static {
            Timer.get().scheduleWithFixedDelay(() -> {
                while(true) {
                    GCFlushedOutputStream.FlushRef ref = (GCFlushedOutputStream.FlushRef)rq.poll();
                    if (ref == null) {
                        return;
                    }

                    GCFlushedOutputStream.LOGGER.log(Level.FINE, "flushing {0}", ref.out);

                    try {
                        ref.out.flush();
                    } catch (IOException var2) {
                        GCFlushedOutputStream.LOGGER.log(Level.WARNING, (String)null, var2);
                    }
                }
            }, 0L, 10L, TimeUnit.SECONDS);
        }
    }
}
