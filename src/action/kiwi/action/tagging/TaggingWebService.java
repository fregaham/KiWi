package kiwi.action.tagging;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import kiwi.action.tagging.pojo.JSONGetPrefiexesListResults;
import kiwi.action.tagging.pojo.JSONTAddTagsListResults;
import kiwi.action.tagging.pojo.JSONTagListResults;
import kiwi.action.tagging.pojo.JSONTaxonomiesListResults;
import kiwi.api.config.ConfigurationService;
import kiwi.api.content.ContentItemService;
import kiwi.api.equity.EquityService;
import kiwi.api.ontology.SKOSPrefixMapperService;
import kiwi.api.ontology.SKOSService;
import kiwi.api.tagging.ExtendedTagCloudEntry;
import kiwi.api.tagging.TagCloudService;
import kiwi.api.tagging.TaggingService;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.SKOSConcept;
import kiwi.model.ontology.SKOSToPrefixMapper;
import kiwi.model.tagging.Tag;
import kiwi.model.user.User;

import net.sf.saxon.expr.StringTokenIterator;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.query.algebra.Str;

import sun.nio.cs.ext.SJIS;

/**
 * WebService endpoint for tagging Path to the methods: [TaggingWS] =
 * http://localhost:8080/KiWi/seam/resource/services/widgets/tagging
 * 
 * [TaggingWS]/listTags.json?docUri=http://
 * query.json?q=StartPage&jsonpCallback=cb123
 * 
 * @author Szaby Grünwald
 * 
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
    private User currentUser;

    @In(create = true)
    private EquityService equityService;

    @In(create = true)
    private SKOSPrefixMapperService skosPrefixMapperService;

    @In(create = true)
    private SKOSService skosService;
    
    @In
    private ConfigurationService configurationService;

    public enum OrderTypes {
        ALPHA, USAGE, EQUITY
    }

    /**
     * GET webservice for listing tags for a specific resource.
     * 
     * @param docUri
     *            WS URI parameter "resource=" resource uri
     * @param order
     *            WS parameter "order=" sorting order, can be alpha, usage or
     *            equity Default is usage.
     * @param reverse
     *            WS parameter "reverse=" true or false. Default is false.
     * @return
     */
    @GET
    @Path("/listTags.json")
    @Produces("application/json")
    public Response getTags(@QueryParam("resource") String docUri,
            @QueryParam("order") @DefaultValue("usage") String order,
            @QueryParam("reverse") @DefaultValue("false") boolean reverse) {
        try {
            ContentItemService ciService = (ContentItemService) Component
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

            List<Tag> tags = taggingService.getTags(item);
            JSONTagListResults jsonRes = new JSONTagListResults();

            insertTagsToJsonResult(tags, jsonRes, item);
            jsonRes.sortTags(orderType, reverse);
            // jsonRes.setResource(docUri);
            // jsonRes.setCurrentLogin(currentUser.getLogin());
            // jsonRes.setOrder(order);
            // jsonRes.setReverse(String.valueOf(reverse));

            return Response.ok(jsonRes.toString()).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage())
                    .build();
        }
    }

    private void insertTagsToJsonResult(List<Tag> tags,
            JSONTagListResults jsonRes, ContentItem item) {
        log.info("JSON packing #0 tags", tags.size());
        TagCloudService tagCloudService = (TagCloudService) Component
                .getInstance("tagCloudService");
        LinkedList<ExtendedTagCloudEntry> tagCloud = new LinkedList<ExtendedTagCloudEntry>();
        tagCloud.addAll(tagCloudService.aggregateTags(tags));

        for (ExtendedTagCloudEntry tagCloudEntry : tagCloud) {

            String uri = ((KiWiUriResource) tagCloudEntry.getTag()
                    .getResource()).getUri();
            Long usage = taggingService.getTagUsage(tagCloudEntry.getTag());

            List<Tag> tagCloudEntryTags = taggingService
                    .getTagsByTaggedTaggingIds(item.getId(), tagCloudEntry
                            .getTag().getId());
            double tq = equityService.getTagEquity(tagCloudEntryTags.get(0));

            final ContentItem tag = tagCloudEntry.getTag();
            // mihai : I am not sure if the skos title is the same with the tag
            // title.
            final String label = tag.getTitle();
            boolean controlled = taggingService.isControlled(tagCloudEntry
                    .getTag().getResource());
            boolean isOwnTag = tagCloudEntry.isOwnTag();
            log.info("tag label: #0, uri: #1, tq: #2", label, uri, tq);
            try {
                jsonRes.addTag(uri, usage, label, tq, controlled, isOwnTag);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Webservice for creating tags for a resource
     * 
     * @param docUri
     *            WS URI parameter "resource=" resource uri
     * @param tags
     * @return
     */
    @POST
    @Path("/addTags")
    @Produces("application/json")
    public Response addTags(@QueryParam("resource") String docUri,
            @QueryParam("tags") String tags) {

        ContentItemService ciService = (ContentItemService) Component
                .getInstance("contentItemService");
        ContentItem item = ciService.getContentItemByUri(docUri);

        if (item == null) {
            final JSONTAddTagsListResults result = new JSONTAddTagsListResults();
            return Response.status(Status.NOT_FOUND).build();
        }

        if (tags == null || "".equals(tags.trim())) {
            final JSONTAddTagsListResults result = new JSONTAddTagsListResults();
            return Response.status(Status.BAD_REQUEST).build();
        }

        final StringBuilder jsons = new StringBuilder(tags);
        // removes the '[' 
        jsons.deleteCharAt(0);
        // removes the ']'
        jsons.deleteCharAt(jsons.length() - 1);
        
        final Set<String> jsonURIS = new HashSet<String>();
        final Set<String> jsonLabel = new HashSet<String>();
        for (final StringTokenizer stk = new StringTokenizer(jsons.toString(), ","); 
                stk.hasMoreElements(); ) {
            final String next = stk.nextToken();
            try {
                final JSONObject jsonObject = new JSONObject(next);
                final Object uri = jsonObject.get("uri");
                if (uri != null) {
                    jsonURIS.add(uri.toString().trim());
                }

                final Object label = jsonObject.get("label");
                if (label != null) {
                    jsonURIS.add(label.toString().trim());
                }

            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }

        final Set<ContentItem> tagsByLabel =
                taggingService.addTagsByLabel(item, jsonLabel);

        taggingService.addTagsByURI(item, jsonLabel);

        final JSONTAddTagsListResults result = new JSONTAddTagsListResults();
        try {
            final String newTags = tagsByLabel.isEmpty()
                ? null
                : tagsByLabel.toString();
            result.addTags("200", null, newTags);
        } catch (JSONException jException) {
            log.error(jException.getMessage(), jException);
        }

        return Response.ok(result.toString()).build();
    }

    /**
     * Webservice for removing existing tags belonging to a resource.
     * 
     * @param docUri
     *            WS URI parameter "resource=" resource uri
     * @param tags
     *            comma-separated list of tag URI's to remove
     * @param mode
     *            can be private of shared
     * @return
     */
    @GET
    @Path("/removeTags")
    @Produces("application/json")
    public Response removeTags(@QueryParam("resource") String docUri,
            @QueryParam("tags") String tags,
            @QueryParam("mode") @DefaultValue("private") String mode) {

        ContentItemService ciService = (ContentItemService) Component
                .getInstance("contentItemService");
        ContentItem item = ciService.getContentItemByUri(docUri);

        if (item == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        if (tags == null || "".equals(tags.trim())) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        String[] tagUris = tags.split(",");
        taggingService.removeTaggings(item, tagUris,
                (mode.toLowerCase() != "private" ? true : false));

        final JSONTAddTagsListResults result = new JSONTAddTagsListResults();
        try {
            result.addTags("200", null, null);
        } catch (JSONException jException) {
            log.error(jException.getMessage(), jException);
        }

        return Response.ok(result.toString()).build();
    }

    @GET
    @Path("/getPrefixes")
    @Produces("application/json")
    public Response getPrefixes() {
        final List<SKOSToPrefixMapper> allMappings = skosPrefixMapperService
                .getAllMappings();

        final JSONGetPrefiexesListResults result = new JSONGetPrefiexesListResults();
        for (SKOSToPrefixMapper mapper : allMappings) {
            final String prefix = mapper.getPrefix();
            try {
                result.addPrefix(prefix);
            } catch (JSONException jException) {
                log.error(jException.getMessage(), jException);
            }
        }

        return Response.ok(result.toString()).build();
    }

    @GET
    @Path("/getTaxonomies")
    @Produces("application/json")
    public Response getTaxonomies() {
        final List<SKOSToPrefixMapper> allMappings = skosPrefixMapperService
                .getAllMappings(0);

        final JSONTaxonomiesListResults results = new JSONTaxonomiesListResults();
        for (SKOSToPrefixMapper mapper : allMappings) {
            final String label = mapper.getLabel();
            final String prefix = mapper.getPrefix();
            try {
                results.addTaxonomies(label, prefix, false);
            } catch (JSONException jException) {
                log.error(jException.getMessage(), jException);
            }
        }
        
        return Response.ok(results.toString()).build();
    }

    @GET
    @Path("/searchTags")
    @Produces("application/json")
    public Response searchTags(@QueryParam("q") String q) {

        final int indexOf = q.indexOf(":"); 
        final String prefix = q.substring(0, indexOf).trim();

        log.debug("Try to process #0", q);
        final boolean noQuery = indexOf == -1;
        final Set<SKOSConcept> concepts;
        if (noQuery) {
            // this block if the search quiery looks like this : 
            // 'prefix:', in this case the label is missing.
            // according with the specification the Information Extraction
            // configuration is used to define pairs top level concept level.
            // the search care about this mapping and it searches for all
            // concepts with :
            // 1.a specified top level concept (specified in the IE Config)
            // 2.a given nesting level (specified in the IE Config)
            // 3.a certain prefix specified in the search query.
            final Map<String, Integer> noQueryParam = getNoQueryParam();

            concepts = new HashSet<SKOSConcept>();
            for (Entry<String, Integer> conf :  noQueryParam.entrySet()) {
                final String uri = conf.getKey();
                final Integer level = conf.getValue();
                final Set<SKOSConcept> allConcepts = skosPrefixMapperService.getAllConcepts(uri, prefix, level);
                concepts.addAll(allConcepts);
            }
        } else {
            final String query = q.substring(indexOf + 1, q.length()).trim();
            // This pattern matches all the string that starts with the given
            // prefix.
            final String jpqlLikePattern = query + "%";
            concepts = skosPrefixMapperService.getConcepts(prefix, jpqlLikePattern);
        }

        // FIXME : skos concepts are also tags :)
        final JSONTagListResults jsonRes = new JSONTagListResults();
        for (SKOSConcept concept : concepts) {
            final String uri = ((KiWiUriResource) concept.getResource())
                    .getUri();
            final ContentItem item = concept.getDelegate();
            final Long usage = 0l;

            double tq = equityService.getTagEquity(item);

            final String label = item.getTitle();
            // mihai : I am not sure if this is the right way to obtain the
            // controlled attribute.
            boolean controlled = taggingService.isControlled(concept
                    .getResource());
            // FIXME : this is wrong, obtian the isOwnTag from somewhere
            // FIXME : I am not sure if I need this attribute.
            boolean isOwnTag = false;
            log.info("tag label: #0, uri: #1, tq: #2", label, uri, tq);

            try {
                jsonRes.addTag(uri, usage, label, tq, controlled, isOwnTag);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }

        return Response.ok(jsonRes.toString()).build();
    }
    
    

    /**
     * Returns a map that contains like key a top concept 
     * URI and like value the nesting level. This setting was 
     * done in the IEConfigurationAction. 
     *  
     * @return a map that contains like key a top concept 
     * URI and like value the nesting level. If this map is 
     *  empty then no configuration was done.
     */
    private Map<String, Integer> getNoQueryParam() {
        final Map<String, Integer> result = new HashMap<String, Integer>();
        final String key = "kiwi.informationextraction.taxonomyConcepts";
        final List<String> listConfiguration = configurationService.getListConfiguration(key);
        for (String conf : listConfiguration) {
            final StringTokenizer tokenizer = new StringTokenizer(conf);
            final String concept = tokenizer.nextToken();
            final String topConcept = tokenizer.nextToken();
            final String nestingLevel = tokenizer.nextToken();
            result.put(topConcept, Integer.parseInt(nestingLevel));
        }

        return result;
    }
}
