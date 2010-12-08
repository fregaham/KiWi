

package kiwi.webservice;


import java.util.*;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import kiwi.api.config.ConfigurationService;
import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.equity.EquityService;
import kiwi.api.ontology.SKOSService;
import kiwi.api.sun.SunTagMapperService;
import kiwi.api.sun.SunTagService;
import kiwi.api.tagging.ExtendedTagCloudEntry;
import kiwi.api.tagging.TagCloudService;
import kiwi.api.tagging.TaggingService;
import kiwi.api.transaction.TransactionService;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.SKOSConcept;
import kiwi.model.tagging.Tag;
import kiwi.service.sun.SunSkosUtils;
import kiwi.service.transaction.KiWiSynchronizationImpl;
import kiwi.webservice.utility.sun.PrefiexesListResult;
import kiwi.webservice.utility.sun.RemoveTagResponse;
import kiwi.webservice.utility.sun.TagsListResult;
import kiwi.webservice.utility.sun.TaxonomiesListResult;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.jboss.seam.transaction.Transaction;
import org.jboss.seam.transaction.UserTransaction;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sun.print.SunMinMaxPage;


/**
 * WebService endpoint for tagging Path to the methods:
 * [TaggingWS] =
 * http://localhost:8080/KiWi/seam/resource/services
 * /widgets/tagging [TaggingWS]/listTags.json?docUri=http://
 * query.json?q=StartPage&jsonpCallback=cb123
 * 
 * @author Szaby Grünwald
 */
@Name("kiwi.webservice.TaggingWebService")
@Scope(ScopeType.STATELESS)
@Path("/widgets/tagging")
public class TaggingWebService {

    @Logger
    private static Log log;

    @In(create = true)
    private TaggingService taggingService;

    @In(create = true)
    private EquityService equityService;

    @In
    private SunTagService sunTagService;

    @In
    private ConfigurationService configurationService;

    @In
    private ContentItemService contentItemService;

    @In
    private KiWiEntityManager kiwiEntityManager;
    
    @In
    private EntityManager entityManager;

    @In
    private TransactionService transactionService;


    public enum OrderTypes {
        ALPHA, USAGE, EQUITY
    }

    /**
     * GET webservice for listing tags for a specific resource.
     * The example does not corespond with the specs.
     * 
     * <pre>
     * {
     * "resource":"URI-of-tagged-resource",
     * "items":
     *  [
     *   {
     *    "uri":"http://kiwi/tags/1234",
     *    "usage":262,
     *    "label":"DEV",
     *    "tq":2263,
     *    "controlled":1,
     *    "prefix":"community",
     *    "parent":"http://...",
     *    "hasChildren":true
     *   },
     *   {
     *    "uri":"http://kiwi/tags/1235",
     *    "usage":11
     *    "label":"web2.0",
     *    "tq":-72,
     *    "controlled":0
     *   },
     *   ...
     *  ]
     * }
     * 
     * </pre>
     * 
     * @param docUri WS URI parameter "resource=" resource uri
     * @param order WS parameter "order=" sorting order, can be
     *            alpha, usage or equity Default is usage.
     * @param reverse WS parameter "reverse=" true or false.
     *            Default is false.
     * @return
     */
    @GET
    @Path("/listTags.json")
    @Produces("application/json")
    public Response getTags(@QueryParam("resource") String docUri,
            @QueryParam("order") @DefaultValue("usage") String order,
            @QueryParam("reverse") @DefaultValue("false") boolean reverse) {
        try {
            ContentItemService ciService =
                    (ContentItemService) Component
                            .getInstance("contentItemService");
            ContentItem item = ciService.getContentItemByUri(docUri);

            if (item == null) {
                throw new WebApplicationException(Response.status(
                        Status.NOT_FOUND).build());
            }

            OrderTypes orderType = OrderTypes.ALPHA;
            if ("alpha".equals(order)) {
                orderType = OrderTypes.ALPHA;
            } else {
                if ("usage".equals(order)) {
                    orderType = OrderTypes.USAGE;
                } else {
                    if ("equity".equals(order)) {
                        orderType = OrderTypes.EQUITY;
                    } else {
// 400 "order type " + order + " is undefined.")
                        return Response
                                .status(Status.BAD_REQUEST)
                                .entity("400 - order type " + order
                                        + " is undefined.").build();
                    }
                }
            }

            final Set<ContentItem> allTags = sunTagService.getAllTags(item);
            final TagsListResult jsonRes = new TagsListResult();

            jsonRes.setReource(((KiWiUriResource) item.getResource()).getUri());

            addItemsToResponse(jsonRes, allTags);

            jsonRes.sortTags(orderType, reverse);

            return Response.ok(jsonRes.toString()).build();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage())
                    .build();
        }
    }

    /**
     * Adds all the the need information from a content item to a
     * JOSN object.
     * 
     * @param jsonRes
     * @param ci
     * @throws JSONException
     */
    private void addFreeTag(final TagsListResult jsonRes, ContentItem ci)
            throws JSONException {
        // JSON label
        final String title = ci.getTitle();
        final KiWiResource resource = ci.getResource();

        // JSON uri
        final String uri = ((KiWiUriResource) resource).getUri();

        // JSON controlled
        // I presume that 0 means false - no Controlled == Free
        // Tag
        final String controled = "0";

        // JSON usage
        // FIXME : find the right usage here.
        final Long usage = 0l;

        // JSON equity
        double tq = 0;

        jsonRes.addFreeTag(uri, usage, title, tq);
        log.debug("Add Free Tag : #0", jsonRes);
    }

    /**
     * @param jsonRes
     * @param ci
     * @throws JSONException
     */
    private void addControlledTag(final TagsListResult jsonRes, ContentItem ci)
            throws JSONException {
        final String title = ci.getTitle();
        final KiWiResource resource = ci.getResource();

        // JSON uri
        final String uri = ((KiWiUriResource) resource).getUri();

        // JSON controlled
        final String controled = "1";
        // mihai - i choose this because the solrService does not
        // run
        // todo : write a test
        final ContentItem parent = getParent(ci);

        final KiWiUriResource kiwiResourceParent =
                parent == null
                        ? (KiWiUriResource) ci.getResource()
                        : (KiWiUriResource) parent.getResource();

        // JSON parent
        final String parentURI = kiwiResourceParent.getUri();
        // JSON equity hasChildren
        final boolean hasChildren = hasChildern(ci);

        // JSON usage
        // FIXME : find the right usage here.
        final Long usage = 0l;

        // JSON equity
        double tq = 0;

        final String prefixProp =
                SunSkosUtils.getPropertyName("sunspaceSkosPrefix");
        final String realPrefix = resource.getProperty(prefixProp);

        jsonRes.addControlledTag(uri, usage, title, tq, realPrefix, parentURI,
                hasChildren);
        log.debug("Add Controlled Tag : #0", jsonRes);

    }

    private ContentItem getParent(ContentItem item) {
        final SKOSConcept facade =
                kiwiEntityManager.createFacade(item, SKOSConcept.class);
        final SKOSConcept broader = facade.getBroader();

        return broader == null ? item : broader.getDelegate();
    }

    private boolean hasChildern(ContentItem item) {
        final SKOSConcept facade =
                kiwiEntityManager.createFacade(item, SKOSConcept.class);
        final HashSet<SKOSConcept> narrower = facade.getNarrower();
        return !narrower.isEmpty();
    }

    private void insertTagsToJsonResult(List<Tag> tags, TagsListResult jsonRes,
            ContentItem item) {
        log.info("JSON packing #0 tags", tags.size());
        TagCloudService tagCloudService =
                (TagCloudService) Component.getInstance("tagCloudService");
        LinkedList<ExtendedTagCloudEntry> tagCloud =
                new LinkedList<ExtendedTagCloudEntry>();
        // tagCloud.addAll(tagCloudService.aggregateTagsByCI(item)
        // aggregateTags(tags));

        for (ExtendedTagCloudEntry tagCloudEntry : tagCloud) {

            String uri =
                    ((KiWiUriResource) tagCloudEntry.getTag().getResource())
                            .getUri();
            Long usage = taggingService.getTagUsage(tagCloudEntry.getTag());

            List<Tag> tagCloudEntryTags =
                    taggingService.getTagsByTaggedTaggingIds(item.getId(),
                            tagCloudEntry.getTag().getId());
            double tq = equityService.getTagEquity(tagCloudEntryTags.get(0));

            final ContentItem tag = tagCloudEntry.getTag();
            // mihai : I am not sure if the skos title is the
            // same with the tag title.
            final String label = tag.getTitle();
            log.info("tag label: #0, uri: #1, tq: #2", label, uri, tq);
            try {
                jsonRes.addFreeTag(uri, usage, label, tq);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Webservice for creating tags for a resource.<br>
     * It produces something similar with the next code snippet :
     * 
     * <pre>
     * {
     * "resource":"URI-of-tagged-resource",
     * "items":
     *  [
     *   {
     *    "uri":"http://kiwi/tags/1234",
     *    "usage":262,
     *    "label":"community:DEV",
     *    "tq":2263,
     *    "controlled":0
     *   },
     *   {
     *    "uri":"http://kiwi/tags/1235",
     *    "usage":11
     *    "label":"community:CEPEDIA",
     *    "tq":-72,
     *    "controlled":0
     *   },
     *   ...
     *  ],
     * }
     * 
     * </pre>
     * 
     * @param docUri WS URI parameter "resource=" resource uri.
     * @param tags
     * @return
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Path("/addTags")
    @Produces("application/json")
    public Response addTags(@FormParam("resource") String docUri,
            @FormParam("tags") String tags) {

        ContentItemService ciService =
                (ContentItemService) Component
                        .getInstance("contentItemService");
        ContentItem item = ciService.getContentItemByUri(docUri);

        if (item == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        if (tags == null || "".equals(tags.trim())) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        final Set<String> jsonURIS = new HashSet<String>();
        final Set<String> jsonLabel = new HashSet<String>();

        JSONArray ar;
        try {
            ar = new JSONArray(tags);

            for (int i = 0; i < ar.length(); i++) {
                final JSONObject jsonObject = ar.getJSONObject(i);

                if (jsonObject.has("uri")) {
                    final Object uri = jsonObject.get("uri");
                    jsonURIS.add(uri.toString().trim());
                }

                if (jsonObject.has("label")) {
                    final Object label = jsonObject.get("label");
                    jsonLabel.add(label.toString().trim());
                }

            }

        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        final TagsListResult jsonRes= new TagsListResult();
        // Add the tags
        final Set<ContentItem> tagsByLabel =
                taggingService.addTagsByLabel(item, jsonLabel);
        
        addItemsToResponse(jsonRes, tagsByLabel);

        taggingService.addTagsByURI(item, jsonURIS);

        final Set<ContentItem> uriTags = new HashSet<ContentItem>();
        for (String uri : jsonURIS) {
            final ContentItem tagByUri =
                    contentItemService.getContentItemByUri(uri);
            uriTags.add(tagByUri);
        }
        addItemsToResponse(jsonRes, uriTags);

        log.debug("Addeed tags #0", jsonRes);

        return Response.ok(jsonRes.toString()).build();
    }

    /**
     * Webservice for removing existing tags belonging to a
     * resource.<br>
     * The response is not specified in the specifications.
     * http://www.kiwi-community
     * .eu/display/sunspace/Tagging+UC+Specification
     * #TaggingUCSpecification -AddTag%28s%29Request For more
     * information about JSON response please consult the
     * SunTaggingRemoveTagResponse.
     * 
     * @param docUri WS URI parameter "resource=" resource uri
     * @param tags comma-separated list of tag URI's to remove
     * @param mode can be private of shared
     * @return
     */
    @POST
    @Path("/removeTags")
    @Produces("application/json")
    public Response removeTags(@FormParam("resource") String docUri,
            @FormParam("tagURI") String tagURI) {

        final ContentItem item = contentItemService.getContentItemByUri(docUri);

        if (item == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        if (tagURI == null || "".equals(tagURI.trim())) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        final RemoveTagResponse jsonRes;
        try {
            jsonRes = new RemoveTagResponse();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        sunTagService.removeTag(item, tagURI);

//        final UserTransaction transaction = Transaction.instance();
//        try {
//            if (!transaction.isActive()) {
//                transaction.begin();
//                transactionService.registerSynchronization(
//                        KiWiSynchronizationImpl.getInstance(),
//                        transactionService.getUserTransaction());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        taggingService.removeTaggings(item, new String[] {tagURI}, true);
//
//        try {
//            transaction.commit();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        try {
            jsonRes.addTag(docUri, tagURI);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            return Response.status(Status.BAD_REQUEST).build();
        }


        return Response.ok(jsonRes.toString()).build();
    }

    /**
     * Web service used to list all the prefixes assigned to SKOS
     * concepts and pack them in JSON objects - one JSON object
     * for every concept.<br>
     * This WS is GET http method.The format follow the next code
     * snippet :
     * 
     * <pre>
     * [{
     *     "prefix":"region",
     *  },{
     *     "prefix":"bu",
     * }]
     * </pre>
     * 
     * @see <a href=
     *      "http://www.kiwi-community.eu/display/sunspace/Tagging+WebServices+Umbrella#TaggingWebServicesUmbrella-GetPrefixes"
     *      >Tagging WebServices Umbrella</a>
     * @return a list of JSON objects which follows the upper
     *         defined format.
     */
    @GET
    @Path("/getPrefixes")
    @Produces("application/json")
    public Response getPrefixes() {
        final Set<String> prefixes = sunTagService.getPrefixes();
        final PrefiexesListResult result = new PrefiexesListResult();
        for (String prefix : prefixes) {
            try {
                result.addPrefix(prefix);
            } catch (JSONException jException) {
                log.error(jException.getMessage(), jException);
            }
        }

        return Response.ok(result.toString()).build();
    }

    /**
     * Web service used to list all the top level SKOS concepts
     * and pack them in JSON objects - one JSON object for every
     * concept.<br>
     * This WS is GET http method. <br>
     * The URI for this web service is : <KIWI_URL>
     * /team/resource/services /widgets/tagging/getTaxonomies <br>
     * The JSON response format is like in next snippet :
     * 
     * <pre>
     * [{
     *     "label": "Region",
     *     "prefix":"region",
     *     "required": true
     *  },{
     *     "label": "Business Unit"
     *     "prefix":"bu",
     *     "required": false
     * }]
     * </pre>
     * 
     * @see <a href=
     *      "http://www.kiwi-community.eu/display/sunspace/Tagging+WebServices+Umbrella#TaggingWebServicesUmbrella-GetTaxonomies"
     *      >Tagging WebServices Umbrella</a>
     * @return a list of JSON objects which contains all top
     *         level concepts, one JSON object for each top
     *         concept.
     */
    @GET
    @Path("/getTaxonomies")
    @Produces("application/json")
    public Response getTaxonomies() {
        final Set<SKOSConcept> taxonomies = sunTagService.getTaxonomies();
        final TaxonomiesListResult results = new TaxonomiesListResult();
        for (SKOSConcept concept : taxonomies) {
            final ContentItem delegate = concept.getDelegate();

            final String title = delegate.getTitle();
            final String prefixProp = getPropertyName("sunspaceSkosPrefix");
            final String prefix =
                    delegate.getResource().getProperty(prefixProp);

            final String requiredProp = getPropertyName("sunspaceSkosRequired");
            final String required =
                    delegate.getResource().getProperty(requiredProp);
            final String uri = ((KiWiUriResource)concept.getDelegate().getResource()).getUri();
            
            try {
                final boolean req =
                        required == null ? false : Boolean
                                .parseBoolean(required);
                results.addTaxonomies(title, prefix, req, uri);
            } catch (JSONException jException) {
                log.error(jException.getMessage(), jException);
            }
        }

        return Response.ok(results.toString()).build();
    }

    private String getPropertyName(String property) {
        final StringBuilder result = new StringBuilder();
        result.append(Constants.NS_SUN_SPACE);
        result.append("/SunSpaceSkosConcept/");
        result.append(property);
        return result.toString();
    }

    private Response searchTagsForLabelPrefix(String prefix)
            throws JSONException {
        log.debug("Search tags for prefix #0", prefix);

        final Set<ContentItem> controlledTags =
                sunTagService.searchControlledTagsForLabel(prefix);
        final TagsListResult jsonRes = new TagsListResult();

        log.debug("Try to add #0 controlled tags.", controlledTags.size());

        for (ContentItem ci : controlledTags) {
            addControlledTag(jsonRes, ci);
        }

        log.debug("Controlled tags succesfully added.");

        final Set<ContentItem> freeTags = sunTagService.searchFreeTags(prefix);
        log.debug("Try to add  #0 free tags.", freeTags.size());
        for (ContentItem ci : freeTags) {
            addFreeTag(jsonRes, ci);
        }

        log.debug("Free tags succesfully added.");

        final Response result = Response.ok(jsonRes.toString()).build();

        return result;
    }

    /**
     * Web service used to search all the tags (controlled or
     * free) and pack them in JSON objects - one JSON object for
     * every tag.<br>
     * This WS is GET http method. The JSON format is like in
     * next snippet :
     * 
     * <pre>
     * {
     * "resource":"URI-of-tagged-resource",
     * "items":
     *  [
     *   {
     *    "uri":"http://kiwi/tags/1234",
     *    "usage":262,
     *    "label":"DEV",
     *    "tq":2263,
     *    "controlled":1,
     *    "prefix":"community",
     *    "parent":"http://...",
     *    "hasChildren":true
     *   },
     *   {
     *    "uri":"http://kiwi/tags/1235",
     *    "usage":11
     *    "label":"web2.0",
     *    "tq":-72,
     *    "controlled":0
     *   },
     *   ...
     *  ]
     * }
     * </pre>
     * 
     * The upper JSON is for a controlled tag and the bottom one
     * is for a free tag. For more information consult the :
     * http://www.kiwi-community.eu/display
     * /sunspace/Tag+Completer+web+services+specification
     * #TagCompleterwebservicesspecification-GetPrefixesRequest
     * 
     * @param q
     * @return
     */
    @GET
    @Path("/searchTags")
    @Produces("application/json")
    public Response searchTags(@QueryParam("q") String q) {
        log.debug("Try to process #0", q);
        final int indexOf = q.indexOf(":");
        final boolean noSeparator = indexOf == -1;
        final String prefix =
                noSeparator ? q.trim() : q.substring(0, indexOf).trim();

        if (noSeparator) {
            log.debug("No seprarator -all tags (both free and controlled ones) with labels starting with prefix ");
// case 6:)
// there is no ':' in the query -> the user has just entered some string 
            Response searchTagsForPrefix;
            try {
                searchTagsForPrefix = searchTagsForLabelPrefix(prefix);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
                return Response.status(Status.BAD_REQUEST).build();
            }
            return searchTagsForPrefix;
        }


        final String label = q.substring(indexOf + 1, q.length()).trim();


        if (label.isEmpty()) {
            Map<String, Integer> noQueryParam = null;
            // HACK (hot fix): This is a hack for case 1: We should only look for
            // business rule, if the given prefix denotes a top level prefix;
            // Also, this should only look for business rules for given taxonomy
            // The original code here was uniformely consulting the business rule
            // regardless of the prefix and relationship of a business rule to a taxonomy
            if ("geo".equals(prefix)) {
                noQueryParam = getNoQueryParam();
            }

            if ((noQueryParam != null) && (!noQueryParam.isEmpty())) {
                //(case 1:) 
                // If a business rule is defined, return all concepts from 
                // the level defined in business rule.
                final Set<ContentItem> tags = new HashSet<ContentItem>();
                for (Entry<String, Integer> conf : noQueryParam.entrySet()) {
                    final String uri = conf.getKey();
                    final Integer level = conf.getValue();
                    final Set<ContentItem> result =
                            sunTagService.searchControlledTags(uri, level);
                    tags.addAll(result);
                }

                // leave case 1
                final TagsListResult jsonRes = new TagsListResult();
                addControlledTags(tags, jsonRes);
                log.debug("Search result for [#0] is #1", q, jsonRes);
                return Response.ok(jsonRes.toString()).build();
            }

            final ContentItem toplevelTag = sunTagService.getToplevelTag(prefix);
            if (toplevelTag != null) {
                // case 2
                // If a business rule is not defined,
                // return all concepts from ALL levels.
                final Set<ContentItem> tags =
                        sunTagService.searchControlledTags(toplevelTag);

                // if a tag has prefix then it also can be included in a
                // hierarchical structure and this makes it controlled.
                final TagsListResult jsonRes = new TagsListResult();
                addControlledTags(tags, jsonRes);

             // leave case 2
                log.debug("Search result for [#0] is #1", q, jsonRes);
                return Response.ok(jsonRes.toString()).build();
            }

            if (toplevelTag == null) {
                // redundant if check :)
                // case 3
                // if the prefix is not a root level prefix 
                // (e.g. "country", "region") then return ALL
                // tags from this prefix's level (e.g. all countries, or all regions)
                // The reason why I don't search also for level is the relation
                // between level and prefix. Each prefix is assigned to a level
                // and all the prefixes must be unique -> there is a direct relation
                // between level and prefix.
                final Set<ContentItem> tags =
                    sunTagService.searchControlledTagsForPrefix(prefix);

                // if a tag has prefix then it also can be included in a
                // hierarchical structure and this makes it controlled.
                final TagsListResult jsonRes = new TagsListResult();
                addControlledTags(tags, jsonRes);

                // leave case 3
                log.debug("Search result for [#0] is #1", q, jsonRes);
                return Response.ok(jsonRes.toString()).build();
            }
        } else {
            // label and prefix are present.
            final ContentItem toplevelTag = sunTagService.getToplevelTag(prefix);
            if (toplevelTag != null) {
                // (case 4:) 
                // if the prefix is a root level prefix (e.g. "geo"),
                // return all concepts from ALL levels,
                // that start with "label"

                // if a tag has prefix then it also can be included in a
                // hierarchical structure and this makes it controlled.
                final Set<ContentItem> tags =
                        sunTagService.searchControlledTags(toplevelTag, label);
                final TagsListResult jsonRes = new TagsListResult();
                addControlledTags(tags, jsonRes);

                // leave case 5
                log.debug("Search result for [#0] is #1", q, jsonRes);
                return Response.ok(jsonRes.toString()).build();
            }

            if (toplevelTag == null) {
                // redundant if check :)
                //  //(case 5) 
                // if the prefix is not a root level prefix 
                // (e.g. "country", "region") then return ALL
                // tags from this prefix's level that start with "label"

                // if a tag has prefix then it also can be included in a 
                // hierarchical structure and this makes it controlled.

                // The reason why I don't search also for level is the relation
                // between level and prefix. Each prefix is assigned to a level
                // and all the prefixes must be unique -> there is a direct relation
                // between level and prefix.

                final Set<ContentItem> tags =
                        sunTagService.searchControlledTags(prefix, label);
                final TagsListResult jsonRes = new TagsListResult();
                addControlledTags(tags, jsonRes);

                // leave case 5
                log.debug("Search result for [#0] is #1", q, jsonRes);
                return Response.ok(jsonRes.toString()).build();
            }
        }

        final IllegalStateException exception =
                new IllegalStateException("This " + q + " can not be processed");
        log.error(exception.getMessage(), exception);
        throw exception;
    }
    
    private void addControlledTags(Set<ContentItem> tags, TagsListResult result) {
        for (ContentItem item : tags) {
            try {
                addControlledTag(result, item);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
// mihai : in this way I only skip only the items with problems.
            }
        }
    }

    /**
     * Returns a map that contains like key a top concept URI and
     * like value the nesting level. This setting was done in the
     * IEConfigurationAction.
     * 
     * @return a map that contains like key a top concept URI and
     *         like value the nesting level. If this map is empty
     *         then no configuration was done.
     */
    private Map<String, Integer> getNoQueryParam() {
        final Map<String, Integer> result = new HashMap<String, Integer>();
        final String key = "kiwi.informationextraction.taxonomyConcepts";
        final List<String> listConfiguration =
                configurationService.getListConfiguration(key);
        for (String conf : listConfiguration) {
            final StringTokenizer tokenizer = new StringTokenizer(conf);
            final String concept = tokenizer.nextToken();
            final String topConcept = tokenizer.nextToken();
            final String nestingLevel = tokenizer.nextToken();
            result.put(topConcept, Integer.parseInt(nestingLevel));
        }

        return result;
    }

    /**
     * Returns all the children for a given content item, the
     * content item is identified after its URI. The response is
     * a list of JSON objects, one for each child. The response
     * depends on the if the wrapped child, the wrapped child can
     * be free or controlled tag. <br>
     * The URI for this web service is : <KIWI_URL>
     * /team/resource/services /widgets/tagging/getChidlern <br>
     * This WS is GET http method. The JSON format is like in
     * next snippet :
     * 
     * <pre>
     * {
     * "resource":"URI-of-tagged-resource",
     * "items":
     *  [
     *   {
     *    "uri":"http://kiwi/tags/1234",
     *    "usage":262,
     *    "label":"DEV",
     *    "tq":2263,
     *    "controlled":1,
     *    "prefix":"community",
     *    "parent":"http://...",
     *    "hasChildren":true
     *   },
     *   {
     *    "uri":"http://kiwi/tags/1235",
     *    "usage":11
     *    "label":"web2.0",
     *    "tq":-72,
     *    "controlled":0
     *   },
     *   ...
     *  ]
     * }
     * </pre>
     * 
     * @see <a href=
     *      "http://www.kiwi-community.eu/display/sunspace/Tagging+WebServices+Umbrella#TaggingWebServicesUmbrella-GetChildren"
     *      >Tagging WebServices Umbrella</a>
     * @param ciUri the uri for the parent document.
     * @return all the children for a given content item like
     *         JSON objects.
     */
    @GET
    @Path("/getChildren")
    @Produces("application/json")
    public Response getChidlren(@QueryParam("uri") String ciUri) {

        final TagsListResult jsonRes = new TagsListResult();

        final ContentItem contentItemByUri =
                contentItemService.getContentItemByUri(ciUri);
        final Set<ContentItem> children =
                sunTagService.getChildren(contentItemByUri);

        addItemsToResponse(jsonRes, children);

        return Response.ok(jsonRes.toString()).build();
    }

    /**
     * @param jsonRes
     * @param items
     */
    private void addItemsToResponse(TagsListResult jsonRes,
            Collection<ContentItem> items) {

        // FIXME : move this logic on the service.
        final String controlledTypeUri = Constants.NS_SKOS + "Concept";
        for (ContentItem ci : items) {
            boolean isControlled = false;
            final KiWiResource resource = ci.getResource();
            if (resource.isUriResource()) {
                final KiWiUriResource uriResource = (KiWiUriResource) resource;

                // for some reasons the 
                // uriResource.hasType(controlledTypeUri)
                // does not works how I expect. It reurns false
                // even if the types list contains the needed 
                // URI type.
                // isControlled = uriResource.hasType(controlledTypeUri);
                isControlled = containsType(uriResource, controlledTypeUri);

                try {
                    if (isControlled) {
                        addControlledTag(jsonRes, ci);
                    } else {
                        addFreeTag(jsonRes, ci);
                    }
                } catch (JSONException e) {
                    log.error(
                            "The Content Item with title :#0 can be transormed in JSON.",
                            ci.getTitle());
                    log.error(e.getMessage(), e);
                }
            }
        }
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

    /**
     * @param ciUri
     * @return
     */
    @GET
    @Path("/getSiblings")
    @Produces("application/json")
    public Response getSiblings(@QueryParam("uri") String ciUri) {
        TagsListResult jsonRes = new TagsListResult();

        final ContentItem contentItemByUri =
                contentItemService.getContentItemByUri(ciUri);
        final Set<ContentItem> sibItems =
                sunTagService.getSiblings(contentItemByUri);
        addItemsToResponse(jsonRes, sibItems);

        return Response.ok(jsonRes.toString()).build();
    }

}
