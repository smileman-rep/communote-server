package com.communote.plugins.api.rest.servlet;

import com.communote.plugins.api.rest.resource.AbstractResourceHandler;

/**
 * A REST API resource
 * 
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a> 
 **/
public abstract class AbstractResource {
    /**
     * @return handler of the resource
     */
    protected abstract AbstractResourceHandler<?, ?, ?, ?, ?> getResourceHandler();
}
