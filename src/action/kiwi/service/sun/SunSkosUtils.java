/**
 * 
 */
package kiwi.service.sun;

import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.SKOSConcept;

/**
 * A set of common used methods - sun space specific.
 * 
 * @author mradules
 *
 */
public final class SunSkosUtils {

    /**
     * Builds a URI like property (Sun Space SCKOS related)
     * for a given property.
     *
     * @param property the property.
     * @return 
     */
    public static String getPropertyName(String property) {
        final StringBuilder result = new StringBuilder();
        result.append(Constants.NS_SUN_SPACE);
        result.append("/SunSpaceSkosConcept/");
        result.append(property);
        return result.toString();
    }

    public  static String getURI(SKOSConcept concept) {
        final ContentItem delegate = concept.getDelegate();
        final String uri = ((KiWiUriResource) delegate.getResource()).getUri();
        return uri;
    }

}
