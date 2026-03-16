package com.axeno.core.models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import com.day.cq.dam.api.Asset;

@Model(
        adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GalleryModel {

    @SlingObject
    private ResourceResolver resourceResolver;

    @ValueMapValue
    private String damPath;

    private List<ImageItem> images;

    @PostConstruct
    protected void init() {

        images = new ArrayList<>();

        if (damPath == null || damPath.isEmpty()) {
            return;
        }

        Resource damFolder = resourceResolver.getResource(damPath);

        if (damFolder != null) {

            Iterator<Resource> children = damFolder.listChildren();

            while (children.hasNext()) {

                Resource child = children.next();
                Asset asset = child.adaptTo(Asset.class);

                if (asset != null) {

                    String title = asset.getMetadataValue("dc:title");

                    if (title == null || title.isEmpty()) {
                        title = asset.getName();
                    }

                    Date createdDate = asset.getMetadata("jcr:created") != null
                            ? (Date) asset.getMetadata("jcr:created")
                            : new Date();

                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
                    String formattedDate = sdf.format(createdDate);

                    images.add(new ImageItem(
                            asset.getPath(),
                            title,
                            formattedDate
                    ));
                }
            }
        }
    }

    public List<ImageItem> getImages() {
        return images;
    }


    public static class ImageItem {

        private final String path;
        private final String title;
        private final String creationDate;

        public ImageItem(String path, String title, String creationDate) {
            this.path = path;
            this.title = title;
            this.creationDate = creationDate;
        }

        public String getPath() {
            return path;
        }

        public String getTitle() {
            return title;
        }

        public String getCreationDate() {
            return creationDate;
        }
    }
}