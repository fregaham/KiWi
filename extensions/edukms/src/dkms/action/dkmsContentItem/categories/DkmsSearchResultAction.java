package dkms.action.dkmsContentItem.categories;

import kiwi.api.content.ContentItemService;
import kiwi.api.search.KiWiSearchResults.SearchResult;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiResource;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.log.Log;

@Name("dkmsSearchResultAction")
@Scope(ScopeType.SESSION)
//@Transactional
public class DkmsSearchResultAction {

	@Logger
	Log log;
	
	@In
	private ContentItemService contentItemService;
	
//	//this method identifies the type of the search result 
//	//and renders results with different types in a different way
//	//we use it to distinguish between artworks, perons and events 
	public String getSearchResultItemTemplate(ContentItem ci) {
		log.info(ci.getTitle());
		
		String res = "";

		if(ci != null){
			for (KiWiResource type : ci.getTypes()) {
				String seRQLID = type.getSeRQLID();
			log.debug("type: #0", seRQLID);
				if (seRQLID.contains(Constants.DKMS_CORE + "DkmsContentItem")) {
					return "/edukms/abcmaths/searchResult.xhtml";
				} else{
					res = "";
				}		
			}
		}
		return res;
	}
	
}
