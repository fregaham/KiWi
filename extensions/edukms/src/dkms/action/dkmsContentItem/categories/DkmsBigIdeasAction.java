/*
	Copyright © dkms, artaround
 */

package dkms.action.dkmsContentItem.categories;



import java.util.LinkedList;
import java.util.Map;

import kiwi.commons.treeChosser.TreeChooserAction;
import kiwi.model.ontology.SKOSConcept;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.log.Log;


@Name("dkmsBigIdeasAction")
@Scope(ScopeType.CONVERSATION)
//@Transactional
public class DkmsBigIdeasAction extends TreeChooserAction{
	
	@In
	private Map<String, String> messages;
	
	@Logger
	private static Log log;
	
	private LinkedList<SKOSConcept> chosenBigIdeas;
	
	private SKOSConcept selectedBigIdea;
	
	@Override
	public String getRoot() {
		return "Big Idea";	
	}
	
	public String getSelectToRemoveMsg(){
		return "artaround.error.technique_select_to_remove";
	}
	
	public String getSelectToAddMsg(){
		return "artaround.error.technique_select_to_add_msg";
	}
	
	public String getNotValidMsg(){
		return "artaround.error.technique_not_valid";
	}
	
	public String getAlreadySelectedMsg(){
		return "artaround.error.technique_already_selected";
	}
	
	public void setSelectedBigIdea(SKOSConcept c) {
		log.info("Selected concept #0",c.getPreferredLabel());
		this.selectedBigIdea = c;
	}
	
	public String removeChosenBigIdea() {
		if( selectedBigIdea == null ) {
			return messages.get(getSelectToRemoveMsg());
		}
		if( chosenBigIdeas.contains(selectedBigIdea) ) {
			chosenBigIdeas.remove(selectedBigIdea);
		}
		return null;
	}
	
	public String addChosenBigIdea() {
		
		if( chosenBigIdeas == null ) {
			chosenBigIdeas = new LinkedList<SKOSConcept>();
		}
		
		if( selectedTreeConcept == null ) {
			return messages.get(getSelectToAddMsg());
		}
		
		if( !selectedTreeConcept.getNarrower().isEmpty() ) {
			return "'"+selectedTreeConcept.getPreferredLabel() +"' "+messages.get(getNotValidMsg());
		}
		
		if( chosenBigIdeas.contains(selectedTreeConcept) ) {
			
			return "Position '"+ selectedTreeConcept.getPreferredLabel()+ "' " +messages.get(getAlreadySelectedMsg());
		}
		
		chosenBigIdeas.add(selectedTreeConcept);
		return null;
 	}
	
	public LinkedList<SKOSConcept> getChosenBigIdeas() {
		if( chosenBigIdeas != null )
			return chosenBigIdeas;
		else
			return new LinkedList<SKOSConcept>();
	}
	
	public void setChosenBigIdeas(LinkedList<SKOSConcept> positions) {
		this.chosenBigIdeas = positions;
	}
	
	public void clear(){
		chosenBigIdeas = new LinkedList<SKOSConcept>();
	}
		
}
