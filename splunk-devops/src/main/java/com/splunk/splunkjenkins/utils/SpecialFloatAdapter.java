package com.splunk.splunkjenkins.utils;


import shaded.splk.com.google.gson.TypeAdapter;
import shaded.splk.com.google.gson.stream.JsonReader;
import shaded.splk.com.google.gson.stream.JsonToken;
import shaded.splk.com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Gson TypeAdapter for handling special float values (NaN, Infinity).
 */
public class SpecialFloatAdapter extends TypeAdapter<Float> {
    /** {@inheritDoc} */
    @Override
    public void write(JsonWriter jsonWriter, Float number) throws IOException {
        if (number == null || Float.isNaN(number) || Float.isInfinite(number)) {
            jsonWriter.nullValue();
        } else {
            jsonWriter.value(number);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Float read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return (float) in.nextDouble();
    }
}
