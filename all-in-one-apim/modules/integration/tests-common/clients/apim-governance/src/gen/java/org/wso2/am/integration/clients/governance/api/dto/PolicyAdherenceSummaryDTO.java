/*
 * WSO2 API Manager - Governance
 * This document specifies a **RESTful API** for WSO2 **API Manager** - Governance. 
 *
 * The version of the OpenAPI document: v1.1
 * Contact: architecture@wso2.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package org.wso2.am.integration.clients.governance.api.dto;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import com.fasterxml.jackson.annotation.JsonCreator;
/**
* Summary of governance policy adherence in the organization.
*/
    @ApiModel(description = "Summary of governance policy adherence in the organization.")

public class PolicyAdherenceSummaryDTO {
        public static final String SERIALIZED_NAME_TOTAL = "total";
        @SerializedName(SERIALIZED_NAME_TOTAL)
            private Integer total;

        public static final String SERIALIZED_NAME_FOLLOWED = "followed";
        @SerializedName(SERIALIZED_NAME_FOLLOWED)
            private Integer followed;

        public static final String SERIALIZED_NAME_VIOLATED = "violated";
        @SerializedName(SERIALIZED_NAME_VIOLATED)
            private Integer violated;

        public static final String SERIALIZED_NAME_UN_APPLIED = "unApplied";
        @SerializedName(SERIALIZED_NAME_UN_APPLIED)
            private Integer unApplied;


        public PolicyAdherenceSummaryDTO total(Integer total) {
        
        this.total = total;
        return this;
        }

    /**
        * Total number of policies.
    * @return total
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "10", value = "Total number of policies.")
    
    public Integer getTotal() {
        return total;
    }


    public void setTotal(Integer total) {
        this.total = total;
    }


        public PolicyAdherenceSummaryDTO followed(Integer followed) {
        
        this.followed = followed;
        return this;
        }

    /**
        * Number of policies followed to.
    * @return followed
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "6", value = "Number of policies followed to.")
    
    public Integer getFollowed() {
        return followed;
    }


    public void setFollowed(Integer followed) {
        this.followed = followed;
    }


        public PolicyAdherenceSummaryDTO violated(Integer violated) {
        
        this.violated = violated;
        return this;
        }

    /**
        * Number of policies violated.
    * @return violated
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "4", value = "Number of policies violated.")
    
    public Integer getViolated() {
        return violated;
    }


    public void setViolated(Integer violated) {
        this.violated = violated;
    }


        public PolicyAdherenceSummaryDTO unApplied(Integer unApplied) {
        
        this.unApplied = unApplied;
        return this;
        }

    /**
        * Number of policies unApplied.
    * @return unApplied
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "0", value = "Number of policies unApplied.")
    
    public Integer getUnApplied() {
        return unApplied;
    }


    public void setUnApplied(Integer unApplied) {
        this.unApplied = unApplied;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
        return true;
        }
        if (o == null || getClass() != o.getClass()) {
        return false;
        }
            PolicyAdherenceSummaryDTO policyAdherenceSummary = (PolicyAdherenceSummaryDTO) o;
            return Objects.equals(this.total, policyAdherenceSummary.total) &&
            Objects.equals(this.followed, policyAdherenceSummary.followed) &&
            Objects.equals(this.violated, policyAdherenceSummary.violated) &&
            Objects.equals(this.unApplied, policyAdherenceSummary.unApplied);
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, followed, violated, unApplied);
    }


@Override
public String toString() {
StringBuilder sb = new StringBuilder();
sb.append("class PolicyAdherenceSummaryDTO {\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    followed: ").append(toIndentedString(followed)).append("\n");
    sb.append("    violated: ").append(toIndentedString(violated)).append("\n");
    sb.append("    unApplied: ").append(toIndentedString(unApplied)).append("\n");
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

