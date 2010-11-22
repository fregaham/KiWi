/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package kiwi.service.importexport.importer;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;

import kiwi.api.config.ConfigurationService;
import kiwi.api.content.ContentItemService;
import kiwi.api.event.KiWiEvents;
import kiwi.api.importexport.ImportService;
import kiwi.api.importexport.importer.ImporterLocal;
import kiwi.api.importexport.importer.ImporterRemote;
import kiwi.api.system.StatusService;
import kiwi.api.tagging.TaggingService;
import kiwi.api.triplestore.TripleStore;
import kiwi.api.user.UserService;
import kiwi.exception.UserExistsException;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.content.MediaContent;
import kiwi.model.content.TextContent;
import kiwi.model.kbase.KiWiAnonResource;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.status.SystemStatus;
import kiwi.model.tagging.Tag;
import kiwi.model.user.User;
import kiwi.service.importexport.KiWiImportException;
import kiwi.service.triplestore.TripleStoreUtil;
import kiwi.util.KiWiFormatUtils;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;

import org.apache.commons.io.IOUtils;
import org.drools.io.impl.ReaderInputStream;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.RaiseEvent;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.log.Log;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

/**
 *
 * @author Stefan
 */
@Stateless
@Name("kiwi.service.importer.kiwi")
@Scope(ScopeType.STATELESS)
public class KiWiImporterImpl implements ImporterLocal, ImporterRemote {

    private static final String USER = "user";
    private static final String ITEMS = "items";
    private static final String MEDIA = "media";
    private static final String METADATA = "metadata";
    private static final String BASEURI = "baseuri";
    @Logger
    private Log log;
    @In(create = true)
    private TripleStore tripleStore;
    @In(create = true)
    private ContentItemService contentItemService;
    @In(create = true)
    private UserService userService;
    @In(create = true)
    private TaggingService taggingService;
    @In(create = true)
    private ConfigurationService configurationService;
    @In
    private EntityManager entityManager;

    private String oldBaseUri;

    private static String[] mime_types = {
        "application/x-kiwi"
    };

    @Override
    @Observer(KiWiEvents.SEAM_POSTINIT)
    @BypassInterceptors
    public void initialise() {
        log.info("registering KiWi importer ...");

        ImportService ies = (ImportService) Component.getInstance("kiwi.core.importService");

        ies.registerImporter(this.getName(), "kiwi.service.importer.kiwi", this);
    }

    /**
     * Get the name of this importer. Used for presentation to the user and for internal
     * identification.
     *
     * @return a string uniquely identifying this importer
     */
    @Override
    public String getName() {
        return "KiWi";
    }

    /**
     * Get a description of this importer for presentation to the user.
     *
     * @return a string describing this importer for the user
     */
    @Override
    public String getDescription() {
        return "Importer for importing data from KiWi's own data format for DB-DUMPs";
    }

    /**
     * Get a collection of all mime types accepted by this importer. Used for automatically
     * selecting the appropriate importer in ImportService.
     *
     * @return a set of strings representing the mime types accepted by this importer
     */
    @Override
    public Set<String> getAcceptTypes() {
        return new HashSet<String>(Arrays.asList(mime_types));
    }

    /**
     * Import data from the input stream provided as argument into the KiWi database.
     * 
     * @param url the url from which to read the data
     * @param types the set of types to associate with each generated content item
     * @param tags the set of content items to use as tags
     * @param user the user to use as author of all imported data
     */
    @Override
    public int importData(URL url, String format, Set<KiWiUriResource> types, Set<ContentItem> tags, User user, Collection<ContentItem> output) throws KiWiImportException {
        try {
            return importData(url.openStream(), format, types, tags, user, output);
        } catch (IOException e) {
            log.error("I/O error while reading kiwi dump document", e);
            throw new KiWiImportException(e.getMessage());
        }

    }

    /**
     * Import data from the input stream provided as argument into the KiWi database.
     * <p>
     * Import function for formats supported by Sesame; imports the data first into a separate memory
     * repository, and then iterates over all statements, adding them to the current knowledge space.
     * This method also checks for resources that have a rdfs:label, dc:title, or skos:prefLabel and uses
     * it as the title for newly created ContentItems.
     *
     * @param is the input stream from which to read the data
     * @param types the set of types to associate with each generated content item
     * @param tags the set of content items to use as tags
     * @param user the user to use as author of all imported data
     */
    @Override
    @RaiseEvent("ontologyChanged")
    public int importData(InputStream is, String format, Set<KiWiUriResource> types, Set<ContentItem> tags, User user, Collection<ContentItem> output) throws KiWiImportException {

        try {
            return importData(inputStreamToZipFile(is));
        } catch (IOException e) {
            log.error("I/O error while reading kiwi dump document", e);
            throw new KiWiImportException(e.getMessage());
        } catch (ParsingException e) {
            log.error("the kiwi dump document could not be parsed", e);
            throw new KiWiImportException(e.getMessage());
        }
    }

    /**
     * Import data from the reader provided as argument into the KiWi database.
     * <p>
     * Import function for formats supported by Sesame; imports the data first into a separate memory
     * repository, and then iterates over all statements, adding them to the current knowledge space.
     * This method also checks for resources that have a rdfs:label, dc:title, or skos:prefLabel and uses
     * it as the title for newly created ContentItems.
     *
     * @param reader the reader from which to read the data
     * @param types the set of types to associate with each generated content item
     * @param tags the set of content items to use as tags
     * @param user the user to use as author of all imported data
     */
    @Override
    public int importData(Reader reader, String format, Set<KiWiUriResource> types, Set<ContentItem> tags, User user, Collection<ContentItem> output) throws KiWiImportException {
        return importData(new ReaderInputStream(reader), format, types, tags, user, output);
    }

    @Asynchronous
    private int importData(ZipFile zipfile) throws IOException, ParsingException, KiWiImportException {
        final StatusService statusService = (StatusService) Component.getInstance("kiwi.core.statusService");

        final SystemStatus status = new SystemStatus("kiwi importer");
        statusService.addSystemStatus(status);
        status.setMessage("import new data from a kiwi importfile");

        try {
            Enumeration<? extends ZipEntry> allEntries = zipfile.entries();
            List<ZipEntry> itemEntries = new ArrayList<ZipEntry>();
            HashMap<String, ContentItem> allItems = new HashMap();
//            HashMap<String, User> allUsers = new HashMap();

            int count = 0;

            // Find old baseUri
            log.info("read baseUri");
            while (allEntries.hasMoreElements()) {

                ZipEntry entry = (ZipEntry) allEntries.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                StringTokenizer st = new StringTokenizer(entry.getName(), "/");

                String directory = st.nextToken();
                String filename = st.nextToken();

                if (directory.equals(BASEURI)) {
                    importBaseUri(filename, zipfile.getInputStream(entry));
                }
            }

            // Import all user
            allEntries = zipfile.entries();
            log.info("read user");
            while (allEntries.hasMoreElements()) {

                ZipEntry entry = (ZipEntry) allEntries.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                StringTokenizer st = new StringTokenizer(entry.getName(), "/");

                String directory = st.nextToken();
                String filename = st.nextToken();

                if (directory.equals(USER)) {
                    importUser(filename, zipfile.getInputStream(entry));
                }
            }

            allEntries = zipfile.entries();
            log.info("read contentItems");
            // Import all contentItems
            while (allEntries.hasMoreElements()) {

                ZipEntry entry = (ZipEntry) allEntries.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                StringTokenizer st = new StringTokenizer(entry.getName(), "/");

                String directory = st.nextToken();
                String filename = st.nextToken();

                if (directory.equals(ITEMS)) {

                    importItems(filename, zipfile.getInputStream(entry), allItems);
                    itemEntries.add(entry);
                    count++;
                }
            }

            allEntries = zipfile.entries();
            log.info("read mediaContent");
            // Import all contentItems with mediaContent
            while (allEntries.hasMoreElements()) {

                ZipEntry entry = (ZipEntry) allEntries.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                StringTokenizer st = new StringTokenizer(entry.getName(), "/");

                String directory = st.nextToken();
                String filename = st.nextToken();

                if (directory.equals(MEDIA)) {

                    if (filename.endsWith("meta")) {
                        importMedia(filename, zipfile.getInputStream(entry),
                                zipfile.getInputStream(zipfile.getEntry(entry.getName().replaceFirst("meta", "bin"))), allItems);
                    }


                }
            }

            // Import tags
            log.info("read tags");
            for (ZipEntry entry : itemEntries) {
                importTags(entry, zipfile.getInputStream(entry), allItems);
            }

            allEntries = zipfile.entries();
            log.info("read metadata");
            // Import mediafile
            while (allEntries.hasMoreElements()) {

                ZipEntry entry = (ZipEntry) allEntries.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                StringTokenizer st = new StringTokenizer(entry.getName(), "/");

                String directory = st.nextToken();

                if (directory.equals(METADATA)) {

                    importMetadata(zipfile.getInputStream(entry));
                }
            }

            // TODO: Please use entityManager.flush() only for debugging 
            // and delete it if the importer works. The entityManager will flush at the
            // transaction end which saves some performance
            log.info("done!!! (Please wait some time for the flush process)");

            return count;

        } finally {
            statusService.removeSystemStatus(status);
        }
    }

    private void importBaseUri(String filename, InputStream inputStream) throws ParsingException, IOException, KiWiImportException {
        Document doc = new Builder().build(inputStream);

        Element root = doc.getRootElement();

        if (!root.getNamespaceURI().equals(Constants.NS_KIWI_EXPORT)) {

            throw new KiWiImportException("wrong namespace in root tag File: " + filename);
        }

        if (root.getChildElements("baseuri", Constants.NS_KIWI_EXPORT).size() != 0) {
            this.oldBaseUri = root.getChildElements("baseuri", Constants.NS_KIWI_EXPORT).get(0).getValue().trim();
        } else {

            throw new KiWiImportException("could not find a baseuri in import file see: " + filename);
        }
    }

    private void importUser(String entryName, InputStream inputStream) throws ParsingException, IOException, KiWiImportException {
        Document doc = new Builder().build(inputStream);

        Element root = doc.getRootElement();
        String login = null, firstname = null, lastname = null, passwordHash = null;

        if (!root.getNamespaceURI().equals(Constants.NS_KIWI_EXPORT)) {

            throw new KiWiImportException("wrong namespace in root tag File:" + entryName);
        }

        if (root.getChildElements("login", Constants.NS_KIWI_EXPORT).size() != 0) {
            login = root.getChildElements("login", Constants.NS_KIWI_EXPORT).get(0).getValue().trim();
        } else {
            throw new KiWiImportException("user need a login:" + entryName);
        }

        if (root.getChildElements("firstname", Constants.NS_KIWI_EXPORT).size() != 0) {
            firstname = root.getChildElements("firstname", Constants.NS_KIWI_EXPORT).get(0).getValue().trim();
        }

        if (root.getChildElements("lastname", Constants.NS_KIWI_EXPORT).size() != 0) {
            lastname = root.getChildElements("lastname", Constants.NS_KIWI_EXPORT).get(0).getValue().trim();
        }

        if (root.getChildElements("passwordHash", Constants.NS_KIWI_EXPORT).size() != 0) {
            passwordHash = root.getChildElements("passwordHash", Constants.NS_KIWI_EXPORT).get(0).getValue().trim();
        } else {
            throw new KiWiImportException("user need a password:" + entryName);
        }

        try {

            User u = userService.getUserByLogin(login);
            if (u != null) {
                u.setFirstName(firstname);
                u.setLastName(lastname);
                u.setPasswordHash(passwordHash);
                userService.saveUser(u);
                entityManager.flush();
            } else {
                u = userService.createUser(login, firstname, lastname, "needPassword");

                u.setPasswordHash(passwordHash);

                userService.saveUser(u);

                userService.addRole(login, "read");
            }
        } catch (UserExistsException e) {

            log.warn("User exist: #0", login);
        } catch (Exception e) {
            log.error("Unknown Exception #0 by #1", e.getMessage(), login);
        }
    }

    private void importItems(String entryName, InputStream inputStream, HashMap<String, ContentItem> allItems) throws ParsingException, IOException, KiWiImportException {
        Document doc = new Builder().build(inputStream);

        Element root = doc.getRootElement();
        ContentItem contentItem;

        if (root.getChildElements("title", Constants.NS_KIWI_EXPORT).size() != 0) {
            if(root.getChildElements("title", Constants.NS_KIWI_EXPORT).get(0).getValue().trim().equals("TagIt")) {
                int i = 0;
            }
        }

        if (!root.getNamespaceURI().equals(Constants.NS_KIWI_EXPORT)) {

            throw new KiWiImportException("wrong namespace in root tag File:" + entryName);
        }

        if (root.getChildElements("uri", Constants.NS_KIWI_EXPORT).size() != 0) {
            String uri = replaceFirst(root.getChildElements("uri", Constants.NS_KIWI_EXPORT).get(0).getValue().trim(), this.oldBaseUri, configurationService.getBaseUri());
            if (contentItemService.getContentItemByUriIncludeDeleted(uri) == null) {

                KiWiUriResource res = tripleStore.createUriResource(uri);

                contentItem = res.getContentItem();
            } else {
                contentItem = contentItemService.getContentItemByUriIncludeDeleted(uri);
                contentItem.setDeleted(false);
            }
            allItems.put(uri, contentItem);

        } else if (root.getChildElements("anon-id", Constants.NS_KIWI_EXPORT).size() != 0) {

            String anonid = root.getChildElements("anon-id", Constants.NS_KIWI_EXPORT).get(0).getValue().trim();
            if (contentItemService.getContentItemByAnonIdIncludeDeleted(anonid) == null) {
                KiWiAnonResource res = tripleStore.createAnonResource(anonid);

                res.setContentItem(new ContentItem(res));

                contentItem = res.getContentItem();
            } else {
                contentItem = contentItemService.getContentItemByAnonIdIncludeDeleted(anonid);
                contentItem.setDeleted(false);
            }
            allItems.put(anonid, contentItem);

        } else {

            throw new KiWiImportException("no identifier for KiWiAnonResource or KiWiUriResource File:" + entryName);
        }

        if (root.getChildElements("title", Constants.NS_KIWI_EXPORT).size() != 0) {
            contentItem.setTitle(root.getChildElements("title", Constants.NS_KIWI_EXPORT).get(0).getValue().trim());
        }

        if (root.getChildElements("modified", Constants.NS_KIWI_EXPORT).size() != 0) {
            try {
                contentItem.setModified(KiWiFormatUtils.ISO8601FORMAT.parse(root.getChildElements("modified", Constants.NS_KIWI_EXPORT).get(0).getValue().trim()));
            } catch (ParseException ex) {
                throw new KiWiImportException("wrong format by modified date File:" + entryName);
            }
        }

        if (root.getChildElements("created", Constants.NS_KIWI_EXPORT).size() != 0) {
            try {
                contentItem.setCreated(KiWiFormatUtils.ISO8601FORMAT.parse(root.getChildElements("created", Constants.NS_KIWI_EXPORT).get(0).getValue().trim()));
            } catch (ParseException ex) {
                throw new KiWiImportException("wrong format by created date File:" + entryName);
            }
        }

        if (root.getChildElements("author", Constants.NS_KIWI_EXPORT).size() != 0) {
            User user = userService.getUserByUri(replaceFirst(root.getChildElements("author", Constants.NS_KIWI_EXPORT).
                    get(0).getValue().trim().substring(5), this.oldBaseUri, configurationService.getBaseUri()));

            contentItem.setAuthor(user);
        }

        if (root.getChildElements("content", Constants.NS_KIWI_EXPORT).size() != 0) {
            TextContent contentFirst = contentItem.getTextContent() == null ? new TextContent() : contentItem.getTextContent();

//            if(content.getId() != null && content.getId() == 50530) {
//                int t = 0;
//            }
//            TextContent content = new TextContent();
            String tagContent = replaceAll(root.getChildElements("content", Constants.NS_KIWI_EXPORT).get(0).toXML(), this.oldBaseUri, configurationService.getBaseUri());

            if (contentFirst.getXmlString() == null || !contentFirst.getXmlString().equals(tagContent)) {
                TextContent contentNew = new TextContent();
                contentNew.setXmlString(tagContent);

                entityManager.persist(contentNew);

                contentItem.setTextContent(contentNew);
            }


        }

        contentItem.setMediaContent(null);
        contentItemService.saveContentItem(contentItem);

    }

    private void importMedia(String entryName, InputStream inputStreamMeta, InputStream inputStreamBin, HashMap<String, ContentItem> allItems) throws ParsingException, IOException, KiWiImportException {
        Document doc = new Builder().build(inputStreamMeta);

        Element root = doc.getRootElement();
        ContentItem contentItem = null;
        Date created = null;
        String filename = null, mimetype = null;
        MediaContent mc = new MediaContent();

        byte[] binary = IOUtils.toByteArray(inputStreamBin);

        if (root.getChildElements("uri", Constants.NS_KIWI_EXPORT).size() != 0) {
            String uri = replaceFirst(root.getChildElements("uri", Constants.NS_KIWI_EXPORT).get(0).getValue().trim(), this.oldBaseUri, configurationService.getBaseUri());
            if (!allItems.containsKey(uri)) {
                KiWiUriResource res = tripleStore.createUriResource(uri);

                res.setContentItem(new ContentItem(res));

                contentItem = res.getContentItem();
            } else {
                contentItem = allItems.get(uri);
                contentItem.setDeleted(false);
            }

        } else if (root.getChildElements("anon-id", Constants.NS_KIWI_EXPORT).size() != 0) {

            String anonid = root.getChildElements("anon-id", Constants.NS_KIWI_EXPORT).get(0).getValue().trim();
            if (!allItems.containsKey(anonid)) {
                KiWiAnonResource res = tripleStore.createAnonResource(anonid);

                res.setContentItem(new ContentItem(res));

                contentItem = res.getContentItem();
            } else {
                contentItem = allItems.get(anonid);
                contentItem.setDeleted(false);
            }

        }

        if (root.getChildElements("created", Constants.NS_KIWI_EXPORT).size() != 0) {
            try {
                created = KiWiFormatUtils.ISO8601FORMAT.parse(root.getChildElements("created", Constants.NS_KIWI_EXPORT).get(0).getValue().trim());
            } catch (ParseException ex) {
                throw new KiWiImportException("wrong format by modified date File:" + entryName);
            }
        }

        if (root.getChildElements("file-name", Constants.NS_KIWI_EXPORT).size() != 0) {
            filename = root.getChildElements("file-name", Constants.NS_KIWI_EXPORT).get(0).getValue().trim();
        }

        if (root.getChildElements("mime-type", Constants.NS_KIWI_EXPORT).size() != 0) {
            mimetype = root.getChildElements("mime-type", Constants.NS_KIWI_EXPORT).get(0).getValue().trim();
        }

        mc.setCreated(created);
        mc.setData(binary);
        mc.setFileName(filename);
        mc.setMimeType(mimetype);
        entityManager.persist(mc);

        contentItem.setMediaContent(mc);
        contentItemService.saveContentItem(contentItem);
    }

    private void importMetadata(InputStream inputStream) throws ParsingException, IOException {

        RDFFormat f = RDFFormat.RDFXML;

        String baseUri = configurationService.getBaseUri();

        try {
            // File sesameDataDir = new
            // File(configurationService.getWorkDir()+File.separator+Long.toHexString(System.currentTimeMillis()));
            Repository myRepository = new SailRepository(new MemoryStore());

            myRepository.initialize();
            RepositoryConnection myCon = myRepository.getConnection();

            if (inputStream != null) {

                long timer = System.currentTimeMillis();

                myCon.add(inputStream, baseUri, f, (Resource) null); // no context (null)
                // for the moment,
                // clear it all

                log.info("imported metadata into temporary repository (#0 ms)",
                        System.currentTimeMillis() - timer);

                createKiWiTriples(myCon);

            } else {
                log.error("could not load ontology; InputStream was null");
            }

            // myCon.clear();
            // myCon.clearNamespaces();
            myCon.close();
            myRepository.shutDown();
            // sesameDataDir.delete();
        } catch (RepositoryException ex) {
            log.error("error while importing Sesame data:", ex);
        } catch (RDFParseException ex) {
            log.error("parse error while importing Sesame data:", ex);
        } catch (IOException ex) {
            log.error("I/O error while importing Sesame data:", ex);
        }

    }

    private void createKiWiTriples(RepositoryConnection myCon) throws RepositoryException {

        HashSet<KiWiTriple> triples = new HashSet<KiWiTriple>();

        // ESCA-JAVA0123:
        for (RepositoryResult<Statement> r = myCon.getStatements(null, null, null, false); r.hasNext();) {
            Statement stmt = r.next();
            KiWiTriple triple = TripleStoreUtil.uncheckedSesameToKiWi(stmt);

            if(triple.getAuthor() != null && triple.getAuthor().getResource().isUriResource()) {
                KiWiUriResource author = ((KiWiUriResource) triple.getAuthor().getResource());
                author.setUri(replaceFirst(author.getUri(), this.oldBaseUri, configurationService.getBaseUri()));
            }

            if(triple.getObject().isUriResource()) {
                KiWiUriResource object = ((KiWiUriResource) triple.getObject());
                object.setUri(replaceFirst(object.getUri(), this.oldBaseUri, configurationService.getBaseUri()));
            }

            if(triple.getContext() != null) triple.getContext().setUri(replaceFirst(triple.getContext().getUri(), this.oldBaseUri, configurationService.getBaseUri()));

            triple.getProperty().setUri(replaceFirst(triple.getProperty().getUri(), this.oldBaseUri, configurationService.getBaseUri()));
            if(triple.getSubject().isUriResource()) {
                KiWiUriResource subject = ((KiWiUriResource) triple.getSubject());
                subject.setUri(replaceFirst(subject.getUri(), this.oldBaseUri, configurationService.getBaseUri()));
            }

            triples.add(triple);
        }
        tripleStore.storeTriplesUnchecked(triples);

        // ESCA-JAVA0123:
        for (RepositoryResult<Namespace> ns = myCon.getNamespaces(); ns.hasNext();) {
            Namespace ns1 = ns.next();

            tripleStore.setNamespace(ns1.getPrefix(), ns1.getName());
        }
    }

    private void importTags(ZipEntry entry, InputStream inputStream, HashMap<String, ContentItem> allItems) throws ParsingException, KiWiImportException, IOException {
        Document doc = new Builder().build(inputStream);

        Element root = doc.getRootElement();
        ContentItem taggedResource = null;

        if (root.getChildElements("uri", Constants.NS_KIWI_EXPORT).size() != 0) {
//            taggedResource =
//                    contentItemService.getContentItemByUri(root.getChildElements("uri", Constants.NS_KIWI_EXPORT).get(0).getValue().trim());
            taggedResource = allItems.get(replaceFirst(root.getChildElements("uri", Constants.NS_KIWI_EXPORT).get(0).getValue().trim(), this.oldBaseUri, configurationService.getBaseUri()));
        } else if (root.getChildElements("anon-id", Constants.NS_KIWI_EXPORT).size() != 0) {
//            taggedResource =
//                    contentItemService.getContentItemByUri(root.getChildElements("anon-id", Constants.NS_KIWI_EXPORT).get(0).getValue().trim());
            taggedResource = allItems.get(root.getChildElements("anon-id", Constants.NS_KIWI_EXPORT).get(0).getValue().trim());
        }


        if (root.getChildElements("taggings", Constants.NS_KIWI_EXPORT).size() != 0) {
            Elements taggings = root.getChildElements("taggings", Constants.NS_KIWI_EXPORT).get(0).getChildElements("tagging", Constants.NS_KIWI_EXPORT);

            for (int i = 0; i < taggings.size(); i++) {
                Element tag = taggings.get(i);

                User taggedBy = null;
                ContentItem taggingResource = null;
                Date time = null;
                KiWiResource res = null;

                if (tag.getChildElements("tagged-by", Constants.NS_KIWI_EXPORT).size() != 0) {
                    taggedBy = userService.getUserByUri(replaceFirst(tag.getChildElements("tagged-by", Constants.NS_KIWI_EXPORT).get(0).getValue().trim().substring(5), this.oldBaseUri, configurationService.getBaseUri()));
                }

                if (tag.getChildElements("tagging-date", Constants.NS_KIWI_EXPORT).size() != 0) {
                    try {
                        time = KiWiFormatUtils.ISO8601FORMAT.parse(tag.getChildElements("tagging-date", Constants.NS_KIWI_EXPORT).get(0).getValue().trim());
                    } catch (ParseException ex) {
                        throw new KiWiImportException("wrong format by modified date File:" + entry.getName());
                    }
                }

                if (tag.getChildElements("tagging-item", Constants.NS_KIWI_EXPORT).size() != 0) {
//                    taggingResource =
//                            contentItemService.getContentItemByUri(tag.getChildElements("tagging-item", Constants.NS_KIWI_EXPORT).get(0).getValue().substring(5).trim());
                    taggingResource = allItems.get(replaceFirst(tag.getChildElements("tagging-item", Constants.NS_KIWI_EXPORT).get(0).getValue().substring(5).trim(), this.oldBaseUri, configurationService.getBaseUri()));
                }

                if (tag.getChildElements("tag-resource-uri", Constants.NS_KIWI_EXPORT).size() != 0) {
                    String uri = replaceFirst(tag.getChildElements("tag-resource-uri", Constants.NS_KIWI_EXPORT).get(0).getValue().substring(5).trim(), this.oldBaseUri, configurationService.getBaseUri());
                    ContentItem item = null;

                    try {
                        item = contentItemService.getContentItemByUriIncludeDeleted(uri);
                        res = item.getResource();
                    } catch (Exception e) {
                    }

                    if (item == null) {
                        item = allItems.get(uri);
                        res = item.getResource();
                    }

                    if (item == null) {
                        res = tripleStore.createUriResource(uri);
                    }

                }

                Tag t = new Tag(taggedResource, taggedBy, taggingResource);
                t.setCreationTime(time);
                t.setResource(res);

                try {
                    taggingService.addTagging(t);
                } catch (Exception e) {
                    log.warn("Warning can not write tag "
                            + "by taggingService.addTagging(t): #0", e.getMessage());
                }

            }
        }
    }

    public ZipFile inputStreamToZipFile(InputStream inputStream) throws IOException {

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data, 0, inputStream.available());

        OutputStream out = new FileOutputStream("/tmp/kiwi/export_temp.kiwi");
        out.write(data);
        out.close();
        return new ZipFile("/tmp/kiwi/export_temp.kiwi");
    }



    /**
     * Help class to read zip entries of a InputStream
     */
    class CompressedFile {

        private ZipInputStream in;
        private Hashtable<ZipEntry, Long> entries = new Hashtable<ZipEntry, Long>();
        private Hashtable<String, ZipEntry> names = new Hashtable<String, ZipEntry>();

        public CompressedFile(InputStream input) throws IOException {
            this.in = new ZipInputStream(input);

            init();
        }

        protected void init() throws IOException {
            long offset = 0;

            while (in.available() > 0) {
                ZipEntry e = in.getNextEntry();
                if (e == null) {
                    break;
                }

                entries.put(e, new Long(offset));
                names.put(e.getName(), e);
                offset += e.getSize();
            }
        }

        public synchronized InputStream getInputStream(ZipEntry e) throws IOException {
            //VERY temporary
            byte[] b = new byte[(int) e.getSize()];
            long offset = ((Long) entries.get(e)).longValue();
            in.read(b, (int) offset, b.length);
            log.info(e.getName() + " Infos von: " + offset + "," + b.length);

            return new ByteArrayInputStream(b);
        }

        public synchronized InputStream getInputStream(String name) throws IOException {

            return getInputStream(names.get(name));
        }

        public void close() throws IOException {
            in.close();
        }

        public Enumeration<ZipEntry> entries() {
            return entries.keys();
        }
    }

    /**
     * Simple replaceFirst algo. without regex
     *
     * @param aInput
     * @param aOldPattern
     * @param aNewPattern
     * @return
     */
    private static String replaceAll(String aInput,String aOldPattern,String aNewPattern) {

        if (aOldPattern.equals("")) {
            throw new IllegalArgumentException("Old pattern must have content.");
        }

        StringBuffer result = new StringBuffer();
        //startIdx and idxOld delimit various chunks of aInput; these
        //chunks always end where aOldPattern begins
        int startIdx = 0;
        int idxOld = 0;
        while ((idxOld = aInput.indexOf(aOldPattern, startIdx)) >= 0) {
            //grab a part of aInput which does not include aOldPattern
            result.append(aInput.substring(startIdx, idxOld));
            //add aNewPattern to take place of aOldPattern
            result.append(aNewPattern);

            //reset the startIdx to just after the current match, to see
            //if there are any further matches
            startIdx = idxOld + aOldPattern.length();
        }
        //the final chunk will go to the end of aInput
        result.append(aInput.substring(startIdx));
        return result.toString();
    }

    /**
     * Simple replaceFirst algo. without regex
     *
     * @param aInput
     * @param aOldPattern
     * @param aNewPattern
     * @return
     */
    private static String replaceFirst(String aInput,String aOldPattern,String aNewPattern) {

        if (aOldPattern.equals("")) {
            throw new IllegalArgumentException("Old pattern must have content.");
        }

        StringBuffer result = new StringBuffer();
        //startIdx and idxOld delimit various chunks of aInput; these
        //chunks always end where aOldPattern begins
        int startIdx = 0;
        int idxOld = 0;
        if ((idxOld = aInput.indexOf(aOldPattern, startIdx)) >= 0) {
            //grab a part of aInput which does not include aOldPattern
            result.append(aInput.substring(startIdx, idxOld));
            //add aNewPattern to take place of aOldPattern
            result.append(aNewPattern);

            //reset the startIdx to just after the current match, to see
            //if there are any further matches
            startIdx = idxOld + aOldPattern.length();
        }
        //the final chunk will go to the end of aInput
        result.append(aInput.substring(startIdx));
        return result.toString();
    }
}
