package kiwi.action.tagging.pojo;


import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONGetPrefiexesListResults {

	private List<JSONObject> taxonomies;

	public JSONGetPrefiexesListResults() {
		super();
		taxonomies = new ArrayList<JSONObject>();
	}

	public void addPrefix(String prefix) throws JSONException {
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("prefix", prefix);
		taxonomies.add(jsonObject);
	}

    @Override
    public String toString() {
        // FIXME : this method depends on the on the toString implementation
        // for the ArrayList and this brings an unwished coupling.
        return taxonomies.toString();
    }
}
