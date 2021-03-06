/*
	Copyright © dkms, artaround
 */

package dkms.action.dkmsContentItem;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;



import dkms.action.utils.DkmsLastModifiedComparator;
import dkms.action.utils.DkmsLastSubmittedComparator;
import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;

@Name("dkmsSortModusAction")
@Scope(ScopeType.CONVERSATION)
@AutoCreate
public class DkmsSortModusAction implements Serializable {
	
	
	private static final long serialVersionUID = 6638934301101664663L;

	@Out(required = false)
	private Comparator<DkmsContentItemFacade> comp;
	
	private LinkedList<String> l ;
	private final String zg = "zuletzt geaendert";
	private final String ze = "zuletzt erstellt";
	
    @Logger
    private Log log;
	
	@Create
	public void begin(){
		l = new LinkedList<String>();
		l.add(ze);
		l.add(zg);
	}
	
	private String selectedSortOrder;
	
	public String getSelectedSortOrder() {
		return selectedSortOrder;
	}

	public void setSelectedSortOrder(String selectedSortOrder) {
		this.selectedSortOrder = selectedSortOrder;
	}

	public List<String> getSortCriterias(){
		
		return l;
	}
		
	public void go(){
		log.info("selectedSortOrder #0", selectedSortOrder);
		
		if (selectedSortOrder.equals(zg)){
			comp =  new DkmsLastModifiedComparator();
		}
		else{
			comp = new DkmsLastSubmittedComparator();
		}	
	}	
}



