package rrs.action;

import java.util.LinkedList;
import java.util.List;

import kiwi.api.fragment.FragmentFacade;
import kiwi.api.fragment.FragmentService;
import kiwi.api.tagging.TaggingService;
import kiwi.model.content.ContentItem;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * Backing bean for the fragment_tags widget, manages list of tags of the fragments in the currentContentItem
 * @author Marek Schmidt
 *
 */
@Name("rrs.fragmentTagsAction")
@Scope(ScopeType.EVENT)
public class FragmentTagsAction {

	@In(create = true)
	FragmentService fragmentService;
	
	@In
	ContentItem currentContentItem;
	
	@In(create = true)
	TaggingService taggingService;
	
	public List<ContentItem> getFragments() {
		List<ContentItem> ret = new LinkedList<ContentItem> ();
		
		for (FragmentFacade ff : fragmentService.getContentItemFragments(currentContentItem, FragmentFacade.class)) {
			ret.add(ff.getDelegate());
		}
		
		return ret;
	}
	
	public List<ContentItem> getFragmentTags(ContentItem ff) {
		return taggingService.getTags(ff);
	}
}
