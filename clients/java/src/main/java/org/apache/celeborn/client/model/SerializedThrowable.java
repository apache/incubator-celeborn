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
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.File;
import java.io.IOException;

/**
 * SerializedThrowable
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class SerializedThrowable {
  public static final String SERIALIZED_NAME_SERIALIZED_THROWABLE = "serialized-throwable";
  @SerializedName(SERIALIZED_NAME_SERIALIZED_THROWABLE)
  private File serializedThrowable;


  public SerializedThrowable serializedThrowable(File serializedThrowable) {
    
    this.serializedThrowable = serializedThrowable;
    return this;
  }

   /**
   * Get serializedThrowable
   * @return serializedThrowable
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public File getSerializedThrowable() {
    return serializedThrowable;
  }


  public void setSerializedThrowable(File serializedThrowable) {
    this.serializedThrowable = serializedThrowable;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SerializedThrowable serializedThrowable = (SerializedThrowable) o;
    return Objects.equals(this.serializedThrowable, serializedThrowable.serializedThrowable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serializedThrowable);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SerializedThrowable {\n");
    sb.append("    serializedThrowable: ").append(toIndentedString(serializedThrowable)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

