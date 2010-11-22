/*
	Copyright © dkms, artaround
 */

package dkms.action.utils;

import java.util.Comparator;

import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;


public class DkmsLastSubmittedComparator implements Comparator<DkmsContentItemFacade>{
	public int compare(DkmsContentItemFacade a, DkmsContentItemFacade b){
		if(a.getCreated().before(b.getCreated())){
			return 1;
		}
		else if(a.getCreated().equals(b.getCreated())){
			return 0;
		}
		else{
			return -1;
		}		
	}
}
