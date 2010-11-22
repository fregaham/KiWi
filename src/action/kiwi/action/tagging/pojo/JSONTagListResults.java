/**
 * 
 */
package kiwi.action.tagging.pojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import kiwi.action.tagging.TaggingWebService.OrderTypes;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Consumes information for the response for a getTags WebService request and through the 
 * toString() method produces a JSON String. 
 * @author Szaby Grünwald
 */
public class JSONTagListResults {

	private List<JSONObject> jsonObjects;
	
	public JSONTagListResults() {
		super();
		jsonObjects = new ArrayList<JSONObject>();
	}

	public void addTag(String uri, Long usage, String label, double tq, boolean controlled, boolean isOwnTag) throws JSONException {
	    final JSONObject jsonObject = new JSONObject();
	    jsonObject.put("uri", uri);
	    jsonObject.put("label", label);
	    jsonObject.put("usage", usage);
	    jsonObject.put("tq", tq);
	    jsonObject.put("controlled", controlled);
	    jsonObject.put("isOwnTag", isOwnTag);
	    
	    jsonObjects.add(jsonObject);
	}


	/**
	 * 
	 * @param orderType defines the order of display- 'alpha' (alphabetically), 'usage', 'equity' (tag equity) 
	 * @param reverse
	 */
	public void sortTags(OrderTypes orderType, boolean reverse) {
		
		Comparator<JSONObject> tagComp = null;
		switch (orderType) {
		case ALPHA:
			tagComp = new Comparator<JSONObject>() {
				
				@Override
				public int compare(JSONObject arg0,
				        JSONObject arg1) {
					final String label0;
					final String label1;
                    try {
                        label0 = (String)arg0.get("label");
                        label1 = (String)arg1.get("label");
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
				public int compare(JSONObject arg0,
				        JSONObject arg1) {
					final Long usage0;
					final Long usage1;
                    try {
                        usage0 = (Long)arg0.get("usage");
                        usage1 = (Long)arg1.get("usage");
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
				public int compare(JSONObject arg0,
				        JSONObject arg1) {
					final Double tq0;
					final Double tq1;
                    try {
                        tq0 = (Double)arg0.get("tq");
                        tq1 = (Double)arg1.get("tq");
                    } catch (JSONException e) {
                        return 0;
                    }
					return tq0.compareTo(tq1);
				}
				
			};
			break;
			
		}
		if(tagComp!=null)
			Collections.sort(jsonObjects, tagComp);
		if(reverse)
			Collections.reverse(jsonObjects);
		
	}
	
    @Override
    public String toString() {
        // FIXME : this method depends on the on the toString implementation
        // for the ArrayList and this brings an unwished coupling.
        return jsonObjects.toString();
    }
}
