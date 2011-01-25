package rrs.action;

import static kiwi.model.kbase.KiWiQueryLanguage.SPARQL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;


import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.informationextraction.InformationExtractionService;
import kiwi.api.multimedia.MultimediaService;
import kiwi.api.tagging.TaggingService;
import kiwi.api.triplestore.TripleStore;
import kiwi.context.CurrentApplicationFactory;
import kiwi.context.CurrentContentItemFactory;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.informationextraction.Suggestion;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.KiWiProperty;
import kiwi.model.tagging.Tag;
import kiwi.model.user.User;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.Text;
import nu.xom.ValidityException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.FilenameUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.TransactionPropagationType;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.log.Log;
import org.jboss.seam.transaction.Transaction;
//import org.primefaces.event.FileUploadEvent;
//import org.primefaces.model.UploadedFile;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

@AutoCreate
@Name("rrs.importAction")
@Scope(ScopeType.CONVERSATION)
public class ImportAction {
	
	private static final String pub = "http://nlp.fit.vutbr.cz/2010/publications#";
	
	@Logger 
	private Log log;
	
	@In(create=true)
	private MultimediaService multimediaService;
	
	@In(create=true)
	private ContentItemService contentItemService;
	
	@In(value="kiwi.informationextraction.informationExtractionService", create=true)
	private InformationExtractionService informationExtractionService;
	
	@In(create=true)
	private TripleStore tripleStore;
	
	@In(create=true)
	private CurrentContentItemFactory currentContentItemFactory;
	
	@In(create=true)
	private CurrentApplicationFactory currentApplicationFactory;
	
	@In(create=true)
	private TaggingService taggingService;

	@In
	private EntityManager entityManager;
	
	@In
	private KiWiEntityManager kiwiEntityManager;
	
	@In(create=true)
	private User currentUser;
	
	private String name;
	public String getName() { return this.name;	}
	public void setName(String name) { this.name = name; }
	
	private long size;
	public long getSize() { return this.size; }
	public void setSize(long size) {
		log.info("size #0", size);
		this.size = size; 
	}
	
	private String contentType;
	public String getContentType() { return this.contentType; }
	public void setContentType(String contentType) {
		log.info("content type: #0", contentType);
		this.contentType = contentType; 
	}

	private byte[] data;
	public byte[] getData() { 
		return this.data; 
	}
	public void setData(byte[] data) {
		log.info("Set data #0", data == null ? "null" : data.length);
		this.data = data;
	}
	
	private String plaintext;
	private String html;
	private ContentItem contentItem;
	private Collection<Suggestion> suggestions;
	
	private String tag;
	
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}

	private String titleText;
	public String getTitleText() {
		return titleText;
	}
	public void setTitleText(String titleText) {
		this.titleText = titleText;
	}

	private String abstractText;
	
	public String getAbstractText() {
		return abstractText;
	}
	public void setAbstractText(String abstractText) {
		this.abstractText = abstractText;
	}
	public void setPlaintext(String plaintext) {
		this.plaintext = plaintext;
	}
	
	public String getPlaintext() {
		return this.plaintext;
	}
	
	public void setHtml(String html) {
		this.html = html;
	}
	public String getHtml() {
		return html;
	}
	
	public void setContentItem(ContentItem contentItem) {
		this.contentItem = contentItem;
	}
	public ContentItem getContentItem() {
		return contentItem;
	}
	
	public void listener(UploadEvent event) throws IOException {
		// UploadedFile item = event.getFile();

		log.info("listener here");
		UploadItem item = event.getUploadItem();
		
        name = FilenameUtils.getName(item.getFileName()).toLowerCase();
        data = item.getData();
        contentType = multimediaService.getMimeType(name, data);
        size = data.length;
        
        log.info("File: '#0', size '#1' with type '#2' was uploaded", name, size, contentType);
	}
	
	private static String stripNonValidXMLCharacters(String in) {
		StringBuffer out = new StringBuffer();

		char current;

		if (in == null || ("".equals(in))) {
		    return "";
		} 
		for (int i = 0; i < in.length(); i++) {
		    current = in.charAt(i);
		    if ((current == 0x9) ||
			    (current == 0xA) ||
			    (current == 0xD) ||
			    ((current >= 0x20) && (current <= 0xD7FF)) ||
			    ((current >= 0xE000) && (current <= 0xFFFD)) ||
			    ((current >= 0x10000) && (current <= 0x10FFFF))) {
			out.append(current);
		    }
		}
		return out.toString();
	}

	/**
	 * Trivial conversion of plaintext to HTML. Blank line separates paragraphs.  
	 * @param plaintext
	 * @return
	 * @throws IOException 
	 */
	private static String plaintext2html(String plaintext) {
		String[] lines = plaintext.split("\n\n");
		
		Element root = new Element("div");
		Document doc = new Document(root);
	
		for (String line : lines) {
			Element p = new Element("p");
			p.appendChild(line);
			root.appendChild(p);
		}
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Serializer s = new Serializer(os) {
			protected void writeXMLDeclaration() {
				// do nothing
			}
		};
		
		try {
			s.write(doc);
			return os.toString("UTF-8");
		}
		catch(IOException x) {
			return null;
		}
	}

	public void doImport() throws URISyntaxException, HttpException, IOException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException, NotSupportedException {
		log.info("Import");
		
		HttpClient client = new HttpClient();
		PutMethod put = new PutMethod("http://athena3.fit.vutbr.cz:31480/pdftotext");
		put.setContentChunked(false);
		put.setRequestEntity(new ByteArrayRequestEntity(data, "application/x-pdf"));
		client.executeMethod(put);
				
		plaintext = put.getResponseBodyAsString();
		
		plaintext = stripNonValidXMLCharacters(plaintext);
		
		html = plaintext2html (plaintext);
		
		contentItem = contentItemService.createContentItem(); 
		contentItem.addType(tripleStore.createUriResource(pub + "Publication"));
		contentItem.addType(tripleStore.createUriResource(pub + "PublicationInterface"));
		
		contentItemService.updateMediaContentItem(contentItem, data, contentType, name);
		contentItemService.updateTextContentItem(contentItem, html);
		contentItemService.saveContentItem(contentItem);
		
		ContentItem mycollection = tripleStore.createUriResource(pub + "mycollection").getContentItem();
		taggingService.createTagging("mycollection", contentItem, mycollection, currentUser);

		// This is not clean, obviously
		// TODO: fix, 
		entityManager.flush();
		Transaction.instance().commit();
		Transaction.instance().begin();
		
		suggestions = informationExtractionService.extractSuggestions(contentItem.getResource(), contentItem.getTextContent(), contentItem.getLanguage());
		
		KiWiResource pubTitle = tripleStore.createUriResource(pub + "title");
		KiWiResource pubAbstract = tripleStore.createUriResource(pub + "abstract");
		
		for (Suggestion suggestion : suggestions) {
			if (suggestion.getKind() == Suggestion.DATATYPE) {
				if (suggestion.getRoles().contains(pubTitle)) {
					titleText = suggestion.getInstance().getContext().getInContext();
				}
				else if (suggestion.getRoles().contains(pubAbstract)) {
					abstractText = suggestion.getInstance().getContext().getInContext();
				}
			}
		}
		// HttpPut httpput = new HttpPut(requestUri);
       
        //ResponseHandler<String> responseHandler = new
//BasicResponseHandler();

	}
	
	/*
	public void edit() {
		// done editing, annotate.
				
		contentItemService.updateTextContentItem(contentItem, html);
		contentItemService.saveContentItem(contentItem);
		
		suggestions = informationExtractionService.extractSuggestions(contentItem.getResource(), contentItem.getTextContent(), contentItem.getLanguage());
		
		KiWiResource pubTitle = tripleStore.createUriResource(pub + "title");
		KiWiResource pubAbstract = tripleStore.createUriResource(pub + "abstract");
		
		for (Suggestion suggestion : suggestions) {
			if (suggestion.getKind() == Suggestion.DATATYPE) {
				if (suggestion.getRoles().contains(pubTitle)) {
					titleText = suggestion.getInstance().getContext().getInContext();
				}
				else if (suggestion.getRoles().contains(pubAbstract)) {
					abstractText = suggestion.getInstance().getContext().getInContext();
				}
			}
		}
	}*/
	
	public String annotate() {
		
		contentItemService.updateTextContentItem(contentItem, html);
		
		KiWiUriResource pubTitle = tripleStore.createUriResource(pub + "title");
		// KiWiUriResource pubAbstract = tripleStore.createUriResource(pub + "abstract");
		
		contentItemService.updateTitle(contentItem, titleText);
		tripleStore.createTriple(contentItem.getResource(), pubTitle, tripleStore.createLiteral(titleText));
		// tripleStore.createTriple(contentItem.getResource(), pubAbstract, tripleStore.createLiteral(abstractText));
		
		contentItemService.saveContentItem(contentItem);
		
		currentApplicationFactory.switchApplication("wiki");
		currentContentItemFactory.setCurrentItemKiWiId(contentItem.getKiwiIdentifier());
		currentContentItemFactory.refresh();
		
		return "/wiki/home.xhtml";
	}
	
	public List<ContentItem> listImportedPublications() throws NamespaceResolvingException {
		KiWiUriResource pubPublication = tripleStore.createUriResource(pub + "Publication");
		
		List<ContentItem> ret = new LinkedList<ContentItem>();
		Iterable<KiWiResource> nodes = pubPublication.listIncomingNodes("rdf:type");
		for (KiWiResource node : nodes) {
			ret.add(node.getContentItem());
		}
		
		return ret;
	}
	
	public List<ContentItem> listMyPublications() throws NamespaceResolvingException {
		String query = "SELECT ?C WHERE { " +
		" ?T <" + Constants.NS_HGTAGS + "taggedResource> ?C . " + 
		" ?T <" + Constants.NS_HGTAGS + "taggedBy> " + currentUser.getResource().getSeRQLID() + " . " +
		" ?T <" + Constants.NS_HGTAGS + "associatedTag> <" + pub + "mycollection> ." +
		"} ";
	
		Query q = kiwiEntityManager.createQuery(
				query, 
				SPARQL, ContentItem.class);
		
		return q.getResultList();
	}
	
	public String goTo(ContentItem item) {
		currentApplicationFactory.switchApplication("wiki");
		currentContentItemFactory.setCurrentItemKiWiId(item.getKiwiIdentifier());
		currentContentItemFactory.refresh();
		
		// Conversation.instance().endBeforeRedirect();
		
		return "/wiki/home.xhtml";
	}
	
	
	
	public void listenerXml(UploadEvent event) throws IOException {
		UploadItem item = event.getUploadItem();
		data = item.getData();
		size = data.length;
		
		log.info("listenerXml here, size: #0, data: #1", size, data);
	}
	
	public void doImportXml() throws ValidityException, ParsingException, IOException {
		String str = new String(data, "UTF-8");
		Builder b = new Builder();
		Document xom = b.build(str, null);
		
		// user has specified a tag that all these document should be tagged with
		ContentItem ciTag = null;
		if (tag != null && !"".equals(tag)) {
			ciTag = taggingService.parseTag(tag);
		}
		
		Nodes nodes = xom.query("/docs/doc");
		for (int i = 0; i < nodes.size(); ++i) {
			Element doc = (Element)nodes.get(i);
			
			String title = null;
			String abstrakt = null;
			String text = null;
			String url = null;
			
			Nodes titles = doc.query("title/text()");
			if (titles.size() == 1) {
				title = ((Text)titles.get(0)).getValue();
			}
			else {
				title = "UNTITLED";
			}
			
			Nodes abstrakts = doc.query("abstract/text()");
			if (abstrakts.size() == 1) {
				abstrakt = ((Text)abstrakts.get(0)).getValue();
			}
			
			Nodes texts = doc.query("text/text()");
			if (texts.size() == 1) {
				text = plaintext2html(((Text)texts.get(0)).getValue());
			}
			else {
				// interpret it as XHTML
				text = doc.getFirstChildElement("text").toXML();
			}
			
			Nodes urls = doc.query("url/text()");
			if (urls.size() == 1) {
				url = ((Text)urls.get(0)).getValue();
			}
			
			// let's overwrite content items with the same title.
			
			ContentItem item = null;
			if (!"UNTITLED".equals(title)) {
				item = contentItemService.getContentItemByTitle(title);
			}
			
			if (item == null) {
				item = contentItemService.createContentItem();
			}
			
			item.addType(tripleStore.createUriResource(pub + "Publication"));
			item.addType(tripleStore.createUriResource(pub + "PublicationInterface"));
			
			contentItemService.saveContentItem(item);
			
			ContentItem mycollection = tripleStore.createUriResource(pub + "mycollection").getContentItem();
			taggingService.createTagging("mycollection", item, mycollection, currentUser);
			if (ciTag != null) {
				taggingService.createTagging(ciTag.getTitle(), item, ciTag, currentUser);
			}
			
			contentItemService.updateTitle(item, title);
			contentItemService.updateTextContentItem(item, text);
			
			KiWiUriResource pubTitle = tripleStore.createUriResource(pub + "title");
			KiWiUriResource pubAbstract = tripleStore.createUriResource(pub + "abstract");
			
			tripleStore.createTriple(item.getResource(), pubTitle, tripleStore.createLiteral(title));
			if (abstrakt != null) {
				tripleStore.createTriple(item.getResource(), pubAbstract, tripleStore.createLiteral(abstrakt));
			}
			
			if (url != null) {
				try {
					URL u = new URL(url);
					URLConnection conn = u.openConnection();
				
					String contentType = conn.getContentType();
				
					ByteArrayOutputStream os = new ByteArrayOutputStream();
				
					InputStream stream = conn.getInputStream();
					byte[] buff = new byte[1024];
					int read = 0;
					while( (read = stream.read(buff)) != -1) {
						if (read > 0) {
							os.write(buff, 0, read);
						}
					}
								
					contentItemService.updateMediaContentItem(item, os.toByteArray(), contentType, url.substring(url.lastIndexOf("/")));
				}
				catch(Exception x) {
					log.error("Error loading article file from #0", x, url);
				}
			}
			
			contentItemService.saveContentItem(item);
			log.info("Imported #0", item.getTitle());
		}
	}
}
