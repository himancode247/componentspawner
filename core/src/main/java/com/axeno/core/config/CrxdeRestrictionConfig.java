package com.axeno.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "CRXDE Restriction Configuration", description = "Configuration to restrict access to CRXDE.")
public @interface CrxdeRestrictionConfig {

    @AttributeDefinition(name = "Enable Restriction", description = "Check to enable the CRXDE restriction filter.")
    boolean enableFilter() default true;

    @AttributeDefinition(name = "Allowed Groups", description = "List of groups that are allowed to access CRXDE.")
    String[] allowedGroups() default {"administrators"};

    @AttributeDefinition(name = "Restricted Paths", description = "Paths that should be restricted by this filter.")
    String[] restrictedPaths() default {"/crx/de/index.jsp", "/crx/de"};

    @AttributeDefinition(name = "Redirect Path", description = "Path to redirect unauthorized users to.")
    String redirectPath() default "/content/training/us/en.html";
}
