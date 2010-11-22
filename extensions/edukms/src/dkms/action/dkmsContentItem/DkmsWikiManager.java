/*
	Copyright © dkms, artaround
 */
package dkms.action.dkmsContentItem;


public class DkmsWikiManager {

	private String version;
	
	private String wikiContent;
	
	private String wikiAuthor;

	public DkmsWikiManager(){
		
	}
	
	public DkmsWikiManager(String version, String wikiContent, String wikiAuthor){
		this.version = version;
		this.wikiContent = wikiContent;
		this.wikiAuthor = wikiAuthor;
	}
	
	public String getWikiAuthor() {
		return wikiAuthor;
	}

	public void setWikiAuthor(String wikiAuthor) {
		this.wikiAuthor = wikiAuthor;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getWikiContent() {
		return wikiContent;
	}

	public void setWikiContent(String wikiContent) {
		this.wikiContent = wikiContent;
	}
		
}
