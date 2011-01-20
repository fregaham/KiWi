/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2008-2009, The KiWi Project (http://www.kiwi-project.eu)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * - Neither the name of the KiWi Project nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Contributor(s):
 * 
 * 
 */

package kiwi.wiki.action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;

import kiwi.api.config.ConfigurationService;
import kiwi.api.content.ContentItemService;
import kiwi.api.event.KiWiEvents;
import kiwi.api.informationextraction.InformationExtractionService;
import kiwi.api.render.RenderingService;
import kiwi.api.security.CryptoService;
import kiwi.api.tagging.ExtendedTagCloudEntry;
import kiwi.api.tagging.TagCloudService;
import kiwi.api.tagging.TaggingService;
import kiwi.api.triplestore.TripleStore;
import kiwi.api.user.PasswordGeneratorService;
import kiwi.api.user.UserService;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.content.TextContent;
import kiwi.model.informationextraction.Suggestion;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.ontology.KiWiClass;
import kiwi.model.tagging.Tag;
import kiwi.model.user.User;
import kiwi.util.KiWiStringUtils;
import kiwi.wiki.action.ie.SuggestionsAction;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Synchronized;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;


/**
 * 
 * 
 * @author Marek Schmidt
 */

@Name("editorAction")
@Scope(ScopeType.CONVERSATION)
@Synchronized
public class EditorAction implements Serializable {

	private static final long serialVersionUID = -3628049977502899019L;

	@Logger
	private static Log log;
		
	// inject services
	@In
    private RenderingService renderingPipeline;
	
	@In
	private CryptoService cryptoService;
	
	private boolean renderKeyUpload;
	
    @In(create=true) 
	private User currentUser;
    
    @In
    private PasswordGeneratorService passwordGenerator;
    
    @In
    private ConfigurationService configurationService;

    @In
    private UserService userService;
    
    @In(required=true)
	private ContentItem currentContentItem;
    
    @In 
    FacesMessages facesMessages;
    
    @In
    TripleStore tripleStore;
    
    @In
    TaggingService taggingService;
    
    @In(create = true)
    TagCloudService tagCloudService;
    
    @In
    EntityManager entityManager;
    
    @In(value="kiwi.informationextraction.informationExtractionService")
    InformationExtractionService informationExtractionService;
   
    
    // private String title;
    // private String content;
    private String oldContent;
    
    private boolean encrypt = false;
    private boolean sign = false;
    private String passphrase;
    private File pemFile;
    private KeyPair keyPair;
    
    private User selectedUser;
    private List<User> encryptionUsers;
    private List<User> allUsers;
    
    @Out(value="editorAction.state")
    private EditorModel state;
    
    private boolean updated = false;
    
    private boolean initialized = false;

    private boolean suggestionsDisplayed = false;
    
    private String language;
    
    private List<String> languages;
    
	/**
	 * Start a nested conversation by loading the current content item as editor content.
	 * currentContentEditor is outjected into conversation scope for editing in edit.xhtml; the save button
	 * calls storeContentItem below and uses the currentContentEditor content as the new content item text.
	 * 
	 */
    @Create
    @Begin(join=true)
	public void begin() {
//		log.info("begin nested conversation, parentId is #0", Conversation.instance().getParentId());
//    	Conversation.instance().beginNested();
    	//Conversation.instance().setDescription("editorAction, contentItem: " + currentContentItem.getTitle());
		
    	String content;
    	
    	if(currentContentItem.getTextContent() != null) {
			content = renderingPipeline.renderEditor(currentContentItem, currentUser);
			log.debug("(conversation: #{conversation.id}) rendering editor content: #0", content);
    	} else {
			content = "<p></p>";   		
			log.debug("(conversation: #{conversation.id}) creating empty editor content: #0", content);
    	}
    	oldContent = content;
    	// title = currentContentItem.getTitle();
    	
    	if(encryptionUsers == null) {
    		encryptionUsers = new ArrayList<User>();
    	}
    	if(allUsers == null) {
        	allUsers = userService.getUsers();
        	allUsers.remove(userService.getAnonymousUser());	
    	}
    	
    	state = new EditorModel();
    
    	
    	// Set the language... We first check the RDF property, if somebody thought it would be better to use that one.
    	languages = getLanguages(); 
    	
    	language = currentContentItem.getResource().getProperty("<"+Constants.NS_KIWI_CORE+"language>");
		
    	if (language == null) {
    		if (currentContentItem.getLanguage() != null) {
    			language = currentContentItem.getLanguage().toString();
    		}
    	}
    	
    	if (language == null) {
    		language = "en_US";
    	}

    	// Make sure we always have 
    	if (!languages.contains(language)) {
    		languages.add(language);
    	}

    	updated = false;
    	suggestionsDisplayed = false;
    	
    	state.initModel(currentContentItem, currentUser, content);
    	
    	initialized = true;
	}
    
    private Locale parseLocale(String str) {
    	String[] langsplit = language.split("_");
		Locale newLocale = null;
		if (langsplit.length == 1) {
			newLocale = new Locale(langsplit[0]);
		}
		else if (langsplit.length == 2) {
			newLocale = new Locale(langsplit[0], langsplit[1]);
		}
		else if (langsplit.length == 3) {
			newLocale = new Locale(langsplit[0], langsplit[1], langsplit[2]);
		}
		else {
			log.error("Could not parse locale #0", language);
			newLocale = Locale.ENGLISH;
		}
		
		return newLocale;
    }
    
    public void storeTitle() {
    	ContentItemService contentItemService = (ContentItemService) Component.getInstance("contentItemService");
		contentItemService.updateTitle(currentContentItem, state.getTitle(getModel().getCurrentResource()));
    }
    
    public void restoreTitle() {
    	setTitle(currentContentItem.getTitle());
    }
    
    @Observer("refreshEditor")
    public void refreshEditor() {
    	initialized = false;
    	
    	log.info("Refreshing editor");
    }
	
	/**
     * Store the current content item from the editor. Ends the nested conversation.
	 * @throws NamespaceResolvingException 
     */
    public String storeContentItem() throws NamespaceResolvingException {
       	ContentItemService contentItemService = (ContentItemService) Component.getInstance("contentItemService");
    	
    	// We clear the suggestion fragments added to the editor.
		state.clearSuggestions();
		
    	String content = state.getHtml();
    	
    	//String title = state.getTitle(state.getCurrentResource());
    	//if (title == null) {
    	//	title = "";
    	//}
    	
    	// update title
//    	if(!title.equals(currentContentItem.getTitle())) {
//    		log.info("updating content item title from '#0' to '#1'", currentContentItem.getTitle(), title);
//    		
//    		contentItemService.updateTitle(currentContentItem, title);
//    		
//    		updated = true;
//    	}
    	
    	if (currentContentItem.getLanguage() == null || !language.equals(currentContentItem.getLanguage().toString())) {
    		
    		Locale newLocale = parseLocale(language);
    		currentContentItem.setLanguage(newLocale);
    		
			currentContentItem.getResource().removeProperty("<" + Constants.NS_KIWI_CORE + "language>");
    		currentContentItem.getResource().setProperty("<" + Constants.NS_KIWI_CORE + "language>", currentContentItem.getLanguage().toString());
    		
    		updated = true;
    	}
    	
    	if(currentContentItem.getTextContent() == null || !content.equals(oldContent)) {
	    
    		if(sign) {
    			byte[] signature = cryptoService.signContent(
    					content.getBytes(), currentUser.getPrivateKey());
    			content = KiWiStringUtils.appendHexSignature(content, signature);
    		}
    		
    		if(encrypt) {
    			if(content.startsWith("<p>") && content.endsWith("</p>")) {
    				content = content.substring(3, content.length()-4);
    			}
    			byte[] keyBytes = passwordGenerator.generatePassword(16).getBytes();
//    			byte[] keyBytes = "0123456789abcdef".getBytes();
        		byte[] cipher = cryptoService.encryptSymmetric(content.getBytes(), keyBytes, "AES");
        		
        		// encrypt with currentuser's public key
        		byte[] encryptedKey = cryptoService.encryptAsymmetric(keyBytes, currentUser);
        		// store key
        		String stringValue = KiWiStringUtils.toHex(encryptedKey, encryptedKey.length);
       			configurationService.setUserConfiguration(currentUser,currentUser.getLogin() + "-key:" + currentContentItem.getResource(),stringValue);
        		// encrypt with publik key from users who are allowed to read the text
       			for(User u : encryptionUsers) {
        			byte[] encryptedKeyRecipient = cryptoService.encryptAsymmetric(keyBytes, u);
            		// store key
        			if(encryptedKeyRecipient != null) {
	            		String stringValueRecipient = KiWiStringUtils.toHex(encryptedKeyRecipient, encryptedKeyRecipient.length);
	           			configurationService.setUserConfiguration(u,u.getLogin() + "-key:" + currentContentItem.getResource(),stringValueRecipient);
        			}
        		}
        		
        		content = new String(KiWiStringUtils.toHexHtml(cipher, cipher.length));
        	}
	    	long start = System.currentTimeMillis();

	    	log.info("Updating metadata and content");
	    	
	    	state.storeModel();
	    	
	    	// update content (returns true on change)
	    	contentItemService.updateTextContentItem(currentContentItem, content);
				
	    	log.debug("processed parsed content (#0ms)", System.currentTimeMillis()-start);
	    	
//    		currentUser = kiwiEntityManager.merge(currentUser);
    		
    		// update last author
    		currentContentItem.setAuthor(currentUser);
    		
     		// refresh user
//    		kiwiEntityManager.refresh(currentUser);
    		
    		
    		//Conversation.instance().end();
    	
    		log.info("stored new version of content item with kiwi id #0", currentContentItem.getResource().getKiwiIdentifier());
    		
    		updated = true;
     	}
    	else if(updated) {
    		// We need to update the metadata, even if the content didn't change.
    		
    		log.info("Updating just metadata");
    		
    		state.storeModel();
    		
    		Events.instance().raiseEvent(KiWiEvents.METADATA_UPDATED, currentContentItem);
       	}
    	
    	if(updated) {
    		// We save if updated, whether or not we updated the content
    		// update modification
    		currentContentItem.setModified(new Date());
    		
    		contentItemService.saveContentItem(currentContentItem);
    		
    		initialized = false;
    		    		
    		return "success";
    	} else {
    		return "";
    	}
    }

    public void cancel() {
//    	Conversation.instance().endAndRedirect();
    }
    
    public String getLanguage() {
    	return language;
    }
    
    public void setLanguage(String language) {
    	this.language = language;
    }
    
    public List<String> getLanguages() {
    	if (languages == null) {
    		List<String> def = new LinkedList<String>();
    		def.add("en_US");
    		languages= configurationService.getListConfiguration("kiwi.editor.languages", def);
    	}
    	
    	return languages;
    }
    
    private EditorModel getModel() {
    	
    	/*(if (state != null && state.getCurrentResource().getKiwiIdentifier() != currentContentItem.getKiwiIdentifier()) {
    		state = null;
    		initialized = false;
    	}*/
    	
    	if (state != null && initialized) {
    		return state;
    	}
    	
    	begin();
    	
    	return state;
    }
 
	/**
	 * @return the currentContentEditor
	 */
	public String getContent() {
		
		String html = getModel().getHtml();
		//log.info("getContent: #0", html);
		
		return html;
	}


	/**
	 * @param currentContentEditor the currentContentEditor to set
	 */
	public void setContent(String currentContentEditor) {
		log.info("(conversation: #{conversation.id}) setContent #0", currentContentEditor);
		
		this.getModel().setHtml(currentContentEditor);
	}

	/**
	 * @return the currentContentTitle
	 */
	public String getTitle() {
		EditorModel model = getModel();
		return model.getTitle(model.getCurrentResource());
	}



	/**
	 * @param currentContentTitle the currentContentTitle to set
	 */
	public void setTitle(String currentContentTitle) {
		EditorModel model = getModel();
		model.setTitle(model.getCurrentResource(), currentContentTitle);
	}



	public void setEncrypt(boolean encrypt) {
		this.encrypt = encrypt;
	}



	public boolean isEncrypt() {
		return encrypt;
	}
	
	public boolean isSign() {
		return sign;
	}



	public void setSign(boolean sign) {
		this.sign = sign;
	}



	public String getPassphrase() {
		return passphrase;
	}
	
	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}
	
	public List<User> getEncryptionUsers() {
		return encryptionUsers;
	}

	public void setEncryptionUsers(List<User> encryptionUsers) {
		this.encryptionUsers = encryptionUsers;
	}
	
	public User getSelectedUser() {
		return selectedUser;
	}

	public void setSelectedUser(User selectedUser) {
		this.selectedUser = selectedUser;
	}

	public List<User> getAllUsers() {
		return allUsers;
	}

	public void setAllUsers(List<User> allUsers) {
		this.allUsers = allUsers;
	}



	public void addToEncryptionUsers() {
		encryptionUsers.add(selectedUser);
		allUsers.remove(selectedUser);
	}

	public String checkPEMfile() {
		try {
			PEMReader pemreader = new PEMReader(
					new FileReader(pemFile), new DefaultPasswordFinder(
							passphrase.toCharArray()));
			keyPair = (KeyPair) pemreader.readObject();
//			System.out.println("priv key alg: " + keyPair.getPrivate().getAlgorithm());
//			System.out.println("pub key alg: " + keyPair.getPublic().getAlgorithm());
			
			currentUser.setPublicKey(keyPair.getPublic());
			currentUser.setPrivateKey(keyPair.getPrivate());
			facesMessages.add("Successfully uploaded PEM file.");
			return "success";
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			facesMessages.add("The PEM file you provided could not be read. Please upload a correct PEM file.");
			return "/wiki/edit.xhtml";
		} catch (IOException e) {
			e.printStackTrace();
			facesMessages.add("The PEM file you provided could not be read.");
			return "/wiki/edit.xhtml";
		}
	}

	public void listener(UploadEvent event) throws Exception{
        UploadItem item = event.getUploadItem();
        pemFile = item.getFile();
    }
	
	public void setRenderKeyUpload(boolean renderKeyUpload) {
		this.renderKeyUpload = renderKeyUpload;
	}



	public boolean isRenderKeyUpload() {
		if ((currentUser.getPrivateKey() == null) &&
    			configurationService
    				.isUserConfigurationSet(currentUser,currentUser.getLogin()+"-key:" + currentContentItem.getResource())) 
    		renderKeyUpload = true;
    	else 
    		renderKeyUpload = false;
		return renderKeyUpload;
	}

	public static class DefaultPasswordFinder implements PasswordFinder {

        private final char [] password;

        private DefaultPasswordFinder(char [] password) {
            this.password = password;
        }

        @Override
        public char[] getPassword() {
            return Arrays.copyOf(password, password.length);
        }
    }

	
	public List<KiWiTriple> getTypes() {
		
		List<KiWiTriple> ret = new LinkedList<KiWiTriple> ();
		EditorModel model = getModel();
		for (KiWiTriple triple: model.getOutgoingTriples(model.getCurrentResource())) {
			if (triple.isDeleted()) continue;
			
			if ((Constants.NS_RDF + "type").equals(triple.getProperty().getUri())) {
				ret.add(triple);
			}
		}
		
		return ret;
	}
	
	public void removeType(KiWiTriple triple) {
		log.info("removing triple.\n");
		
		updated = true;
		
		if (triple.getId() != null) {
			triple.setDeleted(true);
		}
		else {
			// if it is not persisted anyway, we may just safely remove it... nobody will know it even exited...
			EditorModel model = getModel();
			model.getOutgoingTriples(model.getCurrentResource()).remove(triple);
		}
	}
	
	private KiWiClass selectedType;
	
	public void setSelectedType(KiWiClass type) {
		selectedType = type;
	}
	
	public KiWiClass getSelectedType() {
		return selectedType;
	}
	
	public void associateContentType() {
		if (selectedType != null) {
			updated = true;

			EditorModel model = getModel();
			
			KiWiTriple triple = new KiWiTriple();
			triple.setSubject(model.getCurrentResource());
			triple.setProperty(tripleStore.createUriResourceByNamespaceTitle("rdf", "type"));
			triple.setObject(selectedType.getResource());
			model.getOutgoingTriples(currentContentItem.getResource()).add(triple);
		}
	}
	
	
	// Fragments...
	
	/**
	 * Currently edited fragment.
	 */
	KiWiResource fragment;
	
	/**
	 * Current (nested) item that is the current "context", 
	 * in this case, the content item containing the edited fragment.
	 */
	private KiWiResource currentContext;
	
	/**
	 * True if we are currently creating a new fragment, false if we are editing a fragment.
	 */
	private boolean fragmentCreating;
		
	public KiWiResource getFragment() {
		return fragment;
	}
	
	private void setCurrentContext(String kiwiid) {
		if ("".equals(kiwiid)) {
			currentContext = getModel().getCurrentResource();
		}
		else {
			currentContext = getModel().getResourceByNestedId(kiwiid);
		}
	}
	
	/** 
	 * Set the currently edited fragment from javascript client, 
	 * format: <context kiwiid> " " <fragment kiwiid>
	 * if <context kiwiid> is an empty string, current content item is considered a context
	 * if <fragment kiwiid> is an empty string, a new fragment CI shall be created.
	 * @param js
	 */
	public void setFragmentJS(String js) {
		
		log.info("setFragmentJS: #0", js);
		
		String[] split = js.split(" ", 2);
		setCurrentContext (split[0]);
		if ("".equals(split[1])) {
			fragment = getModel().createFragment(currentContext);
			fragmentCreating = true;
		}
		else {
			fragment = getModel().getResourceByFragmentId(split[1]);
			fragmentCreating = false;
		}
		
		taggedResource = fragment;
		tagLabel = "";
		tagCloud = null;
	}
	
	public KiWiResource getTaggedResource() {
		return taggedResource;
	}
	
	public List<KiWiTriple> getOutgoingTriples(KiWiResource resource) {
		return getModel().getOutgoingTriples(resource);
	}
	
	public void fragmentCreate() {
		//TODO: parse the tags.
		/*if (fragment != null) {
			fragmentService.saveFragment(fragmentFacade);
		}*/
	}
	
	public void fragmentDelete() {
		if (fragment != null) {
			getModel().deleteFragment(currentContext, fragment);
		}
	}
	
	public void fragmentCancel() {
		if (fragmentCreating) {
			if (fragment != null) {
				getModel().deleteFragment(currentContext, fragment);
			}
		}
	}
	
	public String getFragmentJS() {
		if (fragment == null) {
			return "";
		}
		else {
			return fragment.getKiwiIdentifier();
		}
	}
	

	// Tagging of fragments
	private List<ExtendedTagCloudEntry> tagCloud;
	private String tagLabel;
	
	/**
	 * The resource whose metadata are being edited right now (could be fragment, or potentially a nested item, or the item itself...)
	 * Both the tagging and commenting functionality uses this member (which supports the idea that there would be one 
	 * dialog with all the metadata including comments)
	 */
	private KiWiResource taggedResource;
	
	public List<String> autocomplete(Object param) {
		return taggingService.autocomplete(param.toString().toLowerCase());
	}
	
	public List<ExtendedTagCloudEntry> getTagCloud() {
		if(tagCloud == null && taggedResource != null) {
			tagCloud = tagCloudService.aggregateTags(getModel().getTags(taggedResource));
		}
		
		return tagCloud;
	}
	
	public void addTag(ContentItem taggingResource) {
		if (taggedResource != null) {
			updated = true;
			getModel().createTag(taggedResource, taggingResource);
			tagCloud = null;
		}
	}
	
	public void removeTag(ContentItem taggingResource) {
		if (taggedResource != null) {
			updated = true;
			getModel().removeTag(taggedResource, taggingResource);
			tagCloud = null;
		}
	}
	
	public String getTagLabel() {
		return tagLabel;
	}
	
	public void setTagLabel(String tagLabel) {
		this.tagLabel = tagLabel;
	}
	
	private void addTag(String label) {
		ContentItem taggingResource = state.createTaggingResource(label);
		state.createTag(taggedResource, taggingResource);
	}

	public void addTag() {
		if (taggedResource != null) {
			updated = true;
			/*String[] components = tagLabel.split(",");
			for(String component : components) {
				String label = component.trim();
				addTag(label);
			}*/
			
			addTag(tagLabel.trim());
			
			tagCloud = null;
		}
	}
	
	public List<EditorModel.Comment> getComments() {
		
		log.info("getComments");
		
		if (taggedResource != null) {
			return getModel().getComments(taggedResource);
		}
		else {
			return Collections.emptyList();
		}
	}
	
	public String getCommentHtml(EditorModel.Comment comment) {
		return comment.getHtml();
	}
	
	// Commenting
	
	private String comment;
	private String commentTitle;
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public String getComment() {
		return this.comment;
	}
	
	public void setCommentTitle(String title) {
		this.commentTitle = title;
	}
	
	public String getCommentTitle() {
		return this.commentTitle;
	}
	
	public void addComment() {
		if (taggedResource != null && commentTitle != null && comment != null) {
			updated = true;
			getModel().addComment(taggedResource, commentTitle, comment);
		}
	}
	
	/**
	 * The currently selected suggestion id.
	 */
	private String suggestionId;
	
	private List<Suggestion> typeSuggestions;
	private List<Suggestion> tagSuggestions;
	
	public String getSuggestionsDisplayed() {
		return suggestionsDisplayed ? "true" : "false";
	}
	
	// Information Extraction suggestions
	public void suggest() {
		EditorModel model = getModel();
		model.clearSuggestions();
		
		suggestionsDisplayed = true;
		
		typeSuggestions = new LinkedList<Suggestion> ();
		tagSuggestions = new LinkedList<Suggestion> ();
		
		TextContent tc = model.getTextContent();
		// log.info("Suggesting: #0", tc.getXmlDocument().toXML());
		
		Collection<Suggestion> suggestions = informationExtractionService.extractSuggestions(currentContentItem.getResource(), tc, parseLocale(language));
		
		for (Suggestion suggestion : suggestions) {
			
			// The text content is just a temporary one, we don't want to accidentally persist it... 
			suggestion.getInstance().setSourceTextContent(null);
			
			// Let's remember tag suggestions, we display them separately
			if (suggestion.getKind() == Suggestion.TAG) {
				// Check if the item does not already have this tag.
				boolean hasTag = false;
				for (Tag t : model.getTags(model.getCurrentResource())) {
					if (suggestion.getLabel().equals(t.getTaggingResource().getTitle())) {
						hasTag = true;
						break;
					}
				}
				
				if (!hasTag) {
					tagSuggestions.add(suggestion);
				}
			}
			
			// ...and the same for the type suggestions
			if (suggestion.getKind() == Suggestion.TYPE) {
				boolean hasType = false;
				iterateTypes: for (KiWiResource type : suggestion.getTypes()) {
					for (KiWiTriple triple: model.getOutgoingTriples(model.getCurrentResource())) {
						if (triple.isDeleted()) continue;
						
						if ((Constants.NS_RDF + "type").equals(triple.getProperty().getUri())) {
							if (triple.getObject().isUriResource()) {
								if (type.getKiwiIdentifier().equals(((KiWiResource)triple.getObject()).getKiwiIdentifier())) {
									hasType = true;
									break iterateTypes;
								}
							}
						}
					}
				}
				
				if (!hasType) {
					typeSuggestions.add(suggestion);
				}
			}
			
			if (suggestion.getInstance().getContext().getIsFragment()) {
				log.info("Suggestion extractlet: #0, kind: #1, label: #2, score: #3, feedbackscore: #4", suggestion.getExtractletName(), suggestion.getKind(), suggestion.getLabel(), suggestion.getScore(), suggestion.getFeedbackScore());
				log.info("#0<#1>#2", suggestion.getInstance().getContext().getInBegin(), suggestion.getInstance().getContext().getInContext(), suggestion.getInstance().getContext().getInEnd());

				if (suggestion.getFeedbackScore() >= 0.5f) {
					// We only let the suggestions that have high feedback score.
					// TODO: perhaps a limit on the number on displayed suggestions, sorted by "score".				
					if (!model.addSuggestion(suggestion)) {
						log.info("Failed to insert suggestion");
					}
				}
			}
		}
		
		SuggestionsAction suggestionsAction = (SuggestionsAction)Component.getInstance("ie.suggestions");
		suggestionsAction.setSuggestions(suggestions);
		// The TagCloudAction uses the suggestions action to retrieve current recommendations,
		// We update the recommendations...
		TagCloudAction tagCloudAction = (TagCloudAction)Component.getInstance("kiwi.wiki.tagCloudAction");
		if (tagCloudAction != null) {
			tagCloudAction.clear();
		}
		
		// We sort the tag and type suggestions by their scores.
		Comparator<Suggestion> suggestionScoreScomparator = new Comparator<Suggestion>() {
			@Override
			public int compare(Suggestion o1, Suggestion o2) {
				if (o1.getScore() > o2.getScore()){
					return -1;
				}
				else if (o1.getScore() < o2.getScore()) {
					return 1;
				}
				
				return 0;
			}
		};
		
		Collections.sort(tagSuggestions, suggestionScoreScomparator);
		Collections.sort(typeSuggestions, suggestionScoreScomparator);
	}
	
	public void hideSuggestions() {
		EditorModel model = getModel();
		model.clearSuggestions();
		suggestionsDisplayed = false;
	}
	
	public void realizeSuggestions() {
		EditorModel model = getModel();
		updated = true;
		for (String id : model.getSuggestionIds()) {
			model.realizeSuggestion(id);
		}
	}

	public void setSuggestionId(String suggestionId) {
		this.suggestionId = suggestionId;
	}

	public String getSuggestionId() {
		return suggestionId;
	}
	
	public void acceptSuggestion(Suggestion suggestion) {
		
		updated = true;
		
		EditorModel model = getModel();
		
		// TODO: disambiguation
		if (suggestion.getKind() == Suggestion.TAG) {
			if (suggestion.getResources().size() > 0) {
				KiWiResource tag = suggestion.getResources().get(0);
				model.createTag(model.getCurrentResource(), tag.getContentItem());
			}
			else {
				log.info("creating new tagging resource #0", suggestion.getLabel());
				ContentItem taggingResource = state.createTaggingResource(suggestion.getLabel());
				model.createTag(model.getCurrentResource(), taggingResource);
			}
		}
		else if (suggestion.getKind() == Suggestion.TYPE) {
			if (suggestion.getTypes().size() > 0) {
				KiWiResource type = suggestion.getTypes().get(0);
				
				KiWiTriple triple = new KiWiTriple();
				triple.setSubject(model.getCurrentResource());
				triple.setProperty(tripleStore.createUriResourceByNamespaceTitle("rdf", "type"));
				triple.setObject(type);
				model.getOutgoingTriples(model.getCurrentResource()).add(triple);				
			}
		}
		
		informationExtractionService.acceptSuggestion(suggestion, currentUser);
		typeSuggestions.remove(suggestion);
		tagSuggestions.remove(suggestion);
	}
	

	public void rejectSuggestion(Suggestion suggestion) {
		informationExtractionService.rejectSuggestion(suggestion, currentUser);
		typeSuggestions.remove(suggestion);
		tagSuggestions.remove(suggestion);
	}
	
	public void acceptSuggestion() {
		
		EditorModel model = getModel();
		if (suggestionId != null) {
			updated = true;

			Suggestion suggestion = model.getSuggestion(suggestionId);
			if (suggestion != null) {
				informationExtractionService.acceptSuggestion(suggestion, currentUser);
				// state.realizeSuggestion(suggestionId);
				model.removeSuggestion(suggestionId);
				
				typeSuggestions.remove(suggestion);
				tagSuggestions.remove(suggestion);
			}
		}
	}
	
	public void rejectSuggestion() {
		EditorModel model = getModel();
		if (suggestionId != null) {
			Suggestion suggestion = model.getSuggestion(suggestionId);
			if (suggestion != null) {
				informationExtractionService.rejectSuggestion(suggestion, currentUser);
				model.removeSuggestion(suggestionId);
				
				typeSuggestions.remove(suggestion);
				tagSuggestions.remove(suggestion);
			}
		}
	}
	
	private String nestedItemId;
	public void setNestedItemId(String nestedId) {
		nestedItemId = nestedId;
	}
	
	public void insertNestedItem() {
		EditorModel model = getModel();
		model.insertNestedItem(nestedItemId);
	}
}
