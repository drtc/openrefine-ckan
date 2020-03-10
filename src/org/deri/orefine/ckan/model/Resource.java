package org.deri.orefine.ckan.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;


public class Resource {

    public final String format;
    public final String description;
    public final String url;

    public Resource(String format, String description, String url) {
        this.format = format;
        this.description = description;
        this.url = url;
    }

    public ObjectNode asJsonObject() {
        ObjectNode obj = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(obj, "url", url);
        JSONUtilities.safePut(obj, "description", description);
        JSONUtilities.safePut(obj, "format", format);

        return obj;
    }
}
