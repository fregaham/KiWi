/**
 * 
 */


package kiwi.webservice.utility.sun;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import kiwi.webservice.TaggingWebService.OrderTypes;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * JSON object used like response for the remove tags web
 * service. The format follow the next code snippet :
 * 
 * <pre>
 * [{"prefix":"Ideator Categories_prefix",
 * "label":"Ideator Categories"},
 * ... 
 * ] *
 * </pre>
 * 
 * @see <a href=
 *      "hhttp://www.kiwi-community.eu/display/sunspace/Tagging+UC+Specification#TaggingUCSpecification-RemoveTag%28s%29Request"
 *      >Tagging WebServices Umbrella</a>
 */
public class RemoveTagResponse extends JSONObject {

    private List<JSONObject> jsonObjects;

    public RemoveTagResponse() throws JSONException {
        super();
        jsonObjects = new ArrayList<JSONObject>();
        this.put("items", jsonObjects);
    }

    public void addTag(String uri, String label) throws JSONException {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("uri", uri);
        jsonObject.put("label", label);
        
        jsonObjects.add(jsonObject);

        // ensures that the items items contains the last list of objects
        this.put("items", jsonObjects);
    }

    /**
     * @param orderType defines the order of display- 'alpha'
     *            (alphabetically), 'usage', 'equity' (tag
     *            equity)
     * @param reverse
     */
    public void sortTags(OrderTypes orderType, boolean reverse) {

        Comparator<JSONObject> tagComp = null;
        switch (orderType) {
            case ALPHA:
                tagComp = new Comparator<JSONObject>() {

                    @Override
                    public int compare(JSONObject arg0, JSONObject arg1) {
                        final String label0;
                        final String label1;
                        try {
                            label0 = (String) arg0.get("label");
                            label1 = (String) arg1.get("label");
                        } catch (JSONException e) {
                            return 0;
                        }

                        return label0.compareToIgnoreCase(label1);
                    }

                };
                break;

            case USAGE:
                tagComp = new Comparator<JSONObject>() {

                    @Override
                    public int compare(JSONObject arg0, JSONObject arg1) {
                        final Long usage0;
                        final Long usage1;
                        try {
                            usage0 = (Long) arg0.get("usage");
                            usage1 = (Long) arg1.get("usage");
                        } catch (JSONException e) {
                            return 0;
                        }

                        return usage0.compareTo(usage1);
                    }

                };
                break;

            case EQUITY:
                tagComp = new Comparator<JSONObject>() {

                    @Override
                    public int compare(JSONObject arg0, JSONObject arg1) {
                        final Double tq0;
                        final Double tq1;
                        try {
                            tq0 = (Double) arg0.get("tq");
                            tq1 = (Double) arg1.get("tq");
                        } catch (JSONException e) {
                            return 0;
                        }
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

}
