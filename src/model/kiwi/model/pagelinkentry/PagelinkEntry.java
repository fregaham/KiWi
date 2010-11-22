/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package kiwi.model.pagelinkentry;

import javax.persistence.Entity;

import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import kiwi.model.content.ContentItem;
import org.hibernate.validator.NotNull;

/**
 *
 * @author Stefan
 */
@Entity
@NamedQueries({
    @NamedQuery(name="pagelinkService.allPagelinks",
                query="SELECT p FROM PagelinkEntry AS p"),
    @NamedQuery(name="pagelinkService.autocomplete",
                            query="select distinct ci.title " +
                                      "from ContentItem ci " +
                                      "where lower(ci.title) like :part " +
                                      "and ci.deleted = false and ci.author is not null " +
                                      "order by ci.title")
})
public class PagelinkEntry {


    @Id
    private String linkname;

    @OneToOne(fetch=FetchType.EAGER)
    @NotNull
    private ContentItem contentItem;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PagelinkEntry other = (PagelinkEntry) obj;
        if ((this.linkname == null) ? (other.linkname != null) : !this.linkname.equals(other.linkname)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + (this.linkname != null ? this.linkname.hashCode() : 0);
        return hash;
    }

    /**
     * @return the linkname
     */
    public String getLinkname() {
        return linkname;
    }

    /**
     * @param linkname the linkname to set
     */
    public void setLinkname(String linkname) {
        this.linkname = linkname;
    }

    /**
     * @return the contentItem
     */
    public ContentItem getContentItem() {
        return contentItem;
    }

    /**
     * @param contentItem the contentItem to set
     */
    public void setContentItem(ContentItem contentItem) {
        this.contentItem = contentItem;
    }

}
