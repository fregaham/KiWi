package kiwi.action.tagging.pojo;


import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONTaxonomiesListResults {

	private List<JSONObject> taxonomies;

	public JSONTaxonomiesListResults() {
		super();
		taxonomies = new ArrayList<JSONObject>();
	}

	public void addTaxonomies(String label, String prefix, boolean required) throws JSONException {
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("label", label);
		jsonObject.put("prefix", prefix);
		jsonObject.put("required", required);

		taxonomies.add(jsonObject);
	}

    @Override
    public String toString() {
        // FIXME : this method depends on the on the toString implementation
        // for the ArrayList and this brings an unwished coupling.
        return taxonomies.toString();
    }
}
