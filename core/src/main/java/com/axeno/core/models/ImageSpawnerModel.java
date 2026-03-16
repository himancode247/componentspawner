package com.axeno.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import java.util.ArrayList;
import java.util.List;

@Model(adaptables = Resource.class)
public class ImageSpawnerModel {

    @ValueMapValue(name = "imageCount")
    private int imageCount;

    // Creates a simple list of integers (0, 1, 2...) based on imageCount
    public List<Integer> getItems() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < imageCount; i++) {
            list.add(i);
        }
        return list;
    }
}