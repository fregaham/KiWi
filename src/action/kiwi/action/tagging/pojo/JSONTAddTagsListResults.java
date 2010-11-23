package kiwi.action.tagging.pojo;


import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONTAddTagsListResults {

	private List<JSONObject> taxonomies;

	public JSONTAddTagsListResults() {
		super();
		taxonomies = new ArrayList<JSONObject>();
	}

	public void addTags(String success, String error, String newItems) throws JSONException {
		final JSONObject jsonObject = new JSONObject();
		if (success != null) {
		    jsonObject.put("Success ", success);    
		}
		
		if (error != null) {
		    jsonObject.put("Error", error);    
		}
		
		if (newItems == null) {
		    jsonObject.put("newItems", newItems);
		}

		taxonomies.add(jsonObject);
	}

    @Override
    public String toString() {
        // FIXME : this method depends on the on the toString implementation
        // for the ArrayList and this brings an unwished coupling.
        return taxonomies.toString();
    }
}
