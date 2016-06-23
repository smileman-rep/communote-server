package com.communote.plugins.widget.extension;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import com.communote.server.web.fe.widgets.type.ContentTypeWidgetExtension;


/**
 * ContentType for hyperlinks.
 * 
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
@Component
@Provides
@Instantiate
public class LinkContentTypeWidgetExtension extends ContentTypeWidgetExtension {
    /**
     * Constructor.
     */
    public LinkContentTypeWidgetExtension() {
        super("link", "type.link",
                "['Note','com.communote','contentTypes.link','link','EQUALS']", 10,
                CATEGORY_NOTE_LIST);
    }
}
