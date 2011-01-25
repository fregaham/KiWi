package rrs.action;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import kiwi.api.content.ContentItemService;
import kiwi.api.fragment.FragmentFacade;
import kiwi.api.fragment.FragmentService;
import kiwi.api.search.KiWiSearchCriteria;
import kiwi.api.search.KiWiSearchResults;
import kiwi.api.search.KiWiSearchResults.SearchResult;
import kiwi.api.search.SolrService;
import kiwi.api.tagging.TaggingService;
import kiwi.api.transaction.TransactionService;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.informationextraction.Context;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.user.User;
import kiwi.service.transaction.KiWiSynchronizationImpl;
import kiwi.util.KiWiXomUtils;
import kiwi.util.KiWiXomUtils.NodePos;

import nu.xom.Document;
import nu.xom.Element;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.jboss.seam.transaction.Transaction;

@Name("rrs.regExAnnotator")
@Scope(ScopeType.EVENT)
public class RegExAnnotator {
	
	public static class FragmentUI {
		public ContentItem getItem() {
			return item;
		}

		public void setItem(ContentItem item) {
			this.item = item;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public List<ContentItem> getTags() {
			if (tags == null) {
				tags = new LinkedList<ContentItem> ();
			}
			return tags;
		}

		public void setTags(List<ContentItem> tags) {
			this.tags = tags;
		}

		private ContentItem item;
		
		private String prefix;
		private String postfix;
		
		private String text;
		
		private List<ContentItem> tags;

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getPostfix() {
			return postfix;
		}

		public void setPostfix(String postfix) {
			this.postfix = postfix;
		}
	}
	
	public static class GroupedFragmentUI {
		private ContentItem item;
		private List<FragmentUI> fragments;
		private List<ContentItem> tags;
		
		public List<ContentItem> getTags() {
			if (tags == null) {
				tags = new LinkedList<ContentItem> ();
			}
			return tags;
		}

		public void setTags(List<ContentItem> tags) {
			this.tags = tags;
		}
		
		public void setItem(ContentItem item) {
			this.item = item;
		}

		public ContentItem getItem() {
			return item;
		}
		
		public List<FragmentUI> getFragments() {
			if (fragments == null) {
				fragments = new LinkedList<FragmentUI>();
			}
			return fragments;
		}
	}
	
	private List<GroupedFragmentUI> groupedFragments;
	
	@Logger
	private static Log log;
	
	@In
	private ContentItem currentContentItem;
	
	@In
	private TransactionService transactionService;
	
	@In
	EntityManager entityManager;
	
	@In(create = true)
	private FragmentService fragmentService;
	
	@In(create = true)
	private ContentItemService contentItemService;
	
	@In(create = true)
	private TaggingService taggingService;
	
	@In(create = true)
	private User currentUser;
	
	@In(create=true)
	private SolrService solrService;
	
	private String regEx;
	
	private String filter;
	
	public void setRegEx(String regEx) {
		this.regEx = regEx;
	}
	
	public String getRegEx() {
		return regEx;
	}
	
	public void setFilter(String filter) {
		this.filter = filter;
	}
	
	public String getFilter() {
		return this.filter;
	}
	
	public List<GroupedFragmentUI> getGroupedFragments() throws NamespaceResolvingException {
		if (groupedFragments == null) {
			
			Map<String, GroupedFragmentUI> id2gf = new TreeMap<String, GroupedFragmentUI>();
			
			for (KiWiTriple triple : currentContentItem.getResource().listIncoming("<" + Constants.NS_HGTAGS + "taggedWithTag" + ">")) {
				if (triple.getSubject().hasType(Constants.NS_KIWI_SPECIAL + "Fragment")) {
					KiWiResource fragment = triple.getSubject();
					ContentItem fragmentItem = fragment.getContentItem();
					if (fragmentItem.isDeleted()) {
						continue;
					}
					
					KiWiResource fragmentOfResource = null;
					for (KiWiTriple fragmentOfTriple : fragment.listOutgoing("<" + Constants.NS_KIWI_SPECIAL + "fragmentOf" +">")) {
						fragmentOfResource = (KiWiResource)fragmentOfTriple.getObject();
					}
					
					if (fragmentOfResource != null) {
						ContentItem fragmentOfItem = fragmentOfResource.getContentItem();
						if (!fragmentOfItem.isDeleted()) {
							// we add the fragment to the collection
							
							GroupedFragmentUI gf = null;
							if (!id2gf.containsKey(fragmentOfItem.getKiwiIdentifier())) {
								gf = new GroupedFragmentUI();
								gf.setItem(fragmentOfItem);
								gf.setTags(taggingService.getTags(fragmentOfItem));
								id2gf.put(fragmentOfItem.getKiwiIdentifier(), gf);
							}
							else {
								gf = id2gf.get(fragmentOfItem.getKiwiIdentifier());
							}
							
							FragmentUI fui = new FragmentUI();
							fui.setItem(fragmentItem);
							
							fui.setTags(taggingService.getTags(fragmentItem));
							
							/*if (fragmentItem.getTextContent() != null) {
								fui.setText(KiWiXomUtils.xom2plain(fragmentItem.getTextContent().getXmlDocument()));
							}
							else {
								fui.setText("");
							}*/
							
							gf.getFragments().add(fui);
						}
					}
				}
			}
			
			groupedFragments = new LinkedList<GroupedFragmentUI> (id2gf.values());
			
			// create prefixes and postfixes and texts
			for (GroupedFragmentUI gf : groupedFragments) {
				if (gf.getItem().getTextContent() != null) {
					
					// We get the text coordinates of the fragments
					Document xom = gf.getItem().getTextContent().getXmlDocument();
					String plain = KiWiXomUtils.xom2plain(xom);
					
					Map<String, Integer> begins = new HashMap<String, Integer> ();
					Map<String, Integer> ends = new HashMap<String, Integer> ();
					
					KiWiXomUtils.NodePosIterator iter = new KiWiXomUtils.NodePosIterator(xom);
					while(iter.hasNext()) {
						NodePos np = iter.next();
						if (np.getNode() instanceof Element) {
							Element e = (Element)np.getNode();
							if ("bookmarkstart".equals(e.getLocalName())) {
								begins.put(e.getAttributeValue("id"), np.getPos());
							}
							else if ("bookmarkend".equals(e.getLocalName())) {
								ends.put(e.getAttributeValue("id"), np.getPos());
							}
						}
					}
					
					for (FragmentUI f : gf.getFragments()) {
						
						String fid = f.getItem().getKiwiIdentifier();
						if (begins.containsKey(fid) && ends.containsKey(fid)) {
							int begin = begins.get(fid);
							int end = ends.get(fid);
							f.setText(plain.substring(begin, end));
							f.setPrefix(plain.substring(Math.max(begin - 80, 0), begin));
							f.setPostfix(plain.substring(end, Math.min(end + 80, plain.length())));
						}
					}
				}				
			}
		}
		
		return groupedFragments;
	}
	
	private void annotateItem(Pattern p, ContentItem item) {
		
		log.info("Annotating #0 (#1)", item.getTitle(), item.getKiwiIdentifier());
		
		Document xom = item.getTextContent().copyXmlDocument();
		String plaintext = KiWiXomUtils.xom2plain(xom);
		
		Map<Integer, Collection<String>> bookmarkstarts = new HashMap<Integer, Collection<String>> ();
		Map<Integer, Collection<String>> bookmarkends = new HashMap<Integer, Collection<String>> ();
		
		boolean textContentChange = false;
		
		KiWiXomUtils.NodePosIterator iter = new KiWiXomUtils.NodePosIterator(xom);
		while(iter.hasNext()) {
			NodePos np = iter.next();
			if (np.getNode() instanceof Element) {
				Element e = (Element)np.getNode();
				if ("bookmarkstart".equals(e.getLocalName())) {
					Collection<String> list = null;
					if (!bookmarkstarts.containsKey(np.getPos())) {
						list = new HashSet<String>();
						bookmarkstarts.put(np.getPos(), list);
					}
					else {
						list = bookmarkstarts.get(np.getPos());
					}
					
					list.add(e.getAttributeValue("id"));
				}
				else if ("bookmarkend".equals(e.getLocalName())) {
					Collection<String> list = null;
					if (!bookmarkends.containsKey(np.getPos())) {
						list = new HashSet<String>();
						bookmarkends.put(np.getPos(), list);
					}
					else {
						list = bookmarkends.get(np.getPos());
					}
					
					list.add(e.getAttributeValue("id"));
				}
			}
		}
		
		Matcher m = p.matcher(plaintext);
		find: while(m.find()) {
			int start = m.start(1);
			int end = m.end(1);
			String content = m.group(1);
			
			log.info("match #0:#1 #2", start, end, content);
			
			String fragmentId = null;
			// Check if there is already a fragment at these coordinates
			Collection<String> startids = bookmarkstarts.get(start);
			Collection<String> endids = bookmarkends.get(end);
			
			if (startids != null && endids != null) {
				iter_startids: for (String startid : startids) {
					for (String endid : endids) {
						if (startid.equals(endid)) {
							fragmentId = startid;
							break iter_startids;
						}
					}
				}
			}
			
			if (fragmentId == null) {
				log.info("creating new fragment");
				
				Context ctx = new Context();
				ctx.setInBegin(start);
				ctx.setInEnd(end);
			
				FragmentFacade ff = fragmentService.createFragment(item, FragmentFacade.class);			
				ctx.fragmentize(xom, ff.getKiwiIdentifier());
			
				fragmentService.saveFragment(ff);
				contentItemService.updateTextContentItem(ff.getDelegate(), content);
			
				taggingService.createTagging(currentContentItem.getTitle(), ff.getDelegate(), currentContentItem, currentUser);
				
				textContentChange = true;
			}
			else {				
				ContentItem fragment = contentItemService.getContentItemByKiwiId(fragmentId);
				
				// does the fragment already have the tag?
				for (ContentItem fragmentTag : taggingService.getTags(fragment)) {
					if (fragmentTag.equals(currentContentItem)) {
						log.info("matched fragment is already tagged");
						continue find;
					}
				}
				
				log.info("adding tag to an existing fragment #0", fragmentId);
				taggingService.createTagging(currentContentItem.getTitle(), fragment, currentContentItem, currentUser);
			}
		}
		
		if (textContentChange) {
			contentItemService.updateTextContentItem(item, xom);
		}
	}
	
	private void annotateInTransaction(Pattern p, ContentItem item) {
		try {
			if(Transaction.instance().isActive()) {
				Transaction.instance().commit();
			}
			Transaction.instance().begin();
			transactionService.registerSynchronization(
            		KiWiSynchronizationImpl.getInstance(), 
            		transactionService.getUserTransaction() );
			Transaction.instance().enlist(entityManager);
			entityManager.joinTransaction();
			
			annotateItem(p, item);
			
			Transaction.instance().commit();
		}
		catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		} catch (RollbackException e) {
			e.printStackTrace();
		} catch (HeuristicMixedException e) {
			e.printStackTrace();
		} catch (HeuristicRollbackException e) {
			e.printStackTrace();
		} catch (NotSupportedException e) {
			e.printStackTrace();
		}
	}
	
	public void annotate() {
		
		Pattern p = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.UNICODE_CASE);
		
		if (filter == null || "".equals(filter)) {
			for(ContentItem item : contentItemService.getContentItems()) {
				if (item.getTextContent() != null && 
						!item.isDeleted() && 
						!item.getResource().hasType(Constants.NS_KIWI_SPECIAL + "Fragment")) {
					annotateInTransaction(p, item);
				}
			}
		}
		else {
			KiWiSearchCriteria criteria = solrService.parseSearchString(filter);
			
			criteria.setLimit(Integer.MAX_VALUE);
			
			KiWiSearchResults results = solrService.search(criteria);
			for (SearchResult result : results.getResults()) {
				if (!result.getItem().isDeleted() && 
						result.getItem().getTextContent() != null && 
						!result.getItem().getResource().hasType(Constants.NS_KIWI_SPECIAL + "Fragment")) {
					annotateInTransaction(p, result.getItem());
				}
			}
		}
		
		groupedFragments = null;
	}
}
