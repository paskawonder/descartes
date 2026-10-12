package com.descartes.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class LocationMedia {

    private String id;
    private String locationId;
    private String url;
    private String category;
    private String attributes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDbPartitionKey
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }
}
