package com.axeno.core.workflow;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = WorkflowProcess.class,
        property = {
                "process.label=Generate DC Title From Asset Name"
        }
)
public class GenerateDcTitleProcess implements WorkflowProcess {

    private static final Logger log = LoggerFactory.getLogger(GenerateDcTitleProcess.class);

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args) {

        String payloadPath = workItem.getWorkflowData().getPayload().toString();
        ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);

        Resource assetResource = resolver.getResource(payloadPath);
        if (assetResource == null) return;

        Asset asset = assetResource.adaptTo(Asset.class);
        if (asset == null) return;

        String mimeType = asset.getMimeType();
        if (mimeType == null || !mimeType.startsWith("image/")) {
            log.debug("Skipping non-image asset {}: mimeType={}", payloadPath, mimeType);
            return;
        }

        try {
            String metadataPath = payloadPath + "/jcr:content/metadata";
            Resource metadataResource = resolver.getResource(metadataPath);
            if (metadataResource == null) {
                log.warn("Metadata node not found at {}", metadataPath);
                return;
            }

            ModifiableValueMap props = metadataResource.adaptTo(ModifiableValueMap.class);
            if (props == null) return;


            String existingTitle = props.get("dc:title", String.class);
            if (existingTitle != null && !existingTitle.trim().isEmpty()) {
                log.debug("dc:title already set for {}, skipping.", payloadPath);
                return;
            }

            String assetName = asset.getName();
            String nameWithoutExt = assetName.contains(".")
                    ? assetName.substring(0, assetName.lastIndexOf('.')) : assetName;

            String cleaned = nameWithoutExt.replaceAll("[^a-zA-Z0-9]", " ");
            cleaned = cleaned.replaceAll(" +", " ");
            cleaned = cleaned.trim();


            if (cleaned.isEmpty()) {
                log.warn("Asset name '{}' produced an empty title after cleaning, skipping.", assetName);
                return;
            }

            String formattedTitle = toTitleCase(cleaned);

            props.put("dc:title", formattedTitle);
            resolver.commit();
            log.info("Set dc:title='{}' for asset {}", formattedTitle, payloadPath);

        } catch (Exception e) {
            log.error("Error setting dc:title for {}", payloadPath, e);
        }
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
