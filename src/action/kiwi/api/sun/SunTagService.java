package kiwi.api.sun;

import java.util.List;
import java.util.Set;

import kiwi.model.content.ContentItem;
import kiwi.model.ontology.SKOSConcept;
import kiwi.model.tagging.Tag;

/**
 * This interface is based on the web services defined here :
 * http://www.kiwi-community.eu/display/sunspace/Tag+Completer+web+services+specification#TagCompleterwebservicesspecification-GetChildrenRequest
 * 
 * @author mradules
 */
public interface SunTagService {

    /**
     * Returns list of taxonomies present in the system. Each taxonomy is represented by its SKOS top concept. 
     * The list is ordered in alphabetical order of the preferred labels.
     * 
     * @return
     */
    Set<SKOSConcept> getTaxonomies();

    /**
     * Provides list of ALL prefixes present in the system. Evaluated by listing all triples that have a 
     * http://www.kiwi-project.eu/kiwi/sunspace/SunSpaceSkosConcept/sunspaceSkosPrefix property and taking the object literals. 
     * Result ordered alphabetically
     * 
     * @return
     */
    Set<String> getPrefixes();

    /**
     * Tag Existing resource.
     * 
     * @param contentItem the resource (document, person, community,...) to be tagged. 
     * @param 
     */
    void addTags(ContentItem contentItem, List<String> tagLables);

    /**
     * Remove tag(s) from existing resource.
     * 
     * @param item
     */
    void removeTag(ContentItem contentItem, Tag tag);

    /**
     * Returns content items used as free tags (i.e. not a skos:Concept) for a given prefix. Prefix
     * is matched on the content item titles. Result is ordered in lexical order of the titles.
     * 
     * @param prefix
     * @return
     */
    Set<ContentItem> searchFreeTags(String prefix);

    /**
     * Returns content items used as controlled tags (i.e. of type skos:Concept) for a given prefix.
     * Prefix is matched on the content item titles. Result is ordered in lexical order of the titles.
     * 
     * @param prefix
     * @return
     */
    Set<ContentItem> searchControlledTagsForLabel(String prefix);
    
    Set<ContentItem> searchControlledTagsForPrefix(String prefix);

    Set<ContentItem> searchControlledTags(String topConceptURI, int level);

    
    /**
     * Returns content items used as controlled tags (i.e. of type skos:Concept) and occurring at the 
     * defined level of the taxonomy (counted from taxonomy concept at level 1) for a given prefix and
     * top concept uri.
     * Prefix is matched on the content item titles. Result is ordered in lexical order of the titles.
     * 
     * @param prefix
     * @param level the level where to look for the concepts, counted from the top concept (level 1)
     * @return
     */
    Set<ContentItem> searchControlledTags(String topConceptURI, String prefix,
            int level);
    
    Set<ContentItem> searchControlledTags(ContentItem topConcept, String prefix, String label);

    /**
     * Searches all the controlled tags that have a given top concept and them
     * title starts with the given prefix.
     *
     * @param topConcept the top concept.
     * @param labelPrefix the prefix for the label.
     * @return all the controlled tags that have a given top concept and them
     *         title starts with the given label.
     */
    Set<ContentItem> searchControlledTags(ContentItem topConcept, String labelPrefix);

    /**
     * Returns content items used as controlled tags (i.e. of type skos:Concept) and occurring at the 
     * defined level of the taxonomy (counted from taxonomy concept at level 1) for a given prefix.
     * Prefix is matched on the content item titles. Result is ordered in lexical order of the titles.
     * 
     * @param prefix
     * @param level the level where to look for the concepts, counted from the top concept (level 1)
     * @return
     */
    Set<ContentItem> searchControlledTags(String label, String prefix);

    /**
     * Return all content items used as free tags, i.e. of type kiwi:Tag. Result is ordered in lexical
     * order of the titles.
     * @return
     */
    Set<ContentItem> getAllTags();

    /**
     * Return all content items used as tags of the given content item, including both free and concept
     * tags.
     * 
     * @param contentItem
     * @return
     */
    Set<ContentItem> getAllTags(ContentItem contentItem);
    
    /**
     * Returns all content items that are children of the specified content item in the SKOS thesaurus.
     * Makes use of the skos:narrower and skos:broader relations. Result is a list ordered by label.
     * 
     * 
     * @param item
     * @return
     */
    Set<ContentItem> getChildren(ContentItem item);
    
    /**
     * Service returns all siblings of the specified tag and also the specified tag.
     * Or in other words, returns all tags whose parent is the parent of the specified tag.
     * Makes use of the skos:narrower and skos:broader relations. Result is a list ordered by parent
     * and label.
     * 
     * @param item
     * @return
     */
    Set<ContentItem> getSiblings(ContentItem item);

    Set<ContentItem> searchControlledTags(ContentItem parent);
    
    ContentItem getToplevelTag(String prefix);
    
    /**
     * Removes a given tag, the tag is identified after the URI.
     *
     * @param tagged
     *            the tagged document with the given tag, it can not be null.
     * @param tagURI
     *            the tag URI, it can not be null.
     */
    void removeTag(ContentItem tagged, String tagURI);
}
