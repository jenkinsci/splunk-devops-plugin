package com.splunk.splunkjenkins.utils;


import shaded.splk.com.google.gson.TypeAdapter;
import shaded.splk.com.google.gson.stream.JsonReader;
import shaded.splk.com.google.gson.stream.JsonToken;
import shaded.splk.com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Gson TypeAdapter for handling special double values (NaN, Infinity).
 */
public class SpecialDoubleAdapter extends TypeAdapter<Double> {
    /** {@inheritDoc} */
    @Override
    public void write(JsonWriter jsonWriter, Double number) throws IOException {
        if (number == null || Double.isNaN(number) || Double.isInfinite(number)) {
            jsonWriter.nullValue();
        } else {
            jsonWriter.value(number);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Double read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return in.nextDouble();
    }
}
