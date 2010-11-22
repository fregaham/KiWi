/*
	Copyright © dkms, artaround
 */
package dkms.action.dkmsContentItem;

import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;


public class DkmsCombinedComponentManager {

	private String combinedItemTitle;	

	private String combinedItemType;
	
	private String combinedItemId;	

	private DkmsContentItemFacade combinedItem;

	

	public DkmsCombinedComponentManager(){
		
	}
	
	public DkmsCombinedComponentManager(String combinedItemTitle, String contentItemId, String combinedItemType){
		this.combinedItemTitle = combinedItemTitle;
		this.combinedItemId = contentItemId;
		this.combinedItemType = combinedItemType;
		
	}
	
	public String getCombinedItemTitle() {
		return combinedItemTitle;
	}

	public void setCombinedItemTitle(String combinedItemTitle) {
		this.combinedItemTitle = combinedItemTitle;
	}

	public String getCombinedItemType() {
		return combinedItemType;
	}

	public void setCombinedItemType(String combinedItemType) {
		this.combinedItemType = combinedItemType;
	}


	public DkmsContentItemFacade getCombinedItem() {
		return combinedItem;
	}

	public void setCombinedItem(DkmsContentItemFacade combinedItem) {
		this.combinedItem = combinedItem;
	}
	
	public String getCombinedItemId() {
		return combinedItemId;
	}

	public void setCombinedItemId(String combinedItemId) {
		this.combinedItemId = combinedItemId;
	}

	
		
}
