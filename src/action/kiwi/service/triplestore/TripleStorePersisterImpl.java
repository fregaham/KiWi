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
 * sschaffe
 * 
 */
package kiwi.service.triplestore;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import kiwi.api.triplestore.TripleStorePersisterLocal;
import kiwi.api.triplestore.TripleStorePersisterRemote;
import kiwi.model.kbase.KiWiNamespace;
import kiwi.model.kbase.KiWiTriple;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.cache.CacheProvider;
import org.jboss.seam.log.Log;
import org.openrdf.model.Statement;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 * The TripleStorePersister is a helper component for the TripleStore and the KiWiSynchronization.
 * <p>
 * The TripleStorePersister really stores the triple data in the database and Sesame repository, 
 * while the TripleStore itself performs all operations only in memory, to be persisted on
 * transaction commit by KiWiSynchronization.beforeCompletion()
 * <p>
 * TripleStorePersister should only be used by developers that know what they are doing. All triple
 * operations should be accessed solely through TripleStore itself.
 *
 * @author Sebastian Schaffert
 *
 */
@Stateless
@Name("kiwi.core.tripleStorePersister")
@Scope(ScopeType.STATELESS)
public class TripleStorePersisterImpl implements TripleStorePersisterLocal, TripleStorePersisterRemote {

	/**
	 * Get the seam logger for issuing logging statements.
	 */
	@Logger
	private static Log log;
	
	/**
	 * The Sesame 2 repository. We are using a native repository for efficient storage.
	 */
	@In(value="tripleStore.repository", create = true)
    private Repository repository;
    
    /**
     * The connection of this TripleStore to the Sesame 2 repository.
     */
	@In(value="tripleStore.repositoryConnection", create=true)
    private RepositoryConnection con;

	
    /**
 	 * The Java EE entity manager. Used for persisting and querying triples and nodes in the
 	 * relational database so that queries can make efficient use of OQL.
 	 */
 	@In
 	private EntityManager entityManager;

	
 	/**
 	 * The cache provider defined in Seam. Used extensively for caching triples and nodes in order to 
 	 * avoid unnecessary database access.
 	 */
 	@In
 	private CacheProvider cacheProvider;

 	
	/**
	 * Store a triple in the database and Sesame repository by calling entityManager.persist and
	 * RepositoryConnection.add; should only be called on transaction commit.
	 * 
	 * @param triple
	 */
 	@Override
	public void storeBaseTriple(KiWiTriple triple) {
 		if (!triple.isBase())
 			throw new IllegalArgumentException("An attempt to call storeBaseTriple on a triple that is not base: "+ triple);
	
		try {
			con.add(TripleStoreUtil.transformKiWiToSesame(repository.getValueFactory(),triple));
//			con.commit();
		} catch(RepositoryException ex) {
			log.error("error while storing triple in Sesame repository", ex);
		}
		
     	if(triple.getId() == null) {
    		entityManager.persist(triple);
    	} else if (!triple.isNew()) { //it already existed as inferred, we have to update it to set base = true
			// update immutable entity in database ...
			Query q = entityManager.createNamedQuery("tripleStore.setTripleBase");
			q.setParameter("id", triple.getId());
			int count = q.executeUpdate();
			if(count == 0) {
				log.error("error while setting triple as base: #0",triple);
			}     		
     	}
 	}
	
	/**
	 * Store a triple in the database and Sesame repository by calling entityManager.persist and
	 * RepositoryConnection.add; should only be called on transaction commit.
	 * 
	 * @param triple
	 */
 	@Override
	public void storeInferredTriple(KiWiTriple triple) {
 		if (!triple.isInferred())
 			throw new IllegalArgumentException("An attempt to call storeInferredTriple on a triple that is not inferred: "+ triple);
 		
		try {
			con.add(TripleStoreUtil.transformKiWiToSesame(repository.getValueFactory(),triple));
//			con.commit();
		} catch(RepositoryException ex) {
			log.error("error while storing triple in Sesame repository", ex);
		}
		
     	if(triple.getId() == null) {
    		entityManager.persist(triple);
    	} else if (!triple.isNew()) { //it already existed as base, we have to update it to set inferred = true
			// update immutable entity in database ...
			Query q = entityManager.createNamedQuery("tripleStore.setTripleInferred");
			q.setParameter("id", triple.getId());
			int count = q.executeUpdate();
			if(count == 0) {
				log.error("error while setting triple as inferred: #0",triple);
			}     		
     	}
 	}

	
	/**
	 * Batch operation to store a collection of triples in the database and Sesame repository by
	 * calling entityManager.persist and RepositoryConnection.add; should only be called on
	 * transaction commit.
	 * 
	 * @param triples
	 */
 	@Override
	public void storeBaseTriples(Collection<KiWiTriple> triples) {
 		//TODO: storing in batches at least for Sesame again
    	for(KiWiTriple triple : triples) {
    		storeBaseTriple(triple);
    	}
 		
 	}
 	
	/**
	 * Batch operation to store a collection of triples in the database and Sesame repository by
	 * calling entityManager.persist and RepositoryConnection.add; should only be called on
	 * transaction commit.
	 * 
	 * @param triples
	 */
 	@Override
	public void storeInferredTriples(Collection<KiWiTriple> triples) {
 		//TODO: storing in batches at least for Sesame again
    	for(KiWiTriple triple : triples) {
    		storeInferredTriple(triple);
    	}
 		
 	} 	
	
	/**
	 * Mark a triple as deleted in the database and remove it from the Sesame repository. Should
	 * only be called on transaction commit.
	 * 
	 * @param triple
	 */
 	@Override
	public void removeBaseTriple(KiWiTriple triple) {
 		if (!triple.isBase())
 			throw new IllegalArgumentException("An attempt to call removeBaseTriple on a triple that is not base: "+ triple);
 		
 		
 		if (!triple.isInferred()) { //if it is inferred is still is queryable
	 		// check caching (should be done in TripleStore, but we currently cannot ensure
	 		// that the triple is not added to the cache by some call to createTriple)!
	 		cacheProvider.remove("triples", TripleStoreUtil.createCacheKey(triple));
	 		
	 		
	    	try {
	        	con.remove(TripleStoreUtil.transformKiWiToSesame(repository.getValueFactory(),triple));
	            //getConnection().commit();
	        } catch(RepositoryException ex) {
	            log.error("error while removing triple from knowledge base:", ex);
	        }
 		}
        
    	if(triple.getId() != null) {
   		
    		// update loaded entity (no effect to database, as KiWiTriple is immutable)
    		triple.setDeleted(true);
    		triple.setBase(false);
    		
			// update immutable entity in database ...
			Query q = entityManager.createNamedQuery("tripleStore.deleteTriple");
			q.setParameter("id", triple.getId());
			int count = q.executeUpdate();
			if(count == 0) {
				log.error("error while deleting triple #0",triple);
			}
    	} else {
    		log.warn("Asked to remove a base triple with a null id: ", triple);
    	}
 		
 	}
	
	/**
	 * Batch operation to mark a collection of triples as deleted in the database and remove them 
	 * from the Sesame repository. Should only be called on transaction commit.
	 * @param triples
	 */
 	@Override
	public void removeBaseTriples(Collection<KiWiTriple> triples) {
 		// TODO: implement as batch operation!
 		for(KiWiTriple t : triples) {
 			removeBaseTriple(t);
 		}
 	}
	
	/**
	 * Mark a triple as not inferred and delete it permanently if it is not and never was base.
	 * Also removes from Sesame when appropriate.
	 * 
	 * Should be called only by the reasoner.
	 * 
	 * @param triple
	 */
 	@Override
	public void removeInferredTriple(KiWiTriple triple) {
 		if (!triple.isInferred())
 			throw new IllegalArgumentException("An attempt to call removeInferredTriple on a triple that is not inferred: "+ triple);
 		
 		if (! triple.isBase()) { //if the triple is base then it will still be "not deleted" and thus queryable
 	 		// check caching (should be done in TripleStore, but we currently cannot ensure
 	 		// that the triple is not added to the cache by some call to createTriple)!
 	 		cacheProvider.remove("triples", TripleStoreUtil.createCacheKey(triple));

 			try {
	        	con.remove(TripleStoreUtil.transformKiWiToSesame(repository.getValueFactory(),triple));
	            //getConnection().commit();
	        } catch(RepositoryException ex) {
	            log.error("Error while removing triple from Sesame:", ex);
	        }
 		}
 		
    	if(triple.getId() != null) {
    		// update loaded entity (no effect to database, as KiWiTriple is immutable)
    		triple.setInferred(false);

    		if (triple.isBase() || triple.isDeleted()) { //if a triple is OR WAS base        		
    			// update immutable entity in database ...
    			Query q = entityManager.createNamedQuery("tripleStore.setTripleNotInferred");
    			q.setParameter("id", triple.getId());
    			int count = q.executeUpdate();
    			if(count == 0) {
    				log.error("Error while setting a triple as not inferred: #0",triple);
    			}    			
    		} else { //the triple is not and never was base (i.e. it is not versioned)
    			Query q = entityManager.createNamedQuery("tripleStore.reallyDeleteTriple");
    			q.setParameter("id", triple.getId());
    			int count = q.executeUpdate();
    			if(count == 0) {
    				log.error("Error while deleting a triple permanently from the database: #0",triple);
    			}    			    			
    		}
    		
    	} else {
    		log.warn("Asked to remove an inferred triple with a null id: ", triple);
    	}
 		
 	}

 	
	/**
	 * Batch operation to delete inferred triples. 
	 * Plus removes from Sesame when appropriate.
	 * @param triples
	 */
 	@Override
	public void removeInferredTriples(Collection<KiWiTriple> triples) {
 		// TODO: implement as batch operation!
 		for(KiWiTriple t : triples) {
 			removeInferredTriple(t);
 		}
 	}
	 	
 	/**
	 * Store a namespace in the database and Sesame repository by calling entityManager.persist and
	 * RepositoryConnection.setNamespace. Should only be called on transaction commit.
	 * @param ns
	 */
 	@Override
	public void storeNamespace(KiWiNamespace ns) {
 		log.debug("Storing namespace #0 : #1 ", ns.getPrefix(), ns.getUri());
        try {
            if(ns.getId() == null) {
            	log.debug("Persisting namespace #0 : #1 ", ns.getPrefix(), ns.getUri());
	            entityManager.persist(ns);
            } else {
            	removeNamespace(ns);            	
            	KiWiNamespace nns = new KiWiNamespace(ns.getPrefix(),ns.getUri());
            	entityManager.persist(nns);
            	
            	log.debug("removing and restoring immutable namespace #0 : #1 ", ns.getPrefix(), ns.getUri());
            }
            con.setNamespace(ns.getPrefix(),ns.getUri());
        } catch(RepositoryException ex) {
            log.error("error while setting namespace with prefix #0",ns.getPrefix(),ex);
            throw new UnsupportedOperationException("error while setting namespace with prefix "+ns.getPrefix(),ex);
        }
 		
 	}
	
	/**
	 * Mark a namespace as deleted in the database and remove it from the Sesame repository by
	 * calling RepositoryConnection.removeNamespace. Should only be called on transaction commit.
	 * @param ns
	 */
 	@Override
	public void removeNamespace(KiWiNamespace ns) {
 		log.debug("Removing namespace #0 : #1 ", ns.getPrefix(), ns.getUri());
        if(ns.getId() == null) {
   			// does this ever happen? Hopefully not...
   			ns.setDeleted(true);
   			
    		entityManager.persist(ns);
    		log.warn("persisted removed namespace, which does not really make sense (#0)",ns.toString());
        } else {
        	// for the cache (not persisted, immutable entity...)
			ns.setDeleted(true);
			
			// update immutable entity in database ...
			Query q = entityManager.createNamedQuery("tripleStore.deleteNamespace");
			q.setParameter("id", ns.getId());
			int count = q.executeUpdate();
			if(count == 0) {
				log.error("error while deleting namespace #0 in database",ns);
			}
        }
        
        
    	try {
    		cacheProvider.remove(TripleStoreImpl.CACHE_NS_BY_PREFIX,ns.getPrefix());
            con.removeNamespace(ns.getPrefix());
        } catch(RepositoryException ex) {
            log.error("error while removing namespace with prefix #0",ns.getPrefix(),ex);
            throw new UnsupportedOperationException("error while removing namespace with prefix "+ns.getPrefix(),ex);
        }
 		
 	}
 	
 	
	/**
	 * Flush the triple store by writing all unsaved changes to the file system.
	 */
    public synchronized void flush() {
    	try {
    		log.debug("Flushing Sesame triplestore");
			con.commit();
//    		con.close();
		} catch (RepositoryException e) {
			log.error("error while trying to commit repository connection: #0",e.getMessage(), e);
		}
    }

    /**
	 * Rolls back the updates on the triple store
	 */
    public synchronized void rollback() {
    	try {
    		log.info("Rollback Sesame triplestore");
			con.rollback();
//    		con.close();
		} catch (RepositoryException e) {
			log.error("error while trying to commit repository connection: #0",e.getMessage(), e);
		}
    }

}
