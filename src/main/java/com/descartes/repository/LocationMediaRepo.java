package com.descartes.repository;

import com.descartes.model.LocationMedia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class LocationMediaRepo {

    public static final String DEFAULT_TABLE_NAME = "LocationMedia";

    private final DynamoDbTable<LocationMedia> table;

    @Autowired
    public LocationMediaRepo(DynamoDbEnhancedClient client) {
        this(client, DEFAULT_TABLE_NAME);
    }

    public LocationMediaRepo(DynamoDbEnhancedClient client, String tableName) {
        this.table = client.table(tableName, TableSchema.fromBean(LocationMedia.class));
    }

    public void save(LocationMedia media) {
        table.putItem(media);
    }

    public List<LocationMedia> findByLocationId(String locationId) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(locationId).build());
        List<LocationMedia> result = new ArrayList<>();
        table.query(condition).items().forEach(result::add);
        return result;
    }

    public Optional<LocationMedia> findById(String locationId, String id) {
        QueryConditional condition = QueryConditional.keyEqualTo(key(locationId, id));
        List<LocationMedia> result = new ArrayList<>();
        return table.query(condition).items().stream().findAny();
    }

    public void deleteById(String locationId, String id) {
        table.deleteItem(key(locationId, id));
    }

    private static Key key(String locationId, String id) {
        return Key.builder().partitionValue(locationId).sortValue(id).build();
    }

}
