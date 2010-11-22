/*
	Copyright © dkms, artaround
 */
package dkms.action.dkmsContentItem;


public class DkmsBlogManager {

	private String version;
	
	private String commentContent;
	
	private String commentAuthor;

	public DkmsBlogManager(){
		
	}
	
	public DkmsBlogManager(String version, String commentContent, String commentAuthor){
		this.version = version;
		this.commentContent = commentContent;
		this.commentAuthor = commentAuthor;
	}
	
	public String getCommentAuthor() {
		return commentAuthor;
	}

	public void setCommentAuthor(String commentAuthor) {
		this.commentAuthor = commentAuthor;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getCommentContent() {
		return commentContent;
	}

	public void setCommentContent(String commentContent) {
		this.commentContent = commentContent;
	}
		
}
