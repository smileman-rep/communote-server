package com.communote.server.core.tag.category;


/**
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public class CategorizedTagNotFoundException extends
        com.communote.server.core.tag.category.CategorizedTagException {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4368665166903188410L;

    /**
     * Constructs a new instance of CategorizedTagNotFoundException
     *
     */
    public CategorizedTagNotFoundException(String message) {
        super(message);
    }

}
