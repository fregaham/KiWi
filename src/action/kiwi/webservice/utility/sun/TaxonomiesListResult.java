

package kiwi.webservice.utility.sun;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * JSON object used like response for the getTaxonomies web
 * service. The format follow the next code snippet :
 * 
 * <pre>
 * [{
 *     "label": "Region",
 *     "prefix":"region",
 *     "required": true,
 *     "uri": "http://..."
 *  },{
 *     "label": "Business Unit"
 *     "prefix":"bu",
 *     "required": false,
 *     "uri": "http://..."
 * }]
 * </pre>
 * 
 * @see <a href=
 *      "http://www.kiwi-community.eu/display/sunspace/Tagging+WebServices+Umbrella#TaggingWebServicesUmbrella-GetTaxonomies"
 *      >Tagging WebServices Umbrella</a>
 * @author mihai
 * @version 0.9
 * @since 0.9
 */
public class TaxonomiesListResult {
    
    private Set<Map<String, Object>> taxonomies;


    public TaxonomiesListResult() {
        taxonomies = new HashSet<Map<String, Object>>();
    }

    public void addTaxonomies(String label, String prefix, boolean required, String uri)
            throws JSONException {
        final Map<String, Object> jsonObject = new HashMap<String, Object>();
        jsonObject.put("label", label);
        jsonObject.put("prefix", prefix);
        jsonObject.put("required", required);
        jsonObject.put("uri", uri);

        taxonomies.add(jsonObject);
    }

    @Override
    public String toString() {
        final JSONArray jsonArray = new JSONArray();
        for (Map<String, Object> taxonimy : taxonomies) {
            final JSONObject jsonObject = new JSONObject(taxonimy);
            jsonArray.put(jsonObject);
        }

        return jsonArray.toString();
    }
}
