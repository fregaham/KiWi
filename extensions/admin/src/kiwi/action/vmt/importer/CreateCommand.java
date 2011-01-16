

package kiwi.action.vmt.importer;


import java.net.URL;
import java.util.HashSet;
import java.util.Locale;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.importexport.importer.RDFImporter;
import kiwi.model.content.ContentItem;
import kiwi.model.ontology.SKOSConcept;

import org.jboss.seam.Component;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;


/**
 * This class is used to add to the underlaying persistence layer
 * a SKOS concept for a given RDF statement.
 *
 * @author mihai
 */
final class CreateCommand implements Runnable {

    private final Statement statement;

    private final ContentItemService contentItemService;

    private final KiWiEntityManager kiwiEntityManager;

    private final Log log;

    private final RepositoryConnection con;

    private final String urlConcept;

    private final String format;

    CreateCommand(Statement statement,
            ContentItemService contentItemService,
            KiWiEntityManager kiwiEntityManager, Log log,
            RepositoryConnection con, String url_concept, String format) {
        this.statement = statement;
        this.contentItemService = contentItemService;
        this.kiwiEntityManager = kiwiEntityManager;
        this.log = log;
        this.con = con;
        this.urlConcept = url_concept;
        this.format = format;
    }

    @Override
    public void run() {

        final Resource subject = statement.getSubject();
        final String conceptUri = subject.toString();

        String broaderURI;
        try {
            broaderURI = importRemoteConcept(conceptUri);
        } catch (Exception e) {
            log.debug(e.getMessage());
            throw new RuntimeException(e);
        }

        final ContentItem broader =
                contentItemService.getContentItemByUri(broaderURI);
        final ContentItem ci =
                contentItemService
                        .getContentItemByUriIncludeDeleted(conceptUri);

        log.debug("Try to create concept for statement : #0",
                statement.toString());

        if (broader != null) {

            log.debug("broaderuri " + broaderURI);
            log.debug(ci.getTitle());
            log.debug(broader.getTitle());

            SKOSConcept concept =
                    kiwiEntityManager.createFacade(ci, SKOSConcept.class);
            updateSKOSConcept(concept);
            SKOSConcept broaderConcept =
                    kiwiEntityManager.createFacade(broader, SKOSConcept.class);

            HashSet<SKOSConcept> skl = broaderConcept.getNarrower();
            skl.add(concept);
            broaderConcept.setNarrower(skl);

            kiwiEntityManager.persist(broaderConcept);

        } else {
            final String msg =
                    "Broader element can not be found ... Some elements may not be imported correctly";
            log.debug(msg);
            FacesMessages.instance().add(msg);
        }

        log.debug("Concept for statement  [#0] was succesful created.",
                statement.toString());

    }

    /**
     * Used to update the actual content item as follow :
     * <ul>
     * <li> sets the pref label for the given concept on
     * the same value like with the wraped content item title.
     * <li> sets the language to English.
     * </ul>
     *
     * @param concept the concept to update.
     */
    private void updateSKOSConcept(SKOSConcept concept) {
        final ContentItem item = concept.getDelegate();
        final String title = item.getTitle();
        concept.setPreferredLabel(title == null ? "no title" : title);
        final Locale language = concept.getLanguage();
        if (language == null) {
            concept.setLanguage(Locale.ENGLISH);
        }
    }

    private String importRemoteConcept(String uri) throws Exception {

        // collect subjects in a HashSet
        // HashSet<Resource> subjects = new HashSet<Resource>();
        String y = urlConcept + "?conceptURI=" + uri + "&format=" + format;
        log.debug(y);
        // String y = url_concept + File.separatorChar +
// uri.trim() + File.separatorChar + format + language;
        URL url1 = new URL(y);
        con.clear();
        con.add(url1, url1.toString(), RDFFormat.TURTLE);

        String broaderURI = getBroader(con);

        RDFImporter ddfImporter =
                (RDFImporter) Component
                        .getInstance("kiwi.service.importer.rdf");
        // ddfImporter.addDataSesame(con, null, null, null,
// null);
        ddfImporter.importDataSesame(con, null, null, null, null);

        return broaderURI;
    }

    private String getBroader(RepositoryConnection myCon) throws Exception {
        for (RepositoryResult<Statement> r =
                con.getStatements(null, null, null, false); r.hasNext();) {
            Statement stmt = r.next();
            if (stmt.getPredicate().toString().contains("broader")) {
                return stmt.getObject().toString();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer("Create Concept for [");
        result.append("Subject : ");
        result.append(statement.getSubject());
        result.append(", ");

        result.append("Predicat : ");
        result.append(statement.getSubject());
        result.append(", ");

        result.append("Object : ");
        result.append(statement.getObject());
        result.append("].");

        return result.toString();
    }
}
