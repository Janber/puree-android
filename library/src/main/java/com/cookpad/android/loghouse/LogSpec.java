package com.cookpad.android.loghouse;

import com.cookpad.android.loghouse.handlers.AfterFlushFilter;
import com.cookpad.android.loghouse.internal.LogDumper;
import com.cookpad.android.loghouse.storage.Records;

import junit.framework.AssertionFailedError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LogSpec {
    private static final Object LOCK = new Object();

    private LogHouseConfiguration conf;
    private List<SerializableLog> logs;
    private String target;

    public LogSpec(LogHouseConfiguration conf) {
        this.conf = conf;
    }

    public LogSpec logs(SerializableLog... logs) {
        return logs(Arrays.asList(logs));
    }

    public LogSpec logs(List<SerializableLog> logs) {
        this.logs = logs;
        return this;
    }

    public LogSpec target(String type) {
        this.target = type;
        return this;
    }

    public void shouldBe(Matcher matcher) {
        synchronized (LOCK) {
            final CountDownLatch latch = new CountDownLatch(logs.size());
            final List<JSONObject> results = new ArrayList<>();

            final String[] compareInfoMessage = {"[compare] target : type\n"};
            conf.setAfterFlushFilter(new AfterFlushFilter() {
                @Override
                public void call(String type, List<JSONObject> serializedLogs) {
                    compareInfoMessage[0] += "    " + target + " : " + type + "\n";

                    if (target.equals(type)) {
                        results.addAll(serializedLogs);
                    }
                    latch.countDown();
                }
            });

            initializeLogHouse(conf);
            putLogs(logs);

            try {
                latch.await(1000, TimeUnit.MILLISECONDS);
                matcher.expect(results);
            } catch (AssertionFailedError e) {
                Records records = LogHouse.getBufferedLogs();
                String message = LogDumper.buildMessage(records);
                throw new AssertionFailedError(e.getMessage() + "\n"
                        + compareInfoMessage[0]
                        + "[result size] " + results.size() + "\n"
                        + message);
            } catch (JSONException | InterruptedException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    private void putLogs(List<SerializableLog> logs) {
        for (SerializableLog log : logs) {
            LogHouse.in(log);
        }
    }

    private void initializeLogHouse(LogHouseConfiguration conf) {
        LogHouse.initialize(conf);
        LogHouse.clear();
    }

    public static interface Matcher {
        public void expect(List<JSONObject> serializedLogs) throws JSONException;
    }
}
