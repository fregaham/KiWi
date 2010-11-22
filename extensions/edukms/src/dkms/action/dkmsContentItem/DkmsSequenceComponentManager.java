/*
	Copyright © dkms, artaround
 */
package dkms.action.dkmsContentItem;

import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;


public class DkmsSequenceComponentManager {

	private String version;
	
	private String sequenceContent;	
	
	private String taskTitle;
	
	private int taskId;
	
	private DkmsContentItemFacade sequenceItem;

	

	public DkmsSequenceComponentManager(){
		
	}
	
	public DkmsSequenceComponentManager(int taskId, String taskTitle, String version, String sequenceContent){
		this.taskId = taskId;
		this.taskTitle = taskTitle;
		this.version = version;
		this.sequenceContent = sequenceContent;
		
	}
	
	

	public int getTaskId() {
		return taskId;
	}

	public void setTaskId(int taskId) {
		this.taskId = taskId;
	}

	public String getTaskTitle() {
		return taskTitle;
	}

	public void setTaskTitle(String taskTitle) {
		this.taskTitle = taskTitle;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getSequenceContent() {
		return sequenceContent;
	}

	public void setSequenceContent(String sequenceContent) {
		this.sequenceContent = sequenceContent;
	}


	public DkmsContentItemFacade getSequenceItem() {
		return sequenceItem;
	}

	public void setSequenceItem(DkmsContentItemFacade sequenceItem) {
		this.sequenceItem = sequenceItem;
	}
	
	

	
		
}
