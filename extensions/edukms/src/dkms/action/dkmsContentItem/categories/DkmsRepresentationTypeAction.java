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


@Name("dkmsRepresentationTypeAction")
@Scope(ScopeType.CONVERSATION)
//@Transactional
public class DkmsRepresentationTypeAction extends TreeChooserAction{
	
	@In
	private Map<String, String> messages;
	
	@Logger
	private static Log log;
	
	private LinkedList<SKOSConcept> chosenRepresentationTypes;
	
	private SKOSConcept selectedRepresentationType;
	
	@Override
	public String getRoot() {
		return "Representation Type";	
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
	
	public void setSelectedRepresentationType(SKOSConcept c) {
		log.info("Selected concept #0",c.getPreferredLabel());
		this.selectedRepresentationType = c;
	}
	
	public String removeChosenRepresentationType() {
		if( selectedRepresentationType == null ) {
			return messages.get(getSelectToRemoveMsg());
		}
		if( chosenRepresentationTypes.contains(selectedRepresentationType) ) {
			chosenRepresentationTypes.remove(selectedRepresentationType);
		}
		return null;
	}
	
	public String addChosenRepresentationType() {
		
		if( chosenRepresentationTypes == null ) {
			chosenRepresentationTypes = new LinkedList<SKOSConcept>();
		}
		
		if( selectedTreeConcept == null ) {
			return messages.get(getSelectToAddMsg());
		}
		
		if( !selectedTreeConcept.getNarrower().isEmpty() ) {
			return "'"+selectedTreeConcept.getPreferredLabel() +"' "+messages.get(getNotValidMsg());
		}
		
		if( chosenRepresentationTypes.contains(selectedTreeConcept) ) {
			
			return "Position '"+ selectedTreeConcept.getPreferredLabel()+ "' " +messages.get(getAlreadySelectedMsg());
		}
		
		chosenRepresentationTypes.add(selectedTreeConcept);
		return null;
 	}
	
	public LinkedList<SKOSConcept> getChosenRepresentationTypes() {
		if( chosenRepresentationTypes != null )
			return chosenRepresentationTypes;
		else
			return new LinkedList<SKOSConcept>();
	}
	
	public void setChosenRepresentationTypes(LinkedList<SKOSConcept> positions) {
		this.chosenRepresentationTypes = positions;
	}
	
	public void clear(){
		chosenRepresentationTypes = new LinkedList<SKOSConcept>();
	}
		
}
