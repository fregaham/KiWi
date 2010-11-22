/*
	Copyright © dkms, artaround
 */

package dkms.action.utils;

import java.util.Comparator;

import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;


public class DkmsLastModifiedComparator implements Comparator<DkmsContentItemFacade>{
	public int compare(DkmsContentItemFacade a, DkmsContentItemFacade b){
		if(a.getModified().before(b.getModified())){
			return 1;
		}
		else if(a.getModified().equals(b.getModified())){
			return 0;
		}
		else{
			return -1;
		}		
	}
}
