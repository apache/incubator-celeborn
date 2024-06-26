/*
 * Flink SQL Gateway REST API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1/1.16
 * Contact: user@flink.apache.org
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package org.apache.celeborn.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Gets or Sets RowKind
 */
@JsonAdapter(RowKind.Adapter.class)
public enum RowKind {
  
  INSERT("INSERT"),
  
  UPDATE_BEFORE("UPDATE_BEFORE"),
  
  UPDATE_AFTER("UPDATE_AFTER"),
  
  DELETE("DELETE");

  private String value;

  RowKind(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static RowKind fromValue(String value) {
    for (RowKind b : RowKind.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  public static class Adapter extends TypeAdapter<RowKind> {
    @Override
    public void write(final JsonWriter jsonWriter, final RowKind enumeration) throws IOException {
      jsonWriter.value(enumeration.getValue());
    }

    @Override
    public RowKind read(final JsonReader jsonReader) throws IOException {
      String value = jsonReader.nextString();
      return RowKind.fromValue(value);
    }
  }
}

