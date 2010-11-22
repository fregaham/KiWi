package rrs.action;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import rrs.service.RrsDb;

@Name("rrs.displayAction")
@Scope(ScopeType.CONVERSATION)
public class DisplayAction {
	@In(value="rrs.db", create=true)
	private RrsDb rrsDb;
	
	private RrsDb.Publication publication;

	public void display(long id) {
		
	}

	public RrsDb.Publication getPublication() {
		if (publication == null) {
			publication = new RrsDb.Publication();
		}
		return publication;
	}
	
	

	
}
