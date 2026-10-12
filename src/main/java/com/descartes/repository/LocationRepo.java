package com.descartes.repository;

import com.descartes.model.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class LocationRepo {

    public static final String DEFAULT_TABLE_NAME = "Location";

    private final DynamoDbTable<Location> table;

    @Autowired
    public LocationRepo(DynamoDbEnhancedClient client) {
        this(client, DEFAULT_TABLE_NAME);
    }

    public LocationRepo(DynamoDbEnhancedClient client, String tableName) {
        this.table = client.table(tableName, TableSchema.fromBean(Location.class));
    }

    public void save(Location location) {
        table.putItem(location);
    }

    public Optional<Location> findById(String locationId) {
        return Optional.ofNullable(table.getItem(Key.builder().partitionValue(locationId).build()));
    }

    public void delete(String id) {
        table.deleteItem(Key.builder().partitionValue(id).build());
    }

    public List<Location> findAll(int limit) {
        List<Location> result = new ArrayList<>();
        ScanEnhancedRequest request = ScanEnhancedRequest.builder().limit(limit).build();
        for (var page : table.scan(request)) {
            for (Location item : page.items()) {
                result.add(item);
                if (result.size() >= limit) {
                    return result;
                }
            }
        }
        return result;
    }

}