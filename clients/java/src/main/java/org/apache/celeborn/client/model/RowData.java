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
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.celeborn.client.model.RowKind;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.celeborn.client.JSON;


/**
 * RowData
 */
@JsonPropertyOrder({
  RowData.JSON_PROPERTY_ARITY,
  RowData.JSON_PROPERTY_ROW_KIND
})
@javax.annotation.processing.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class RowData {
  public static final String JSON_PROPERTY_ARITY = "arity";
  private Integer arity;

  public static final String JSON_PROPERTY_ROW_KIND = "rowKind";
  private RowKind rowKind;


  public RowData arity(Integer arity) {
    this.arity = arity;
    return this;
  }

   /**
   * Get arity
   * @return arity
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")
  @JsonProperty(JSON_PROPERTY_ARITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getArity() {
    return arity;
  }


  public void setArity(Integer arity) {
    this.arity = arity;
  }


  public RowData rowKind(RowKind rowKind) {
    this.rowKind = rowKind;
    return this;
  }

   /**
   * Get rowKind
   * @return rowKind
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")
  @JsonProperty(JSON_PROPERTY_ROW_KIND)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public RowKind getRowKind() {
    return rowKind;
  }


  public void setRowKind(RowKind rowKind) {
    this.rowKind = rowKind;
  }


  /**
   * Return true if this RowData object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RowData rowData = (RowData) o;
    return Objects.equals(this.arity, rowData.arity) &&
        Objects.equals(this.rowKind, rowData.rowKind);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arity, rowKind);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RowData {\n");
    sb.append("    arity: ").append(toIndentedString(arity)).append("\n");
    sb.append("    rowKind: ").append(toIndentedString(rowKind)).append("\n");
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

