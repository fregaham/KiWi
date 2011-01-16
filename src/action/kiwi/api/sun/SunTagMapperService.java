/**
 * 
 */
package kiwi.api.sun;

import java.util.Set;

import kiwi.exception.NamespaceResolvingException;
import kiwi.model.content.ContentItem;
import kiwi.model.ontology.SKOSConcept;

/**
 * This service is used to assign extra properties
 * (specific for the Sun Use Case) to SKOS Concepts.
 *
 * @author mradules
 */
public interface SunTagMapperService {

    /**
     * Assigns a default set of properties over all the SKOS concepts.
     * The logic is specified on the implementation.
     */
    void defaultMapping();

    /**
     * Assign the given set of properties to the given SKOSConcept.
     *
     * @param concept the involved concept.
     * @param prefix the prefix for this concept.
     * @param level the nesting for the concept.
     * @param required true if the mapping is required or not.
     * @throws NamespaceResolvingException by any name spaced related problem.
     */
    void assignResource(SKOSConcept concept, String prefix, int level,
            boolean required) throws NamespaceResolvingException;

    /**
     * Searches all the SKOS concepts with a given top level concept.
     *
     * @param topConcept the top level concept.
     * @return all the SKOS narrowed from the given top concept.
     */
    Set<SKOSConcept> search(SKOSConcept topConcept);

    /**
     * Searches all the SKOS concepts with a given top level concept and a
     * certain nesting level. It is not defined here if the nesting level
     * starts with 0 or 1, this will be done in the implementation.
     *
     * @param topConcept the top level concept.
     * @param level the nesting level.
     * @return all the SKOS narrowed from the given top concept.
     */
    Set<SKOSConcept> search(SKOSConcept topConcept, int level);
}
