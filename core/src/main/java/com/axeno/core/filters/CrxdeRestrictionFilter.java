package com.axeno.core.filters;

import com.axeno.core.config.CrxdeRestrictionConfig;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Component(service = Filter.class, configurationPolicy = ConfigurationPolicy.OPTIONAL, property = {
        "sling.filter.scope=request",
        "sling.filter.pattern=/crx/de.*",
        "osgi.http.whiteboard.filter.regex=/crx/de.*",
        "osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=*)"
})
@Designate(ocd = CrxdeRestrictionConfig.class)
public class CrxdeRestrictionFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private boolean enableFilter;
    private List<String> allowedGroups;
    private List<String> restrictedPaths;
    private String redirectPath;

    @Activate
    @Modified
    protected void activate(CrxdeRestrictionConfig config) {
        this.enableFilter = config.enableFilter();
        this.allowedGroups = Arrays.asList(config.allowedGroups());
        this.restrictedPaths = Arrays.asList(config.restrictedPaths());
        this.redirectPath = config.redirectPath();
        logger.info("CrxdeRestrictionFilter activated with enabled={} allowedGroups={}", enableFilter, allowedGroups);
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
                         final FilterChain filterChain) throws IOException, ServletException {

        if (!enableFilter) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestUri = httpRequest.getRequestURI();

        boolean isRestrictedPath = restrictedPaths.stream().anyMatch(requestUri::startsWith);

        if (isRestrictedPath && !isUserAllowed(httpRequest)) {
            logger.warn("Unauthorized user attempted to access restricted path: {}", requestUri);
            if (redirectPath != null && !redirectPath.isEmpty()) {
                httpResponse.sendRedirect(redirectPath);
            } else {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to CRXDE is restricted for this user.");
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isUserAllowed(HttpServletRequest request) {
        ResourceResolver resourceResolver = null;

        if (request instanceof SlingHttpServletRequest) {
            resourceResolver = ((SlingHttpServletRequest) request).getResourceResolver();
        } else if (resourceResolverFactory != null) {
            resourceResolver = resourceResolverFactory.getThreadResourceResolver();
        }

        if (resourceResolver != null) {
            UserManager userManager = resourceResolver.adaptTo(UserManager.class);
            if (userManager != null) {
                try {
                    String userId = resourceResolver.getUserID();
                    if (userId == null || userId.isEmpty()) {
                        userId = request.getRemoteUser();
                    }

                    if (userId == null) {
                        return false;
                    }

                    if ("admin".equals(userId)) {
                        return true;
                    }

                    Authorizable currentUser = userManager.getAuthorizable(userId);
                    if (currentUser == null) {
                        return false;
                    }

                    Iterator<Group> groups = currentUser.memberOf();
                    while (groups.hasNext()) {
                        Group group = groups.next();
                        if (allowedGroups.contains(group.getID())) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error determining user groups: {}", e.getMessage());
                }
            } else {
                logger.warn("UserManager could not be adapted from ResourceResolver.");
            }
        } else {
            logger.warn("Could not obtain ResourceResolver to check user permissions.");
            if ("admin".equals(request.getRemoteUser())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // Nothing to do
    }

    @Override
    public void destroy() {
        // Nothing to do
    }
}
