

package kiwi.action.vmt.importer;


import java.net.URL;


import kiwi.api.importexport.importer.RDFImporter;

import org.jboss.seam.Component;

import org.jboss.seam.log.Log;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;


/**
 * This class is used to update to the underlaying persistence layer
 * a SKOS concept for a given RDF statement.
 * 
 * @author mihai
 */
final class UpdateCommand implements Runnable {

    private final Statement statement;

    private final Log log;

    private final RepositoryConnection con;

    private final String urlConcept;

    private final String format;

    UpdateCommand(Statement statement, Log log,
            RepositoryConnection con, String url_concept, String format) {
        this.statement = statement;
        this.log = log;
        this.con = con;
        this.urlConcept = url_concept;
        this.format = format;
    }

    @Override
    public void run() {

        final Resource subject = statement.getSubject();
        final String conceptUri = subject.toString();

        // collect subjects in a HashSet
        // HashSet<Resource> subjects = new HashSet<Resource>();
        String y =
                urlConcept + "?conceptURI=" + conceptUri + "&format=" + format;
        // String y = url_concept + File.separatorChar +
// uri.trim() + File.separatorChar + format + language;
        log.debug("Try to update concept [#0] with : #1", conceptUri, statement);
        try {
            URL url1 = new URL(y);
            con.clear();
            con.add(url1, url1.toString(), RDFFormat.TURTLE);

            final RDFImporter ddfImporter =
                    (RDFImporter) Component
                            .getInstance("kiwi.service.importer.rdf");
            ddfImporter.importDataSesame(con, null, null, null, null);
            log.debug("Concept for [#0] as succesful updated.", conceptUri);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer("Update Concept for [");
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
