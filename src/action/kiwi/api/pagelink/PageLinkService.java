/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package kiwi.api.pagelink;

import java.util.List;
import kiwi.model.pagelinkentry.PagelinkEntry;

/**
 *
 * @author Stefan
 */
public interface PageLinkService {

    public void savePageLink(PagelinkEntry entry) throws PageLinkFailedException;

    public boolean containsPageLink(String link);

    public void removePageLink(PagelinkEntry entry) throws PageLinkFailedException;

    public List<PagelinkEntry> getPageLinks();

    public PagelinkEntry getPageLink(String page) throws PageLinkFailedException;
    
    public List<String> getAutocompletResult(String contentItemTitle);
}
