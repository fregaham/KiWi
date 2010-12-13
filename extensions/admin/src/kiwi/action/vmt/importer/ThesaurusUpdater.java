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
package kiwi.action.vmt.importer;



import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.importexport.importer.RDFImporter;
import kiwi.model.content.ContentItem;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;
import org.joda.time.DateTime;
import org.openrdf.model.Statement;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;


/**
 * @author Rolf Sint
 * @version 0.7
 * @since 0.7
 *
 */
@AutoCreate
@Name("thesaurusUpdater")
@Scope(ScopeType.CONVERSATION)
public class ThesaurusUpdater {
    
    private static final String PREDICAT_REPLACED = "http://purl.org/dc/terms/replaced";

    private static final String PREDICAT_DEPRECATED = "http://www.w3.org/2002/07/owl#deprecated";

    private static final String PREDICAT_MODIFIED = "http://purl.org/dc/terms/modified";

    private static final String PREDICAT_CREATED = "http://purl.org/dc/terms/created";

    @Logger
    private Log log;
    
    @In
    private KiWiEntityManager kiwiEntityManager;
    
    @In
    ContentItemService contentItemService;
    
    @In 
    private  FacesMessages facesMessages;

    private String updateUri;
    
    private Date date;
    
    private RepositoryConnection con;
    
    private String format;
    
    private String url_concept;
    
    private java.util.Set<String> formats;
    
    @Create
    public void init(){
	formats = new HashSet<String>();
	formats.add("n3");
    }

    
    public void updateThesaurus() {

        log.debug("updating thesaurus begin.");

        final DateTime dt = new DateTime(date == null ? new Date() : date);
        updateUri = updateUri + "/" + dt.toLocalDateTime();

        log.debug("Updating thesaurus for date #0.", dt.toLocalDateTime());
        log.info("Update Ontology from : #0", updateUri);
        facesMessages.add("Update Ontology from : #0", updateUri);

        try {
            final Repository myRepository =
                    new SailRepository(new MemoryStore());
            myRepository.initialize();
            con = myRepository.getConnection();
        } catch (RepositoryException exception) {
            log.error(exception.getMessage(), exception);
            throw new ThesaurusUpdateException(exception);
        }

        updateUri += "?format=" + format;
        final List<Runnable> commands = new LinkedList<Runnable>();

        log.debug(updateUri);
        URL u;
        try {
            u = new URL(updateUri);
        } catch (MalformedURLException exception) {
            log.error(exception.getMessage(), exception);
            throw new ThesaurusUpdateException(exception);
        }

        try {
            con.add(u, u.toString(), RDFFormat.TURTLE);
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            throw new ThesaurusUpdateException(exception);
        }

        // get all concepts
        final GraphQueryResult rs;
        try {
            rs = con.prepareGraphQuery(
                    QueryLanguage.SERQL,
                    "CONSTRUCT * FROM {x} rdf:type {skos:Concept} USING NAMESPACE rdf = <http://www.w3.org/1999/02/22-rdf-syntax-ns#> , skos = <http://www.w3.org/2004/02/skos/core#> ")
                    .evaluate();
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            throw new ThesaurusUpdateException(exception);
        }

        int conceptIndex = 0;
        final ConceptUpdateException updateException = 
                new ConceptUpdateException();
        try {
            while (rs.hasNext()) {
                // process all the concepts
                log.debug("--- ");
                final Statement mainStatement = rs.next();
                updateException.setStatement(mainStatement);
                log.debug("Tries to process the statement : #0", mainStatement);
                // get all the triples with this subject
                final RepositoryResult<Statement> rs1 =
                        con.getStatements(mainStatement.getSubject(), null, null, true);

                log.debug("Try to process [Subject:#0, Predicat:#1, Object:#2]",
                        mainStatement.getSubject(),
                        mainStatement.getPredicate(),
                        mainStatement.getObject());
                // get all data about the concept
                while (rs1.hasNext()) {
                    // this loop determinate if a concept is
                    final Statement secStatement = rs1.next();
                    updateException.setStatement(secStatement);

                    // lls.add(st1);
                    final String subject = secStatement.getSubject().toString();
                    final String predicate = secStatement.getPredicate().toString();
                    final String object = secStatement.getObject().toString();
                    log.debug("Try to process Subject:#0, Predicat:#1, Object:#2 ",
                            subject, predicate, object);

                    if (predicate.equals(PREDICAT_CREATED)) {
                        final Runnable create =
                                new CreateCommand(mainStatement, contentItemService,
                                        kiwiEntityManager, log, con, url_concept,
                                        format);
                        commands.add(create);
                    }

                    if (predicate.equals(PREDICAT_MODIFIED)) {
                        final Runnable update =
                                new UpdateCommand(mainStatement, log, con, url_concept, format);
                        commands.add(update);
                    }

                    if (predicate.equals(PREDICAT_DEPRECATED)) {
                        final Runnable remove =
                                new RemoveCommand(mainStatement, contentItemService,
                                        kiwiEntityManager, log);
                        commands.add(remove);
                    }

                    if (predicate.equals(PREDICAT_REPLACED)) {
                    }
                }
                log.debug("[Subject:#0, Predicat:#1, Object:#2] was process.",
                        mainStatement.getSubject(), mainStatement.getPredicate(), mainStatement.getObject());
                log.debug("--- ");
                conceptIndex++;
            }
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            updateException.initCause(exception);
            throw updateException;
        }

        
        log.debug("#0 concepts was prcessed.", conceptIndex);
        for (Runnable command : commands) {
            log.debug("Try to execute command [#0].", command);
            try {
                command.run();
                log.debug("command [#0] was executed.", command);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
    
    //deletes old properties
    //imports a Concept with a given uri
    public void updateRemoteConcept(String uri) throws Exception{
		
		// collect subjects in a HashSet
		//HashSet<Resource> subjects = new HashSet<Resource>();
		String y = url_concept +"?conceptURI="+uri+"&format="+format;
		log.debug(y);
		//String y = url_concept + File.separatorChar + uri.trim() + File.separatorChar + format + language;
		try{
        		URL url1 = new URL(y);
        		con.clear();
        		con.add(url1, url1.toString(), RDFFormat.TURTLE);
        		
        		RDFImporter    ddfImporter = (RDFImporter)    Component.getInstance("kiwi.service.importer.rdf");
        		ddfImporter.importDataSesame(con, null, null, null, null);
        		}
		catch(Exception e){
		    	FacesMessages.instance().add("Something wrong happened. Most likely the urls to the webservices are not valid.");
		}
    }
    
    //imports a Concept with a given uri
    public String importRemoteConcept(String uri) throws Exception{
		
		// collect subjects in a HashSet
		//HashSet<Resource> subjects = new HashSet<Resource>();
		String y = url_concept +"?conceptURI="+uri+"&format="+format;
		log.debug(y);
		//String y = url_concept + File.separatorChar + uri.trim() + File.separatorChar + format + language;
		URL url1 = new URL(y);
		con.clear();
		con.add(url1, url1.toString(), RDFFormat.TURTLE);
		
		String broaderURI = getBroader(con);

		RDFImporter    ddfImporter = (RDFImporter)    Component.getInstance("kiwi.service.importer.rdf");
		//ddfImporter.addDataSesame(con, null, null, null, null);
		ddfImporter.importDataSesame(con, null, null, null, null);
		
		return broaderURI;
    }
    
    public String getBroader(RepositoryConnection myCon) throws Exception{
	for(RepositoryResult<Statement> r = con.getStatements(null, null, null, false) ; r.hasNext(); ) {
	    Statement stmt = r.next();
	    if(stmt.getPredicate().toString().contains("broader")){
		return stmt.getObject().toString();
	    }
	}
	return null;
    }

  

    public String getUpdateUri() {
        return updateUri;
    }

    public void setUpdateUri(String updateUri) {
        this.updateUri = updateUri;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }



    public String getFormat() {
        return format;
    }



    public void setFormat(String format) {
        this.format = format;
    }


    public String getUrl_concept() {
        return url_concept;
    }


    public void setUrl_concept(String urlConcept) {
        url_concept = urlConcept;
    }
    
    public java.util.Set<String> getFormats() {	
        return formats;
    }
    
    private boolean conceptExists(String uri) {
        final ContentItem item = contentItemService.getContentItemByUri(uri);
        return item == null;
    }
}
