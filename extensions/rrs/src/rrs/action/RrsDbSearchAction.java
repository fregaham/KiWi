package rrs.action;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import rrs.service.RrsDb;

@Name("rrs.rrsDbSearchAction")
@Scope(ScopeType.PAGE)
public class RrsDbSearchAction {
	@In(value="rrs.db", create=true)
	private RrsDb rrsDb;
	
	private String query;
	private List<RrsDb.Publication> results;
	
	public void setQuery(String query) {
		this.query = query;
	}
	
	public String getQuery() {
		return this.query;
	}
	
	public void search() throws SQLException {
		results = rrsDb.getPublicationsByLikeTitle(query);
	}

	public List<RrsDb.Publication> getResults() {
		if (results == null) {
			results = Collections.emptyList();
		}
		
		return results;
	}
}
