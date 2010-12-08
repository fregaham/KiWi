

package kiwi.service.sun;

import static kiwi.model.SunConstants.*;

import java.util.*;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;

import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.ontology.SKOSService;
import kiwi.api.search.KiWiSearchCriteria;
import kiwi.api.search.KiWiSearchResults;
import kiwi.api.search.KiWiSearchResults.SearchResult;
import kiwi.api.search.SolrService;
import kiwi.api.sun.SunTagService;
import kiwi.api.tagging.TaggingService;
import kiwi.api.transaction.TransactionService;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiLiteral;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.SKOSConcept;
import kiwi.model.tagging.Tag;
import kiwi.service.transaction.KiWiSynchronizationImpl;
import kiwi.util.MD5;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.jboss.seam.transaction.Transaction;
import org.jboss.seam.transaction.UserTransaction;


/**
 * @author Mihai
 */
@Name("sunTagService")
@Scope(ScopeType.STATELESS)
@AutoCreate
public class SunTagServiceImpl implements SunTagService {

    @Logger
    private Log log;

    @In(create = true)
    private SolrService solrService;

    @In
    private SKOSService skosService;

    @In
    private TaggingService taggingService;

    @In
    private TripleStore tripleStore;
    
    @In
    private TransactionService transactionService;

    @In
    private KiWiEntityManager kiwiEntityManager;
    
    @In
    private EntityManager entityManager;

    @Override
    public void addTags(ContentItem item, List<String> tagLables) {
        taggingService.addTagsByLabel(item, new HashSet(tagLables));
    }

    @Override
    public void removeTag(ContentItem item, Tag tag) {
        taggingService.removeTagging(tag);
    }

    @Override
    public Set<SKOSConcept> getTaxonomies() {
        final List<SKOSConcept> topConcepts = skosService.getTopConcepts();
        
        final int size = topConcepts.size();
        final Set<SKOSConcept> result = new HashSet<SKOSConcept>(size);
        for (SKOSConcept concept : topConcepts) {
            result.add(concept);
        }
        return result;
    }

    @Override
    public Set<String> getPrefixes() {

        // Sebastian:
        // there is a simpler way of retrieving the prefixes by
// using the triple store either by SPARQL or even by
        // using the method "tripleStore.getTriplesByP"
        final String prefixProp =
                SunSkosUtils.getPropertyName("sunspaceSkosPrefix");

        final Set<String> result = new HashSet<String>();
        for (KiWiTriple t : tripleStore.getTriplesByP(prefixProp)) {
            if (t.getObject().isLiteral()) {
                result.add(((KiWiLiteral) t.getObject()).getContent());
            }
        }
        return result;
    }

    @Override
    public Set<ContentItem> searchControlledTags(String topConceptURI, int level) {
        final KiWiSearchCriteria criteria =
                getConceptCriteria(topConceptURI, level);
        return search(criteria);
    }

    @Override
    public Set<ContentItem> searchControlledTags(String topConceptURI, String prefix, int level) {
        final KiWiSearchCriteria criteria = getConceptCriteria(topConceptURI, prefix, level);

        return search(criteria);
    }

    @Override
    public Set<ContentItem> searchControlledTags(ContentItem topConcept,
            String prefix, String label) {

        final KiWiResource resource = topConcept.getResource();
        final String uriProp =
                SunSkosUtils.getPropertyName(SUN_SKOS_TOPCONCEPT_URI);
        final String topConceptURI = resource.getProperty(uriProp);

        if (topConceptURI == null) {
            final NullPointerException nullException =
                    new NullPointerException("No sunspaceSkosTopconceptURI property found.");
            log.error(nullException.getMessage(), nullException);
            throw nullException;
        }
        
        final String topConceptURIMD5 = getMD5URI(topConcept);

        final KiWiSearchCriteria criteria =
                getConceptCriteria(topConceptURI, prefix, label);

        final Set<ContentItem> result = search(criteria);
        return result;
    }

    /**
     * Searches all the controlled tags that have a given top concept and them
     * title starts with the given prefix. The specified <code>topConcept</code>
     *
     * @param topConcept the top concept, it can not be null.
     * @param labelPrefix the prefix for the label, it can not be null.
     * @return all the controlled tags that have a given top concept and them
     *         title starts with the given label.
     */
    @Override
    public Set<ContentItem> searchControlledTags(ContentItem topConcept,
            String labelPrefix) {

        if (topConcept == null) {
            final NullPointerException exception =
                    new NullPointerException("The topConcept argument can not be null");
            log.error(exception.getMessage(), exception);
            throw exception;
        }

        if (labelPrefix == null) {
            final NullPointerException exception =
                new NullPointerException("The labelPrefix argument can not be null");
            log.error(exception.getMessage(), exception);
            throw exception;
        }

        final String topConceptURIMD5 = getMD5URI(topConcept);

        final KiWiSearchCriteria criteria =
            getConceptCriteria(topConceptURIMD5, null, labelPrefix);

        final Set<ContentItem> result = search(criteria);
        return result;
    }
    
    private String getMD5URI(ContentItem item) {

        final KiWiResource resource = item.getResource();
        final String topConceptURI = ((KiWiUriResource) resource).getUri();
        final String result = MD5.md5sum(topConceptURI); 

        return result;
    }

    @Override
    public Set<ContentItem> searchFreeTags(String prefix) {
        final KiWiSearchCriteria criteria = getFreeTagCriteria(prefix);
        final KiWiSearchResults search = solrService.search(criteria);
        final List<SearchResult> resultList = search.getResults();
        final Set<ContentItem> result = new HashSet<ContentItem>();

        final String controlledTypeUri = Constants.NS_SKOS + "Concept";

        for (SearchResult items : resultList) {
            // this loop removes the controlled tags from the response set.
            // this because some tags are also SKOS and this this make
            // them to be controlled.
            // mihai : I am sure that this can be more elegant done with a SOLR
            // query but som some reasons most of the SOLR queries ends in a
            // parse exception.
            final ContentItem item = items.getItem();
            boolean isControlled = false;
            final KiWiResource resource = item.getResource();
            if (resource.isUriResource()) {
                final KiWiUriResource uriResource = (KiWiUriResource) resource;

                // for some reasons the
                // uriResource.hasType(controlledTypeUri)
                // does not works how I expect. It reurns false
                // even if the types list contains the needed 
                // URI type.
                // isControlled = uriResource.hasType(controlledTypeUri);
                isControlled = containsType(uriResource, controlledTypeUri);
            }
            if (!isControlled) {
                result.add(item);
            }
        }

        return result;
    }
    
    /**
     * proves if a given <code>KiWiUriResource</code> has a
     * certain type, the type is defined like URI. <br>
     * This is a work-around for the :
     * 
     * <pre>
     * KiWiUriResource uriResource = ...
     * String controlledTypeUri = Constants.NS_SKOS + "Concept";
     * boolean isControlled = uriResource.hasType(controlledTypeUri);
     * </pre>
     * 
     * @param uriResource the resource for the type check
     * @param typeUri the uri for the type.
     * @return true if the given resource is from the specified
     *         type.
     */
    private boolean containsType(KiWiUriResource uriResource, String typeUri) {
        final Collection<KiWiResource> types = uriResource.getTypes();

        for (KiWiResource type : types) {
            if (type.isUriResource()) {
                final KiWiUriResource uriType = (KiWiUriResource) type;
                final String uri = uriType.getUri();
                if (uri.equals(typeUri)) {
                    return true;
                }
            }
        }
        return false;
    }

    private KiWiSearchCriteria getConceptCriteria(String topConceptURI, String prefix, int level) {

        final KiWiSearchCriteria criteria =
                solrService.parseSearchString("type:skos:Concept");

// criteria.setOffset(100);
// criteria.setLimit(10);

        if (topConceptURI == null && prefix == null && level == -1) {
            return criteria;
        }

        final Map<String, Set<String>> literalProperties =
                criteria.getRdfLiteralProperties();

        final String realPrefix = prefix == null ? "*" : prefix + "*";
        final String prefixProp =
                SunSkosUtils.getPropertyName(SUN_SKOS_PREFIX);
        literalProperties.put(prefixProp, Collections.singleton(realPrefix));
        if (topConceptURI != null) {
            final String md5URI = MD5.md5sum(topConceptURI);
            final String topURIProp =
                SunSkosUtils.getPropertyName(SUN_SKOS_TOPCONCEPT_URI);
            literalProperties.put(topURIProp, Collections.singleton(md5URI));
        }

        return criteria;
    }

    private KiWiSearchCriteria getConceptCriteria(String topConceptURI, int level) {
        
        final KiWiSearchCriteria criteria =
            solrService.parseSearchString("type:skos:Concept");

// criteria.setOffset(100);
// criteria.setLimit(10);

        final Map<String, Set<String>> literalProperties =
            criteria.getRdfLiteralProperties();
        if (topConceptURI != null) {
            final String md5URI = MD5.md5sum(topConceptURI);
            final String topURIProp =
                SunSkosUtils.getPropertyName(SUN_SKOS_TOPCONCEPT_URI);
            literalProperties.put(topURIProp, Collections.singleton(md5URI));
        }

        if (level >= 0) {
            final String levelPropKey = SunSkosUtils
                    .getPropertyName(SUN_SKOS_LEVEL);
            final String strLevel = String.valueOf(level);
            literalProperties
                    .put(levelPropKey, Collections.singleton(strLevel));
        }

        return criteria;
    }

    private KiWiSearchCriteria getConceptCriteria(String topConceptURI,
            String prefix, String label) {

        final KiWiSearchCriteria criteria =
                solrService.parseSearchString("type:skos:Concept");

        final Map<String, Set<String>> literalProperties =
                criteria.getRdfLiteralProperties();

        final String topURIProp =
                SunSkosUtils.getPropertyName(SUN_SKOS_TOPCONCEPT_URI);
        literalProperties.put(topURIProp, Collections.singleton(topConceptURI));

        if (prefix != null) {
            final String prefixProp =
                SunSkosUtils.getPropertyName(SUN_SKOS_PREFIX);
            literalProperties.put(prefixProp, Collections.singleton(prefix));
        }

        final String realLabel = label == null ? "*" : label + "*";
        // FIXME : use the ConstantsNS to get the uri.
        final KiWiUriResource titleProp =
                tripleStore.createUriResourceByNamespaceTitle("kiwi", "title");
        literalProperties.put(titleProp.getUri(), Collections.singleton(realLabel));

        return criteria;
    }


    private KiWiSearchCriteria getFreeTagCriteria(String prefix) {

        final KiWiSearchCriteria criteria =
                solrService.parseSearchString("type:kiwi:Tag");

// criteria.setOffset(100);
// criteria.setLimit(10);

        if (prefix == null) {
            return criteria;
        }

        final Map<String, Set<String>> literalProperties =
                criteria.getRdfLiteralProperties();

        final String realPrefix = prefix + "*";

        // FIXME : use the ConstantsNS to get the uri.
        final KiWiUriResource titleProp =
                tripleStore.createUriResourceByNamespaceTitle("kiwi", "title");

        literalProperties.put(titleProp.getUri(),
                Collections.singleton(realPrefix));

        return criteria;
    }

    /**
     * Builds a criteria that match all the kiwi tags with a
     * title starting with a given string.
     * 
     * @deprecated use the {@link #getFreeTagCriteria(String)}.
     *             The criteria created wit this methods produces
     *             a SORL PARSE exception.
     * @param prefix the prefix for the tag title.
     * @return a criteria that match all the kiwi tags with the
     *         title starting with a given criteria.
     */
    @Deprecated
    private KiWiSearchCriteria getTagCriteria(String prefix) {
// Mihai : this code produces a SOLR Paerser prblem.
// I don't have time to investigate it - I create an unit
// test for it - see SunTagServiceTest - testSearchTag
        final String searchWithPrefix =
                "type:kiwi:Tag title:\"" + prefix + "*\"";
        final String searchWithoutPrefix = "type:kiwi:Tag";
        final KiWiSearchCriteria criteria =
                solrService.parseSearchString(prefix == null
                        ? searchWithoutPrefix
                        : searchWithPrefix);

        return criteria;
    }

    @Override
    public Set<ContentItem> getAllTags() {
        final List<ContentItem> allTags = taggingService.getTaggingResources();

        final Set<ContentItem> result = listToSet(allTags);
        return result;
    }

    private Set<ContentItem> listToSet(final List<ContentItem> allTags) {
        final Set<ContentItem> result =
                new HashSet<ContentItem>(allTags.size());
        for (ContentItem item : allTags) {
            result.add(item);
        }
        return result;
    }

    @Override
    public Set<ContentItem> getAllTags(ContentItem contentItem) {
        final List<ContentItem> tags = taggingService.getTags(contentItem);
        final Set<ContentItem> result = listToSet(tags);
        return result;
    }

    /**
     * @param criteria
     * @return
     */
    private Set<ContentItem> search(final KiWiSearchCriteria criteria) {
        
        // FIXME : this can create performance problems for big data quantities
        // do paging !
        criteria.setOffset(0);
        criteria.setLimit(1000);

        final KiWiSearchResults search = solrService.search(criteria);
        final List<SearchResult> resultList = search.getResults();
        final Set<ContentItem> result = new HashSet<ContentItem>();
        for (SearchResult items : resultList) {
            final ContentItem item = items.getItem();
            result.add(item);
        }
        


        return result;
    }

    /*
     * (non-Javadoc)
     * @see kiwi.api.sun.SunTagService#getChildren()
     */
    @Override
    public Set<ContentItem> getChildren(ContentItem item) {

        if (item == null) {
            throw new NullPointerException("The item argument can not be null");
        }

        // mihai : I choose this because the skosService.narrow method hangs
        // FIXME : unit test for skosService.narrow
        final Set<ContentItem> result = getChildrenFor(item);
        return result;
    }

    /**
     * Returns all narrower items for the given <code>ContentItem</code>. The
     * specified content item must be from type SKOSConcept.
     * 
     * @param item
     *            the involved content item, can not be null.
     * @return the narrower items for the given <code>ContentItem</code> or null
     *         if the given <code>ContentItem</code> does not have a broader.
     */
    private Set<ContentItem> getChildrenFor(ContentItem item) {

        if (item == null) {
            final NullPointerException ex = new NullPointerException(
                    "The specified item csn not be null");
            log.error(ex.getMessage(), ex);
            throw ex;
        }

        final SKOSConcept skosConcept = kiwiEntityManager.createFacade(item,
                SKOSConcept.class);
        final HashSet<SKOSConcept> concepts = skosConcept.getNarrower();

        final Set<ContentItem> result = new HashSet<ContentItem>();
        for (SKOSConcept concept : concepts) {
            final ContentItem ci = concept.getDelegate();
            result.add(ci);
        }
        return result;
    }

    /**
     * Returns the broader for the given <code>ContentItem</code>.
     * The specified content item must be from type SKOSConcept.
     *
     * @param item
     *            the involved content item, can not be null.
     * @return the broader for the given <code>ContentItem</code> or null if the
     *         given <code>ContentItem</code> does not have a broader.
     */
    private ContentItem getParentFor(ContentItem item) {

        if (item == null) {
            final NullPointerException ex =
                    new NullPointerException("The specified item csn not be null");
            log.error(ex.getMessage(), ex);
            throw ex;
        }

        final SKOSConcept skosConcept = kiwiEntityManager.createFacade(item,
                SKOSConcept.class);
        final SKOSConcept broader = skosConcept.getBroader();
        if (broader == null) {
            return null;
        }

        final ContentItem result = broader.getDelegate();
        return result;
    }

    /*
     * (non-Javadoc)
     * @see kiwi.api.sun.SunTagService#getSiblings()
     */
    @Override
    public Set<ContentItem> getSiblings(ContentItem item) {

        if (item == null) {
            throw new NullPointerException("The item argument can not be null");
        }

        // mihai : I choose this because the skosService.narrow method hangs
        // FIXME : unit test for skosService.narrow
        ContentItem parent = getParentFor(item);
        if (parent == null) {
            parent = item;
        }

        final Set<ContentItem> children = getChildrenFor(parent);

        return children;
    }

    @Override
    public Set<ContentItem> searchControlledTagsForLabel(String labelPrefix) {
        return searchControlledTags((String) null, labelPrefix);
    }

    @Override
    public Set<ContentItem> searchControlledTagsForPrefix(String prefix) {
        final KiWiSearchCriteria criteria =
                solrService.parseSearchString("type:skos:Concept");

        final Map<String, Set<String>> literalProperties =
                criteria.getRdfLiteralProperties();

                    final String prefixProp =
                    SunSkosUtils.getPropertyName(SUN_SKOS_PREFIX);
            literalProperties.put(prefixProp, Collections.singleton(prefix));

        final Set<ContentItem> result = search(criteria);
        return result;
    }

    @Override
    public Set<ContentItem> searchControlledTags(String prefix, String label) {

        final KiWiSearchCriteria criteria =
                solrService.parseSearchString("type:skos:Concept");

        final Map<String, Set<String>> literalProperties =
                criteria.getRdfLiteralProperties();

        if (prefix != null) {
            final String prefixProp = SunSkosUtils.getPropertyName(SUN_SKOS_PREFIX);
            literalProperties.put(prefixProp, Collections.singleton(prefix));
        }

        final String realLabel = label == null ? "*" : label + "*";
     // FIXME : use the ConstantsNS to get the uri.
        final KiWiUriResource titleProp =
                tripleStore.createUriResourceByNamespaceTitle("kiwi", "title");
        literalProperties.put(titleProp.getUri(), Collections.singleton(realLabel));

        final Set<ContentItem> result = search(criteria);
        return result;
    }
    
    @Override
    public Set<ContentItem> searchControlledTags(ContentItem parent) {
        final KiWiSearchCriteria criteria =
            solrService.parseSearchString("type:skos:Concept");


        final Map<String, Set<String>> literalProperties =
            criteria.getRdfLiteralProperties();

        final String uriProp = SunSkosUtils.getPropertyName(SUN_SKOS_TOPCONCEPT_URI);
        String md5URI = parent.getResource().getProperty(uriProp);
        if (md5URI == null) {
            final IllegalArgumentException
                    illegalException =
                            new IllegalArgumentException("The preant argument must contain sunspaceSkosTopconceptURI property");
            log.error(illegalException.getMessage(), illegalException);
            throw illegalException;
        }

        literalProperties.put(uriProp, Collections.singleton(md5URI));

        final Set<ContentItem> result = search(criteria);
        return result;
    }

    @Override
    public ContentItem getToplevelTag(String prefix) {
        final List<SKOSConcept> topConcepts = skosService.getTopConcepts();

        for (SKOSConcept concept : topConcepts) {
            final String prefixKey = SunSkosUtils.getPropertyName(SUN_SKOS_PREFIX);
            String cpnceptPrefix = concept.getResource().getProperty(prefixKey);
            if (prefix.equals(cpnceptPrefix)) {
                return concept.getDelegate();
            }
        }
        
        return null;
    }

    /**
     * Removes a given tag, the tag is identified after the URI. <br>
     * <b>Note : </b> this methods starts a new transaction and wraps the tag
     * removal action in it. If this action fails a
     * <code>RuntimeException</code> raises and the transaction is marked like
     * roll back.
     * 
     * @param tagged
     *            the tagged document with the given tag, it can not be null.
     * @param tagURI
     *            the tag URI, it can not be null.
     * @throws NullPointerException
     *             if any argument is null.
     * @throws RuntimeException
     *             if the tag removal action fails from any reasons. This
     *             exception chains the real cause for the exception to obtain
     *             it use the <code>getCause</code> method.
     */
    @Override
    public void removeTag(ContentItem tagged, String tagURI) {

        if (tagged == null) {
            final NullPointerException exception = new NullPointerException(
                    "The tagged paramter can not be null.");
            log.error(exception.getMessage(), exception);
            throw exception;
        }

        if (tagURI == null) {
            final NullPointerException exception = new NullPointerException(
                    "The tagURI paramter can not be null.");
            log.error(exception.getMessage(), exception);
            throw exception;
        }

        // mihai : I choose BTM because the CTM are not working.
        // FIXME : try to use the CTM to emphases the portability.
        final UserTransaction transaction = Transaction.instance();
        try {
            if (!transaction.isActive()) {
                transaction.begin();
                transactionService.registerSynchronization(
                        KiWiSynchronizationImpl.getInstance(),
                        transactionService.getUserTransaction());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            final RuntimeException exception = new RuntimeException(e);
            throw exception;
        }

        taggingService.removeTaggings(tagged, new String[] {tagURI}, true);

        try {
            transaction.commit();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            final RuntimeException exception = new RuntimeException(e);
            throw exception;
        }
        Events.instance().raiseEvent("tagUpdate");
    }
}
