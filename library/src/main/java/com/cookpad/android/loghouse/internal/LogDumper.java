package com.cookpad.android.loghouse.internal;

import android.util.Log;

import com.cookpad.android.loghouse.storage.Records;

import org.json.JSONObject;

public class LogDumper {
    private static final String TAG = LogDumper.class.getSimpleName();

    public static void outLogcat(Records records) {
        Log.d(TAG, buildMessage(records));
    }

    public static String buildMessage(Records records) {
        switch (records.size()) {
            case 0:
                return "No records in LogHouse's buffer";
            case 1:
                return "1 record in LogHouse's buffer" + "\n"
                        + records.getSerializedLogs().get(0);
            default:
                StringBuilder builder = new StringBuilder();
                builder.append(records.size() + " records in LogHouse's buffer\n");
                for (JSONObject log : records.getSerializedLogs()) {
                    builder.append(log.toString() + "\n");
                }
                return builder.substring(0, builder.length() - 1);
        }
    }
}
