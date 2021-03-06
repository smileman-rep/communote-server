package com.communote.plugins.api.rest.resource.user.property;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.communote.plugins.api.rest.exception.ExtensionNotSupportedException;
import com.communote.plugins.api.rest.exception.ResponseBuildException;
import com.communote.plugins.api.rest.resource.DefaultParameter;
import com.communote.plugins.api.rest.resource.DefaultResourceHandler;
import com.communote.plugins.api.rest.response.ResponseHelper;
import com.communote.server.api.ServiceLocator;
import com.communote.server.api.core.common.NotFoundException;
import com.communote.server.api.core.property.PropertyManagement;
import com.communote.server.api.core.property.PropertyType;
import com.communote.server.api.core.security.AuthorizationException;
import com.communote.server.model.property.StringProperty;

/**
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public class PropertyResourceHandler
        extends
        DefaultResourceHandler<CreatePropertyParameter, DefaultParameter, DefaultParameter,
        GetPropertyParameter, GetCollectionPropertyParameter> {

    private static final PropertyType PROPERTY_TYPE = PropertyType.UserProperty;

    /**
     * Getter for the {@link PropertyManagement}
     * 
     * @return the {@link PropertyManagement}
     */
    public PropertyManagement getPropertyManagement() {
        return ServiceLocator.instance().getService(PropertyManagement.class);
    }

    /**
     * Request to set user property
     * 
     * @param createPropertyParameter
     *            - an object that contains all the parameter that can or have to be used for such a
     *            request
     * @param requestedMimeType
     *            - is the mime type that indicates which data exchange format to use
     * @param uriInfo
     *            - this object is created by the request and contains some request specific data
     * @param requestSessionId
     *            - The sessionId for the session
     * @param request
     *            - javax request
     * @return null
     * @throws NotFoundException
     *             can note found property
     * @throws NumberFormatException
     *             wrong format of property
     * @throws AuthorizationException
     *             Thrown, when the user is not allowed to access.
     * @throws ResponseBuildException
     *             exception while building the response
     * @throws ExtensionNotSupportedException
     *             extension is not supported
     */
    @Override
    public Response handleCreateInternally(CreatePropertyParameter createPropertyParameter,
            String requestedMimeType, UriInfo uriInfo, String requestSessionId, Request request)
            throws NumberFormatException, NotFoundException, AuthorizationException,
            ResponseBuildException, ExtensionNotSupportedException {
        getPropertyManagement().setObjectProperty(PROPERTY_TYPE,
                createPropertyParameter.getUserId(), createPropertyParameter.getKeyGroup(),
                createPropertyParameter.getKey(), createPropertyParameter.getValue());
        return ResponseHelper.buildSuccessResponse(null, request);
    }

    /**
     * Request to get user property
     * 
     * @param getPropertyParameter
     *            - an object that contains all the parameter that can or have to be used for such a
     *            request
     * @param requestedMimeType
     *            - is the mime type that indicates which data exchange format to use
     * @param uriInfo
     *            - this object is created by the request and contains some request specific data
     * @param requestSessionId
     *            - The sessionId for the session
     * @param request
     *            - javax request
     * @return null
     * @throws NotFoundException
     *             can note found property
     * @throws NumberFormatException
     *             wrong format of property
     * @throws AuthorizationException
     *             user is not authorized for get property
     * @throws ResponseBuildException
     *             exception while building the response
     * @throws ExtensionNotSupportedException
     *             extension is not supported
     */
    @Override
    public Response handleGetInternally(GetPropertyParameter getPropertyParameter,
            String requestedMimeType, UriInfo uriInfo, String requestSessionId, Request request)
            throws NumberFormatException, NotFoundException, AuthorizationException,
            ResponseBuildException, ExtensionNotSupportedException {
        StringProperty property = getPropertyManagement().getObjectProperty(PROPERTY_TYPE,
                getPropertyParameter.getUserId(), getPropertyParameter.getKeyGroup(),
                getPropertyParameter.getKey());

        if (property == null) {
            throw new NotFoundException("Can not found property with specified parameters.");
        }

        PropertyResource propertyResource = new PropertyResource();
        propertyResource.setPropertyId(property.getId());
        propertyResource.setKeyGroup(property.getKeyGroup());
        propertyResource.setKey(property.getPropertyKey());
        propertyResource.setValue(property.getPropertyValue());
        return ResponseHelper.buildSuccessResponse(propertyResource, request);
    }

    /**
     * Request to list user properties
     * 
     * @param getCollectionPropertyParameter
     *            - an object that contains all the parameter that can or have to be used for such a
     *            request
     * @param requestedMimeType
     *            - is the mime type that indicates which data exchange format to use
     * @param uriInfo
     *            - this object is created by the request and contains some request specific data
     * @param requestSessionId
     *            - The sessionId for the session
     * @param request
     *            - javax request
     * @return null
     * @throws NotFoundException
     *             can note found property
     * @throws NumberFormatException
     *             wrong format of property
     * @throws AuthorizationException
     *             user is not authorized for get property
     * @throws ResponseBuildException
     *             exception while building the response
     * @throws ExtensionNotSupportedException
     *             extension is not supported
     */
    @Override
    public Response handleListInternally(
            GetCollectionPropertyParameter getCollectionPropertyParameter,
            String requestedMimeType, UriInfo uriInfo, String requestSessionId, Request request)
            throws NumberFormatException, NotFoundException, AuthorizationException,
            ResponseBuildException, ExtensionNotSupportedException {
        StringProperty property = getPropertyManagement().getObjectProperty(PROPERTY_TYPE,
                getCollectionPropertyParameter.getUserId(),
                getCollectionPropertyParameter.getKeyGroup(),
                getCollectionPropertyParameter.getKey());

        if (property == null) {
            throw new NotFoundException("Can not found property with specified parameters.");
        }
        PropertyResource propertyResource = new PropertyResource();
        propertyResource.setPropertyId(property.getId());
        propertyResource.setKeyGroup(property.getKeyGroup());
        propertyResource.setKey(property.getPropertyKey());
        propertyResource.setValue(property.getPropertyValue());
        List<PropertyResource> propertyResources = new ArrayList<PropertyResource>();
        propertyResources.add(propertyResource);
        return ResponseHelper.buildSuccessResponse(propertyResources, request);
    }
}
