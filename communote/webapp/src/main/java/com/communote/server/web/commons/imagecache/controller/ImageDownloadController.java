package com.communote.server.web.commons.imagecache.controller;

import java.io.InputStream;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import com.communote.common.util.ParameterHelper;
import com.communote.server.api.ServiceLocator;
import com.communote.server.api.core.image.Image;
import com.communote.server.api.core.image.ImageManager;
import com.communote.server.api.core.image.ImageNotFoundException;
import com.communote.server.api.core.security.AuthorizationException;
import com.communote.server.model.user.ImageSizeType;

/**
 * Controller for supporting image download.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class ImageDownloadController implements Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageDownloadController.class);

    // 1 day
    private final static int MAX_AGE_IN_SECONDS = 60 * 60 * 24;
    private final static int MAX_AGE_IN_MILLIS = 1000 * MAX_AGE_IN_SECONDS;

    /**
     * Checks, if the property was modified since the last request.
     *
     * @param request
     *            The request.
     * @param date
     *            The last modification date of the given property.
     * @return True, if the property was modified.
     */
    private boolean checkWasModified(HttpServletRequest request, Date date) {
        long modifiedSince = request.getDateHeader("If-Modified-Since");
        // cannot use less-than comparison because there can be different providers for an image
        // type. Because of the provider fallback mechanism (use a built-in provider if an external
        // provider could not load an image) the image URL generated by one provider can be answered
        // with an image from another provider. Thus, a 304 might be sent if the external provider
        // suddenly becomes available and returns an image which is older
        // TODO maybe use eTag instead and encode provider ID and last modified timestamp in it?
        return modifiedSince != (date.getTime() / 60000 * 60000);
    }

    /**
     * @param request
     *            The request.
     * @return The type if any.
     */
    private String getTypeName(HttpServletRequest request) {
        String pathInfo = request.getServletPath() + request.getPathInfo();
        String type = StringUtils.substringBetween(pathInfo, "/image/", ".jpg");
        return type != null ? type : StringUtils.substringBetween(pathInfo, "/image/", ".do");
    }

    /**
     * Returns the image.
     *
     * @param request
     *            The request.
     * @param response
     *            The response.
     * @return <code>null</code>. This controller writes the data directly into the response.
     *
     * @throws Exception
     *             On any error.
     */
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String typeName = getTypeName(request);
        ImageSizeType size = null;
        String sizeString = ParameterHelper.getParameterAsString(request.getParameterMap(), "size");
        if (sizeString != null) {
            try {
                size = ImageSizeType.fromString(sizeString);
            } catch (IllegalArgumentException e) {
                // ignore and fallback to SMALL
                // TODO better send 403?
            }
        }
        if (size == null) {
            LOGGER.trace("No or unsupported size given. Falling back to SMALL.");
            size = ImageSizeType.SMALL;
        }
        ImageManager imageManagement = ServiceLocator.findService(ImageManager.class);
        try {
            Image image = imageManagement.getImage(typeName, request.getParameterMap(), size);
            if (!checkWasModified(request, image.getLastModificationDate())) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return null;
            }
            response.setContentType(image.getMimeType());
            response.addHeader("Content-Length", Long.toString(image.getSize()));
            response.setDateHeader("Last-Modified", image.getLastModificationDate().getTime());
            response.setDateHeader("Expires", System.currentTimeMillis() + MAX_AGE_IN_MILLIS);
            // images should not be cached by public shared caches because they may contain
            // sensitive data, thus cache-control should contain the 'private' directive. But
            // certain versions of Fireox won't cache images on disk if the image was retrieved via
            // HTTPS and cache-control does not contain the 'public' directive. Note: using public
            // with HTTPS does not introduce a security issue because of the end-to-end encryption
            String sharedCaching = request.isSecure() ? ", public" : ", private";
            response.setHeader("Cache-Control", "max-age=" + MAX_AGE_IN_SECONDS + sharedCaching);
            InputStream imageInputStream = image.openStream();
            IOUtils.copy(imageInputStream, response.getOutputStream());
            IOUtils.closeQuietly(imageInputStream);
        } catch (AuthorizationException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (ImageNotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        return null;
    }
}
