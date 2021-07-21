package com.splunk.splunkjenkins.console;

import jenkins.util.Timer;
import org.jenkinsci.remoting.SerializableOnlyOverRemoting;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
Code copied over from
https://github.com/jenkinsci/workflow-api-plugin/blob/master/src/main/java/org/jenkinsci/plugins/workflow/log/DelayBufferedOutputStream.java
 */
final class DelayBufferedOutputStream extends BufferedOutputStream {
    private static final Logger LOGGER = Logger.getLogger(DelayBufferedOutputStream.class.getName());
    private final DelayBufferedOutputStream.Tuning tuning;
    private long recurrencePeriod;

    DelayBufferedOutputStream(OutputStream out) {
        this(out, DelayBufferedOutputStream.Tuning.DEFAULT);
    }

    DelayBufferedOutputStream(OutputStream out, DelayBufferedOutputStream.Tuning tuning) {
        super(new DelayBufferedOutputStream.FlushControlledOutputStream(out), tuning.bufferSize);
        this.tuning = tuning;
        this.recurrencePeriod = tuning.minRecurrencePeriod;
        this.reschedule();
    }

    private void reschedule() {
        Timer.get().schedule(new DelayBufferedOutputStream.Flush(this), this.recurrencePeriod, TimeUnit.MILLISECONDS);
        this.recurrencePeriod = Math.min((long)((float)this.recurrencePeriod * this.tuning.recurrencePeriodBackoff), this.tuning.maxRecurrencePeriod);
    }

    private void flushBuffer() throws IOException {
        ThreadLocal<Boolean> enableFlush = ((DelayBufferedOutputStream.FlushControlledOutputStream)this.out).enableFlush;
        boolean orig = (Boolean)enableFlush.get();
        enableFlush.set(false);

        try {
            this.flush();
        } finally {
            enableFlush.set(orig);
        }

    }

    void flushAndReschedule() {
        try {
            this.flushBuffer();
        } catch (IOException var2) {
            LOGGER.log(Level.FINE, (String)null, var2);
        }

        this.reschedule();
    }

    public String toString() {
        return "DelayBufferedOutputStream[" + this.out + "]";
    }

    private static final class FlushControlledOutputStream extends FilterOutputStream {
        private final ThreadLocal<Boolean> enableFlush = new ThreadLocal<Boolean>() {
            protected Boolean initialValue() {
                return true;
            }
        };

        FlushControlledOutputStream(OutputStream out) {
            super(out);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            this.out.write(b, off, len);
        }

        public void flush() throws IOException {
            if ((Boolean)this.enableFlush.get()) {
                super.flush();
            }

        }

        public String toString() {
            return "FlushControlledOutputStream[" + this.out + "]";
        }
    }

    private static final class Flush implements Runnable {
        private final Reference<DelayBufferedOutputStream> osr;

        Flush(DelayBufferedOutputStream os) {
            this.osr = new WeakReference(os);
        }

        public void run() {
            DelayBufferedOutputStream os = (DelayBufferedOutputStream)this.osr.get();
            if (os != null) {
                os.flushAndReschedule();
            }

        }
    }

    static final class Tuning implements SerializableOnlyOverRemoting {
        long minRecurrencePeriod = Long.getLong(DelayBufferedOutputStream.class.getName() + ".minRecurrencePeriod", 1000L);
        long maxRecurrencePeriod = Long.getLong(DelayBufferedOutputStream.class.getName() + ".maxRecurrencePeriod", 10000L);
        float recurrencePeriodBackoff = Float.parseFloat(System.getProperty(DelayBufferedOutputStream.class.getName() + ".recurrencePeriodBackoff", "1.05"));
        int bufferSize = Integer.getInteger(DelayBufferedOutputStream.class.getName() + ".bufferSize", 65536);
        static final DelayBufferedOutputStream.Tuning DEFAULT = new DelayBufferedOutputStream.Tuning();

        private Tuning() {
        }
    }
}
