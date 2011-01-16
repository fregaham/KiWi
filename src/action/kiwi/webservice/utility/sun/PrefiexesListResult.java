

package kiwi.webservice.utility.sun;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * JSON object used like response for the getPrefiexes web
 * service. The format follow the next code snippet :
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
 * @author mihai
 * @version 0.9
 * @since 0.9
 */
public class PrefiexesListResult {
    
    private final Set<String> prefixes;

    public PrefiexesListResult() {
        prefixes = new HashSet<String>();
    }

    public void addPrefix(String prefix) throws JSONException {
        prefixes.add(prefix);
    }

    @Override
    public String toString() {

        final JSONArray jsonArray = new JSONArray();
        for (String prefix : prefixes) {
            final JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("prefix", prefix);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonArray.put(jsonObject);
        }

        return jsonArray.toString();
    }
}
