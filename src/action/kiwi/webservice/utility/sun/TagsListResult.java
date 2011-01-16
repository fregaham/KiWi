/**
 * 
 */


package kiwi.webservice.utility.sun;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kiwi.webservice.TaggingWebService.OrderTypes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * JSON object used to transport informations about tags. It is
 * used by most of the services where the response must contains
 * a Tag list. <br>
 * The response can contain two kinds of tags, free and
 * controlled. The format follow the next code snippet :
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
 *      "http://www.kiwi-community.eu/display/sunspace/Tagging+UC+Specification#TaggingUCSpecification-GetTagsResponse"
 *      >Tagging WebServices Umbrella</a>
 */
public class TagsListResult  {

    /**
     * Here are all the JOSN objects stored.
     */
    private JSONObject mainObject;

    /**
     * All the items.
     */
    private List<Map<String, Object>> jsonObjects;

    public TagsListResult()  {
        mainObject = new JSONObject();
        jsonObjects = new ArrayList<Map<String, Object>>();
    }

    public void setReource(String uri) throws JSONException {
        mainObject.put("resource", uri);
    }
    
    

    /**
     * Produces a JSON response for an add controlled tag action.
     * The response looks like this:
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
     *   ...
     *  ]
     * }
     * </pre>
     * 
     * @param uri
     * @param usage
     * @param label
     * @param tq
     * @param prefix
     * @param parent
     * @param hasChildren
     * @throws JSONException
     * @see <a href=
     *      "http://www.kiwi-community.eu/display/sunspace/Tagging+UC+Specification#TaggingUCSpecification-GetTagsResponse"
     *      >Tagging WebServices Umbrella</a>
     */
    public void addControlledTag(String uri, Long usage, String label,
            double tq, String prefix, String parent, boolean hasChildren)
            throws JSONException {
        final Map<String, Object> jsonObject = new HashMap<String, Object>();
        jsonObject.put("uri", uri);
        jsonObject.put("label", label);
        jsonObject.put("usage", usage);
        jsonObject.put("tq", tq);
        jsonObject.put("prefix", prefix);
        jsonObject.put("parent", parent);
        jsonObject.put("controlled", 1);
        jsonObject.put("hasChildren", hasChildren);
        
        jsonObjects.add(jsonObject);
    }

    /**
     * Produces a JSON response for an add free tag action. The
     * response looks like this:
     * 
     * <pre>
     * {
     * "resource":"URI-of-tagged-resource",
     * "items":
     *  [
     *   {
     *    "uri":"http://kiwi/tags/1235",
     *    "usage":11
     *    "label":"web2.0",
     *    "tq":-72,
     *    "controlled":0,
     *    "hasChildren": false
     *   },
     *   ...
     *  ]
     * }
     * </pre>
     * 
     * @param uri
     * @param usage
     * @param label
     * @param tq
     * @param controlled
     * @throws JSONException
     * @see <a href=
     *      "http://www.kiwi-community.eu/display/sunspace/Tagging+UC+Specification#TaggingUCSpecification-GetTagsResponse"
     *      >Tagging WebServices Umbrella</a>
     */
    public void addFreeTag(String uri, Long usage, String label, double tq)
            throws JSONException {
        final Map<String, Object> jsonObject = new HashMap<String, Object>();
        jsonObject.put("uri", uri);
        jsonObject.put("usage", usage  );
        jsonObject.put("label", label);
        jsonObject.put("tq", tq);
        jsonObject.put("controlled", 0);
        jsonObject.put("hasChildren", false);
        
        jsonObjects.add(jsonObject);
    }

    /**
     * @param orderType defines the order of display- 'alpha'
     *            (alphabetically), 'usage', 'equity' (tag
     *            equity)
     * @param reverse
     */
    public void sortTags(OrderTypes orderType, boolean reverse) {

        Comparator<Map<String, Object>> tagComp = null;
        switch (orderType) {
            case ALPHA:
                tagComp = new Comparator<Map<String, Object>>() {

                    @Override
                public int compare(Map<String, Object> arg0,
                        Map<String, Object> arg1) {
                    final String label0;
                    final String label1;
                    label0 = (String) arg0.get("label");
                    label1 = (String) arg1.get("label");

                    return label0.compareToIgnoreCase(label1);
                }

                };
                break;

            case USAGE:
                tagComp = new Comparator<Map<String, Object>>() {

                    @Override
                public int compare(Map<String, Object> arg0,
                        Map<String, Object> arg1) {
                    final Long usage0;
                    final Long usage1;

                    usage0 = (Long) arg0.get("usage");
                    usage1 = (Long) arg1.get("usage");

                    return usage0.compareTo(usage1);
                }

                };
                break;

            case EQUITY:
                tagComp = new Comparator<Map<String, Object>>() {

                    @Override
                public int compare(Map<String, Object> arg0, Map<String, Object> arg1) {
                    final Double tq0;
                    final Double tq1;

                    tq0 = (Double) arg0.get("tq");
                    tq1 = (Double) arg1.get("tq");
                    return tq0.compareTo(tq1);
                }

                };
                break;

        }
        if (tagComp != null)
            Collections.sort(jsonObjects, tagComp);
        if (reverse)
            Collections.reverse(jsonObjects);
    }

    /**
     * @return the mainObject
     */
    JSONObject getMainObject() {
        return mainObject;
    }

    /**
     * @param mainObject the mainObject to set
     */
    void setMainObject(JSONObject mainObject) {
        this.mainObject = mainObject;
    }

    /**
     * @return the jsonObjects
     */
    List<Map<String, Object>> getJsonObjects() {
        return jsonObjects;
    }

    /**
     * @param jsonObjects the jsonObjects to set
     */
    void setJsonObjects(List<Map<String, Object>> jsonObjects) {
        this.jsonObjects = jsonObjects;
    }

    public String toString() {
        // no duplicated and it also keeps the order (LinkedHashSet)
        final Set<Map<String, Object>> noDuplicates =
                new LinkedHashSet<Map<String,Object>>(jsonObjects);

        // mihai I am not sure if the constructor with a set of map works
        // how I imagine every map is a JSON objects, this is the reason
        // why I chose to wrote some code.
//        final JSONArray jsonArray = new JSONArray(noDuplicates);
        final JSONArray jsonArray = new JSONArray();
        for (Map<String, Object> json : noDuplicates) {
            jsonArray.put(new JSONObject(json));
        }

        try {
            mainObject.put("items", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return mainObject.toString();
    }
}
