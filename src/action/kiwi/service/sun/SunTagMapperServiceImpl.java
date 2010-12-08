package kiwi.service.sun;

import static kiwi.model.SunConstants.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.ontology.SKOSService;
import kiwi.api.search.KiWiSearchCriteria;
import kiwi.api.search.KiWiSearchResults;
import kiwi.api.search.SolrService;
import kiwi.api.search.KiWiSearchResults.SearchResult;

import kiwi.api.sun.SunTagMapperService;
import kiwi.api.triplestore.TripleStore;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiLiteral;
import kiwi.model.kbase.KiWiNode;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.SKOSConcept;
import kiwi.service.triplestore.TripleStoreImpl;
import kiwi.service.triplestore.TripleStorePersisterImpl;
import kiwi.util.MD5;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;

import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;

/**
 * Seam component used to assign several sun use case specific properties to the
 * SKOS concepts.
 * 
 * 
 * @author mradules
 */
@Name("sunTagMapperService")
@Scope(ScopeType.SESSION)
@AutoCreate
public class SunTagMapperServiceImpl implements SunTagMapperService {

    @Logger
    private Log log;

    @In
    private SKOSService skosService;

    @In
    private SolrService solrService;

    @In
    private KiWiEntityManager kiwiEntityManager;

    @In
    private TripleStore tripleStore;
    /**
     * 
     */
    private Set<String> topConceptURIs;

    /**
     * Acts like a constructor in the seam environment.
     */
    @Create
    public void init() {
        topConceptURIs = new HashSet<String>();
        final List<SKOSConcept> topConcepts = skosService.getTopConcepts();
        for (SKOSConcept concept : topConcepts) {
            final String uri = ((KiWiUriResource) concept.getResource())
                    .getUri();
            topConceptURIs.add(uri);
        }
    }

    /**
     * Iterates over all the existing SKOS concepts and assign for each SKOS
     * concept a nesting level, prefix and parent (top level) concept.<br>
     * You can obtain this mapping with the <code>getSKOSMapping</code> method.<br>
     * This method raises an seam event with the key "sunSkosMappingsEvent".
     */
    @Override
    @Observer("doSunSpaceDefaultMapping")
    public void defaultMapping() {
        final List<SKOSConcept> topConcepts = skosService.getTopConcepts();

        Iterator<SKOSConcept> actual = topConcepts.iterator();
        int level = 0;
        // mihai : the Set guarantee no duplicates
        Set<SKOSConcept> allNarrower = new HashSet<SKOSConcept>();
        while (actual.hasNext()) {
            // mihai : I know this loop in not efficent but I run
            // out of time.
            // I can imagine that are more elegant ways to get
            // the top concept for a given concept but I don't
            // know so much about SKOS
            SKOSConcept nextConcept = actual.next();

            final String defaultPrefix = nextConcept.getTitle() + "_prefix";
            try {
                assignResource(nextConcept, defaultPrefix, level, false);
            } catch (NamespaceResolvingException e) {
                log.error(e.getMessage(), e);
            }

            final HashSet<SKOSConcept> nextNarrower = nextConcept.getNarrower();
            allNarrower.addAll(nextNarrower);
            // final Iterator<SKOSConcept> nextNarrowerIterator =
            // nextNarrower.iterator();
            // while (nextNarrowerIterator.hasNext()) {
            // final SKOSConcept nextC = nextNarrowerIterator.next();
            // final Set<SKOSConcept> narrower = nextC.getNarrower();
            // allNarrower.addAll(narrower);
            // }

            if (!actual.hasNext()) {
                actual = allNarrower.iterator();
                allNarrower = new HashSet<SKOSConcept>();
                level++;
            }
        }

        Events.instance().raiseAsynchronousEvent("sunSkosMappingsEvent");
    }

    /**
     * Assigns a given SKOS concept to a set of parameters, this are persisted
     * like rdf property for a SKOSconcept.
     * 
     */
    @Override
    public void assignResource(SKOSConcept concept, String prefix, int level,
            boolean required) throws NamespaceResolvingException {
        final String prefixProp = SunSkosUtils.getPropertyName(SUN_SKOS_PREFIX);
        final KiWiResource resource = concept.getResource();
        
        final String oldPrefix = resource.getProperty(prefixProp);
        if (!prefix.equals(oldPrefix)) {
            // I remove all the previous values to ensure that the
            // property SUN_SKOS_PREFIX exist only once.
            removeProperty(resource, prefixProp);
            resource.setProperty(prefixProp, prefix);
        }

        if (level >= 0) {
            final String levelProp = SunSkosUtils
                    .getPropertyName(SUN_SKOS_LEVEL);
            // I remove all the previous values to ensure that the
            // property SUN_SKOS_LEVEL exist only once.

            resource.setProperty(levelProp, String.valueOf(level));
        }

        final String topConceptProp = SunSkosUtils
                .getPropertyName(SUN_SKOS_TOPCONCEPT_URI);

        final String uri = getTopLevelConceptURI(concept);
        final String uriMD5sum = MD5.md5sum(uri);

        final String oldUriMD5sum = resource.getProperty(topConceptProp);
        if (!uriMD5sum.equals(oldUriMD5sum)) {
            // I remove all the previous values to ensure that the
            // property SUN_SKOS_TOPCONCEPT_URI exist only once.
            // if a Sun SKOS concept has more than one top level concept
            // then this must be removed.
            // this allows only one SKSO top level concept.

            removeProperty(resource, topConceptProp);
            resource.setProperty(topConceptProp, uriMD5sum);
        }

        final String requiredProp = SunSkosUtils
                .getPropertyName(SUN_SKOS_REQUIRED);

        final String oldRequired = resource.getProperty(requiredProp);
        if (!String.valueOf(required).equals(oldRequired)) {
            // I remove all the previous values to ensure that the
            // property SUN_PACESKOS_REQUIRED exist only once.

            removeProperty(resource, requiredProp);
            resource.setProperty(requiredProp, String.valueOf(required));
        }

         logAssignment(concept, prefix, level, uri);

    }

    private void logAssignment(SKOSConcept concept, String prefix, int level,
            final String uri) {
        final StringBuffer msg = new StringBuffer();
        msg.append("\n");
        
        msg.append("Top concent URI : ");
        msg.append(uri);
        msg.append("\n");
        
        msg.append("Top concent URI-md5 : " );
        msg.append(MD5.md5sum(uri));
        msg.append("\n");

        msg.append("Concept title : ");
        msg.append(concept.getTitle());
        msg.append("\n");

        msg.append("Prefix : ");

        msg.append(prefix);
        msg.append("\n");

        msg.append("level ");

        msg.append(level);
        msg.append("\n");


        log.debug(msg);
    }

    /**
     * Removes all the all the triples that has like subject the given
     * <code>KiWiResource</code> and a URI for the predicat.
     * 
     * @param subject
     *            the subject.
     * @param propertyURI
     *            the predicat.
     */
    private void removeProperty(KiWiResource subject, String propertyURI) {

        final KiWiUriResource predicat = tripleStore
                .createUriResource(propertyURI);
        final List<KiWiTriple> triples = tripleStore.getTriplesBySP(subject,
                predicat);
        for (KiWiTriple triple : triples) {
            final KiWiNode object = triple.getObject();
            tripleStore.removeTriple(triple);
        }
    }

    @Override
    public Set<SKOSConcept> search(SKOSConcept topConcept, int level) {
        if (topConcept == null) {
            final NullPointerException nullException = new NullPointerException(
                    "The Top Concept can not be null.");
            log.error(nullException.getMessage(), nullException);
            throw nullException;
        }

        KiWiSearchCriteria criteria;
        try {
            criteria = buildCriteria(topConcept, level);
        } catch (NamespaceResolvingException e) {
            // FIXME : not the best way to signal an error. Fix this.
            log.error(e.getMessage(), e);
            return new HashSet<SKOSConcept>();
        }
        final Set<SKOSConcept> result = search(criteria);
        return result;
    }

    @Override
    public Set<SKOSConcept> search(SKOSConcept topConcept) {
        final Set<SKOSConcept> result = search(topConcept, -1);
        return result;
    }

    private Set<SKOSConcept> search(KiWiSearchCriteria criteria) {
        // FIXME : this can create performance problems for big data quantities
        // do paging !
        criteria.setOffset(0);
        criteria.setLimit(1000);

        final KiWiSearchResults search = solrService.search(criteria);
        final List<SearchResult> resultList = search.getResults();

        final Set<SKOSConcept> result = new HashSet<SKOSConcept>();
        for (SearchResult items : resultList) {
            final ContentItem item = items.getItem();
            final SKOSConcept skos = kiwiEntityManager.createFacade(item,
                    SKOSConcept.class);
            result.add(skos);
        }
        return result;
    }

    private KiWiSearchCriteria buildCriteria(SKOSConcept topConcept, int level)
            throws NamespaceResolvingException {

        final KiWiSearchCriteria result = solrService
                .parseSearchString("type:skos:Concept");

        final Map<String, Set<String>> literalProperties = result
                .getRdfLiteralProperties();

        // final String uri = "http://www.newmedialab.at/fcp/Ressort";
        final String uri = getTopLevelConceptURI(topConcept);
        final String md5Uri = MD5.md5sum(uri);

        final String uriPropKey = SunSkosUtils
                .getPropertyName(SUN_SKOS_TOPCONCEPT_URI);
        literalProperties.put(uriPropKey, Collections.singleton(md5Uri));

        if (level >= 0) {
            final String levelPropKey = SunSkosUtils
                    .getPropertyName(SUN_SKOS_LEVEL);
            final String strLevel = String.valueOf(level);
            literalProperties
                    .put(levelPropKey, Collections.singleton(strLevel));
        }

        
        return result;
    }

    // private String getTopConceptURI(SKOSConcept concept)
    // throws NamespaceResolvingException {
    //
    // final KiWiUriResource resource =
    // (KiWiUriResource) concept.getResource();
    //
    // final String uriPropKey =
    // SunSkosUtils.getPropertyName(SUN_SKOS_TOPCONCEPT_URI);
    //
    // final String result = resource.getProperty(uriPropKey);
    // return result;
    // }

    private String getTopLevelConceptURI(SKOSConcept concept) {
        final Set<ContentItem> topBraderConceptFor = getTopBroaderConcepts(concept);

        for (ContentItem item : topBraderConceptFor) {
            final String uri = ((KiWiUriResource) item.getResource()).getUri();
            final boolean isTop = topConceptURIs.contains(uri);
            if (isTop) {
                return uri;
            }
        }

        throw new IllegalStateException("No top level found");
    }

    private Set<ContentItem> getTopBroaderConcepts(SKOSConcept concept) {
        final List<KiWiResource> listBroader = skosService.listBroader(
                concept.getResource(), false);
        Iterator<KiWiResource> iterator = listBroader.iterator();

        final Set<ContentItem> result = new HashSet<ContentItem>();
        if (!iterator.hasNext()) {
            result.add(concept.getDelegate());
            return result;
        }

        List<KiWiResource> nextIteration = new LinkedList<KiWiResource>();
        while (iterator.hasNext()) {
            final KiWiResource next = iterator.next();
            final List<KiWiResource> nextBroader = skosService.listBroader(
                    next, false);
            if (nextBroader.isEmpty()) {
                result.add(next.getContentItem());
            } else {
                nextIteration.addAll(nextBroader);
            }

            if (!iterator.hasNext()) {
                iterator = nextIteration.iterator();
                nextIteration = new LinkedList<KiWiResource>();
            }
        }

        return result;
    }
}
