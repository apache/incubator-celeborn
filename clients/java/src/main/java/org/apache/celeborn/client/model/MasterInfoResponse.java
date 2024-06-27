/*
 * Celeborn Master REST API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1.0.0
 * Contact: user@celeborn.apache.org
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package org.apache.celeborn.client.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.celeborn.client.model.MasterCommitInfo;
import org.apache.celeborn.client.model.MasterLeader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.celeborn.client.JSON;

/**
 * MasterInfoResponse
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class MasterInfoResponse {
  public static final String SERIALIZED_NAME_GROUP_ID = "groupId";
  @SerializedName(SERIALIZED_NAME_GROUP_ID)
  private String groupId;

  public static final String SERIALIZED_NAME_LEADER = "leader";
  @SerializedName(SERIALIZED_NAME_LEADER)
  private MasterLeader leader;

  public static final String SERIALIZED_NAME_MASTER_COMMIT_INFO = "masterCommitInfo";
  @SerializedName(SERIALIZED_NAME_MASTER_COMMIT_INFO)
  private List<MasterCommitInfo> masterCommitInfo = new ArrayList<>();

  public MasterInfoResponse() {
  }

  public MasterInfoResponse groupId(String groupId) {
    this.groupId = groupId;
    return this;
  }

   /**
   * The group id of the master.
   * @return groupId
  **/
  @javax.annotation.Nonnull
  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }


  public MasterInfoResponse leader(MasterLeader leader) {
    this.leader = leader;
    return this;
  }

   /**
   * Get leader
   * @return leader
  **/
  @javax.annotation.Nonnull
  public MasterLeader getLeader() {
    return leader;
  }

  public void setLeader(MasterLeader leader) {
    this.leader = leader;
  }


  public MasterInfoResponse masterCommitInfo(List<MasterCommitInfo> masterCommitInfo) {
    this.masterCommitInfo = masterCommitInfo;
    return this;
  }

  public MasterInfoResponse addMasterCommitInfoItem(MasterCommitInfo masterCommitInfoItem) {
    if (this.masterCommitInfo == null) {
      this.masterCommitInfo = new ArrayList<>();
    }
    this.masterCommitInfo.add(masterCommitInfoItem);
    return this;
  }

   /**
   * The commit info of the master.
   * @return masterCommitInfo
  **/
  @javax.annotation.Nonnull
  public List<MasterCommitInfo> getMasterCommitInfo() {
    return masterCommitInfo;
  }

  public void setMasterCommitInfo(List<MasterCommitInfo> masterCommitInfo) {
    this.masterCommitInfo = masterCommitInfo;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MasterInfoResponse masterInfoResponse = (MasterInfoResponse) o;
    return Objects.equals(this.groupId, masterInfoResponse.groupId) &&
        Objects.equals(this.leader, masterInfoResponse.leader) &&
        Objects.equals(this.masterCommitInfo, masterInfoResponse.masterCommitInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, leader, masterCommitInfo);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MasterInfoResponse {\n");
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    leader: ").append(toIndentedString(leader)).append("\n");
    sb.append("    masterCommitInfo: ").append(toIndentedString(masterCommitInfo)).append("\n");
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


  public static HashSet<String> openapiFields;
  public static HashSet<String> openapiRequiredFields;

  static {
    // a set of all properties/fields (JSON key names)
    openapiFields = new HashSet<String>();
    openapiFields.add("groupId");
    openapiFields.add("leader");
    openapiFields.add("masterCommitInfo");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
    openapiRequiredFields.add("groupId");
    openapiRequiredFields.add("leader");
    openapiRequiredFields.add("masterCommitInfo");
  }

 /**
  * Validates the JSON Element and throws an exception if issues found
  *
  * @param jsonElement JSON Element
  * @throws IOException if the JSON Element is invalid with respect to MasterInfoResponse
  */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!MasterInfoResponse.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in MasterInfoResponse is not found in the empty JSON string", MasterInfoResponse.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!MasterInfoResponse.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `MasterInfoResponse` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }

      // check to make sure all required properties/fields are present in the JSON string
      for (String requiredField : MasterInfoResponse.openapiRequiredFields) {
        if (jsonElement.getAsJsonObject().get(requiredField) == null) {
          throw new IllegalArgumentException(String.format("The required field `%s` is not found in the JSON string: %s", requiredField, jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if (!jsonObj.get("groupId").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `groupId` to be a primitive type in the JSON string but got `%s`", jsonObj.get("groupId").toString()));
      }
      // validate the required field `leader`
      MasterLeader.validateJsonElement(jsonObj.get("leader"));
      // ensure the json data is an array
      if (!jsonObj.get("masterCommitInfo").isJsonArray()) {
        throw new IllegalArgumentException(String.format("Expected the field `masterCommitInfo` to be an array in the JSON string but got `%s`", jsonObj.get("masterCommitInfo").toString()));
      }

      JsonArray jsonArraymasterCommitInfo = jsonObj.getAsJsonArray("masterCommitInfo");
      // validate the required field `masterCommitInfo` (array)
      for (int i = 0; i < jsonArraymasterCommitInfo.size(); i++) {
        MasterCommitInfo.validateJsonElement(jsonArraymasterCommitInfo.get(i));
      };
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!MasterInfoResponse.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'MasterInfoResponse' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<MasterInfoResponse> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(MasterInfoResponse.class));

       return (TypeAdapter<T>) new TypeAdapter<MasterInfoResponse>() {
           @Override
           public void write(JsonWriter out, MasterInfoResponse value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public MasterInfoResponse read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

 /**
  * Create an instance of MasterInfoResponse given an JSON string
  *
  * @param jsonString JSON string
  * @return An instance of MasterInfoResponse
  * @throws IOException if the JSON string is invalid with respect to MasterInfoResponse
  */
  public static MasterInfoResponse fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, MasterInfoResponse.class);
  }

 /**
  * Convert an instance of MasterInfoResponse to an JSON string
  *
  * @return JSON string
  */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

