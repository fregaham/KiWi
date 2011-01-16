/*
	Copyright © dkms, artaround
 */

package dkms.action.dkmsContentItem.categories;



import java.util.LinkedList;
import java.util.Map;

import kiwi.commons.treeChosser.TreeChooserAction;
import kiwi.commons.treeChosser.TreeNode;
import kiwi.model.ontology.SKOSConcept;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
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
	
	public LinkedList<TreeNode> getBigIdeas(){
		
		
		TreeNode[] tr = getTreeRoots();
		
		LinkedList<TreeNode> bigIdeasRoots = new LinkedList<TreeNode>();
		
		for(TreeNode l : tr){
		
			if (l.getConcept().getDefinition().equals("Big Idea")){
				
				bigIdeasRoots.add(l);
			}		
		}
		
		return bigIdeasRoots;
	}
	
	public LinkedList<TreeNode> getContentAreas(){
		
		
		TreeNode[] tr = getTreeRoots();
		
		LinkedList<TreeNode> contentAreaRoots = new LinkedList<TreeNode>();
		
		for(TreeNode l : tr){
		
			if (l.getConcept().getDefinition().equals("Content Area")){
				
				contentAreaRoots.add(l);
			}		
		}
		
		return contentAreaRoots;
	}
	
	public LinkedList<TreeNode> getLanguages(){
		
		
		TreeNode[] tr = getTreeRoots();
		
		LinkedList<TreeNode> contentAreaRoots = new LinkedList<TreeNode>();
		
		for(TreeNode l : tr){
		
			if (l.getConcept().getDefinition().equals("Language")){
				
				contentAreaRoots.add(l);
			}		
		}
		
		return contentAreaRoots;
	}
	
	public LinkedList<TreeNode> getStati(){
		
		
		TreeNode[] tr = getTreeRoots();
		
		LinkedList<TreeNode> contentAreaRoots = new LinkedList<TreeNode>();
		
		for(TreeNode l : tr){
		
			if (l.getConcept().getDefinition().equals("Status")){
				
				contentAreaRoots.add(l);
			}		
		}
		
		return contentAreaRoots;
	}
	
	public LinkedList<TreeNode> getTypesOfResource(){
		
		
		TreeNode[] tr = getTreeRoots();
		
		LinkedList<TreeNode> contentAreaRoots = new LinkedList<TreeNode>();
		
		for(TreeNode l : tr){
		
			if (l.getConcept().getDefinition().equals("Type of resource")){
				
				contentAreaRoots.add(l);
			}		
		}
		
		return contentAreaRoots;
	}
	
	public LinkedList<TreeNode> getIntentions(){		
		
		TreeNode[] tr = getTreeRoots();
		
		LinkedList<TreeNode> contentAreaRoots = new LinkedList<TreeNode>();
		
		for(TreeNode l : tr){
		
			if (l.getConcept().getDefinition().equals("Intention")){
				
				contentAreaRoots.add(l);
			}		
		}
		
		return contentAreaRoots;
	}
	
	public LinkedList<TreeNode> getAgeGroups(){		
		
		TreeNode[] tr = getTreeRoots();
		
		LinkedList<TreeNode> contentAreaRoots = new LinkedList<TreeNode>();
		
		for(TreeNode l : tr){
		
			if (l.getConcept().getDefinition().equals("Age Group")){
				
				contentAreaRoots.add(l);
			}		
		}
		
		return contentAreaRoots;
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
	
	public void setChosenBigIdeas(LinkedList<SKOSConcept> positions) {
		this.chosenBigIdeas = positions;
	}
	
	
	public LinkedList<SKOSConcept> getBigIdeaSet(){
		
		if( chosenBigIdeas != null ){
			LinkedList<SKOSConcept> bigIdeasSet = new LinkedList<SKOSConcept>();			
			for(SKOSConcept l : chosenBigIdeas){
				if (l.getDefinition().equals("Big Idea")){
				bigIdeasSet.add(l);				
				}
			}
			return bigIdeasSet;
		}
		else
			return new LinkedList<SKOSConcept>();
		
	}
	
	public LinkedList<SKOSConcept> getContentAreaSet(){
		
		if( chosenBigIdeas != null ){
			LinkedList<SKOSConcept> contentAreaSet = new LinkedList<SKOSConcept>();			
			for(SKOSConcept l : chosenBigIdeas){
				if (l.getDefinition().equals("Content Area")){
					contentAreaSet.add(l);				
				}
			}
			return contentAreaSet;
		}
		else
			return new LinkedList<SKOSConcept>();		
	}
	
	public LinkedList<SKOSConcept> getLanguageSet(){
		
		if( chosenBigIdeas != null ){
			LinkedList<SKOSConcept> languageSet = new LinkedList<SKOSConcept>();			
			for(SKOSConcept l : chosenBigIdeas){
				if (l.getDefinition().equals("Language")){
					languageSet.add(l);				
				}
			}
			return languageSet;
		}
		else
			return new LinkedList<SKOSConcept>();		
	}
	
	public LinkedList<SKOSConcept> getStatusSet(){
		
		if( chosenBigIdeas != null ){
			LinkedList<SKOSConcept> statusSet = new LinkedList<SKOSConcept>();			
			for(SKOSConcept l : chosenBigIdeas){
				if (l.getDefinition().equals("Status")){
					statusSet.add(l);				
				}
			}
			return statusSet;
		}
		else
			return new LinkedList<SKOSConcept>();		
	}
	
	
	public LinkedList<SKOSConcept> getTypeOfResourceSet(){
		
		if( chosenBigIdeas != null ){
			LinkedList<SKOSConcept> typeOfResourceSet = new LinkedList<SKOSConcept>();			
			for(SKOSConcept l : chosenBigIdeas){
				if (l.getDefinition().equals("Type of resource")){
					typeOfResourceSet.add(l);				
				}
			}
			return typeOfResourceSet;
		}
		else
			return new LinkedList<SKOSConcept>();		
	}
	
	
	public LinkedList<SKOSConcept> getIntentionSet(){
		
		if( chosenBigIdeas != null ){
			LinkedList<SKOSConcept> intentionSet = new LinkedList<SKOSConcept>();			
			for(SKOSConcept l : chosenBigIdeas){
				if (l.getDefinition().equals("Intention")){
					intentionSet.add(l);				
				}
			}
			return intentionSet;
		}
		else
			return new LinkedList<SKOSConcept>();		
	}
	
	public LinkedList<SKOSConcept> getAgeGroupSet(){
		
		if( chosenBigIdeas != null ){
			LinkedList<SKOSConcept> ageGroupSet = new LinkedList<SKOSConcept>();			
			for(SKOSConcept l : chosenBigIdeas){
				if (l.getDefinition().equals("Age Group")){
					ageGroupSet.add(l);				
				}
			}
			return ageGroupSet;
		}
		else
			return new LinkedList<SKOSConcept>();		
	}
	
	
	public LinkedList<SKOSConcept> getChosenBigIdeas() {
		if( chosenBigIdeas != null )
			return chosenBigIdeas;
		else
			return new LinkedList<SKOSConcept>();
	}	

	
	public void clear(){
		chosenBigIdeas = new LinkedList<SKOSConcept>();
	}
		
}
