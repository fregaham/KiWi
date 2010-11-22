/*
 * File : ImportEquityData.java.java Date : Apr 7, 2010 DO NOT ALTER OR REMOVE
 * COPYRIGHT NOTICES OR THIS HEADER. Copyright 2008 The KiWi Project. All rights
 * reserved. http://www.kiwi-project.eu The contents of this file are subject to
 * the terms of either the GNU General Public License Version 2 only ("GPL") or
 * the Common Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the License.
 * You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html or nbbuild/licenses/CDDL-GPL-2-CP.
 * See the License for the specific language governing permissions and
 * limitations under the License. When distributing the software, include this
 * License Header Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP. KiWi designates this particular file as
 * subject to the "Classpath" exception as provided by Sun in the GPL Version 2
 * section of the License file that accompanied this code. If applicable, add
 * the following below the License Header, with the fields enclosed by brackets
 * [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]" If you wish your
 * version of this file to be governed by only the CDDL or only the GPL Version
 * 2, indicate your decision by adding "[Contributor] elects to include this
 * software in this distribution under the [CDDL or GPL Version 2] license." If
 * you do not indicate a single choice of license, a recipient has the option to
 * distribute your version of this file under either the CDDL, the GPL Version 2
 * or to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL Version
 * 2 license, then the option applies only if the new code is made subject to
 * such option by the copyright holder. Contributor(s):
 */


package kiwi.admin.service.sunspace;


import java.util.Date;

import javax.persistence.EntityManager;

import kiwi.api.transaction.TransactionService;
import kiwi.model.ceq.SunSpaceImportKeyword;
import kiwi.service.transaction.KiWiSynchronizationImpl;

import org.jboss.seam.log.Log;
import org.jboss.seam.transaction.Transaction;
import org.jboss.seam.transaction.UserTransaction;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Used to parse and persist the Equity Keyword (tag) related
 * files. <br>
 * More preciselly this parser runs over a given xml file an
 * transforms its content in one or more
 * <code>SunSpaceImportKeyword</code> instances and persist this
 * instances. <br>
 * This class is not designed to be extended.
 *
 * @author mradules
 * @version 0.9
 * @since 0.9
 */
final class SunSpaceKeywordSaxParser extends DefaultHandler {

    /**
     * The number of items needed persisted in a transaction.
     */
    private static final int FLUSH_COUNT = 30;

    /**
     * Used to communicates with the underlying persistence
     * layer.
     */
    private final EntityManager entityManager;
    
    private final TransactionService transactionService;

    /**
     * All the log massages goes trough this field.
     */
    private final Log logger;

    /**
     * True if this parser parses a row.
     */
    private boolean inRow;

    /**
     * True if this parser parses a column.
     */
    private boolean inColumn;

    /**
     * Holds the name for the parsed property, only if I parse a
     * row.
     */
    private String property;

    /**
     * The parsed data object.
     */
    private SunSpaceImportKeyword keyword;

    private final ImportMonitor importMonitor;

    private final StringBuilder keywordIdBuffer;

    private final StringBuilder keywordNameBuffer;

    private final StringBuilder keywordControlledBuffer;

    /**
     * Counts the processed items.
     */
    private int itemCount;

    /**
     * The date when the import starts.
     */
    private Date startTime;

    /**
     * Builds a <code>CEQLogSaxParser</code> for a given list of
     * arguments.
     *
     * @param entityManager the persistence context.
     * @param log the logger used to log debug information.
     */
    SunSpaceKeywordSaxParser(final EntityManager entityManager, 
    		final TransactionService transactionService,
    		final Log log,
            ImportMonitor importMonitor) {
        this.entityManager = entityManager;
        this.transactionService = transactionService;
        logger = log;
        this.importMonitor = importMonitor;
        keywordIdBuffer = new StringBuilder();
        keywordNameBuffer = new StringBuilder();
        keywordControlledBuffer = new StringBuilder();
    }

    @Override
    public void startDocument() throws SAXException {
        startTime = new Date();
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        if ("row".equals(qName)) {
            keyword = new SunSpaceImportKeyword();
            inRow = true;
            property = null;
        }

        if ("column".equals(qName)) {
            property = attributes.getValue("name");
            inColumn = true;
        }

    }

    @Override
    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (inColumn) {
            final String value = new String(ch, start, length);
            if ("KEYWORD_ID".equals(property)) {
// keyword.setId(Long.valueOf(value));
// keyword.setKeywordId(Long.valueOf(value));
                keywordIdBuffer.append(value);
            } else if ("KEYWORD_NAME".equals(property)) {
// keyword.setKeywordName(value);
                keywordNameBuffer.append(value);
            } else if ("KEYWORD_CONTROLLED".equals(property)) {
// keyword.setKeywordControlled(Long.valueOf(value));
                keywordControlledBuffer.append(value);
            }
        }
    }

    /**
     * Populate the keyword entity with data.
     */
    private void store() {
        final String keywordId = keywordIdBuffer.toString();
        keyword.setId(Long.valueOf(keywordId));
        keyword.setKeywordId(Long.valueOf(keywordId));

        final String name = keywordNameBuffer.toString();
        keyword.setKeywordName(name);

        final String controlled = keywordControlledBuffer.toString();
        keyword.setKeywordControlled(Long.valueOf(controlled));
    }

    private void cleanBuffers() {
        keywordIdBuffer.delete(0, keywordIdBuffer.length());
        keywordNameBuffer.delete(0, keywordNameBuffer.length());
        keywordControlledBuffer.delete(0, keywordControlledBuffer.length());
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        property = null;
        try {
            if ("row".equals(qName)) {
                final UserTransaction transaction = Transaction.instance();
                if (!transaction.isActive()) {
                    transaction.begin();
                    transactionService.registerSynchronization(
                    		KiWiSynchronizationImpl.getInstance(), 
                    		transactionService.getUserTransaction() );
                }
                transaction.enlist(entityManager);
                inRow = false;

                store();
                cleanBuffers();
                // I use there because the (unique) id are set by
                // hand and hyberenate think that the entity has
                // already an persistence identity - this will
                // lead to an exception if I use persist.
                if(!entityManager.contains(keyword) && keyword.getId() != null) {
                	keyword = entityManager.merge(keyword);
                } else {
                	entityManager.persist(keyword);
                }

                if (itemCount % FLUSH_COUNT == 0) {
                    transaction.commit();
                }

                itemCount++;

                final StringBuilder message = new StringBuilder();
                message.append(itemCount);
                message.append(" SunSpaceImportKeyword items was imported.");

                importMonitor.notifyChange(message.toString());
            }
        } catch (final Exception e) {
            // I know, catching all exception is not a not nice
            // thing but in this case I am only interested if the
            // trans. works or not.
            logger.warn("The #0 can not be imported", keyword);
            logger.warn(e.getMessage(), e);
        }

        if ("column".equals(qName)) {
            inColumn = false;
        }
    }

    @Override
    public void endDocument() throws SAXException {

        try {
            final UserTransaction transaction = Transaction.instance();
            if (!transaction.isActive()) {
                transaction.begin();
                transactionService.registerSynchronization(
                		KiWiSynchronizationImpl.getInstance(), 
                		transactionService.getUserTransaction() );
            }
            transaction.enlist(entityManager);
//            entityManager.flush();

            transaction.commit();
        } catch (final Exception exception) {
            // I know, catching all exception is not a not nice
            // thing but in this case I am only interested if the
            // trans. works or not.
            logger.warn(exception.getMessage(), exception);
        }

        final Date now = new Date();
        final String msg =
                "Import of #0 ceq log was done in #1 seconds (#2 mins)";
        logger.debug(msg, itemCount,
                (now.getTime() - startTime.getTime()) * 1000,
                (now.getTime() - startTime.getTime()) * 1000 / 60);

    }

}
