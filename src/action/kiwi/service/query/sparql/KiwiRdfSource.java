package kiwi.service.query.sparql;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Stateless;

import kiwi.api.triplestore.TripleStore;
import kiwi.model.kbase.KiWiTriple;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

import name.levering.ryan.sparql.common.RdfSource;

@Name("kiwiRdfSource")
@AutoCreate
@Scope(ScopeType.STATELESS)
//@Stateless
public class KiwiRdfSource implements RdfSource {
	
	@Logger
	private Log log;
	
	@In
	private TripleStore tripleStore;
	
	private boolean checkval(Value s, URI p, Value o) {
		boolean ret = true;
		if( s == null){
			log.warn("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX subj null in query");
			ret = false;
		}
		if( p == null){
			log.warn("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX pred null in query");
			ret= false;
		}
		if( o == null){
			log.warn("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX obj  null in query");
			ret=false;
		}
		return ret;
	}
	public List<KiWiTriple> getSPO(Value subj, URI pred, Value obj) {
		int num = 0;
		if( subj == null) num+=4;
		if( pred == null) num+=2;
		if( obj == null)  num+=1;
		
		System.out.println("---------------------->" + num);
		
		List<KiWiTriple> result;
		switch(num){
		case 0: // none numm
			result = tripleStore.getTriplesBySPO(subj.stringValue(),pred.stringValue(),obj.stringValue()); 
			break;
		case 1: // o null
			result = new ArrayList<KiWiTriple>();
			//result = tripleStore.getTriplesBySP(subj.stringValue(),pred.stringValue());
			break;
		case 2:	// pred null
			result = new ArrayList<KiWiTriple>();
			//result = tripleStore.getTriplesBySO(subj.stringValue(),obj.stringValue());
			break;
			
		case 3:	// pred and obj null
			result = new ArrayList<KiWiTriple>();
			//result = tripleStore.getTriplesByS(subj.stringValue());
			break;
		case 4: // subj null
			System.out.println("++++++++++calling getTriplesByPO(pred.stringValue(),obj.stringValue()" + num);
			System.out.println("triple store ------" + tripleStore);
			result = tripleStore.getTriplesByPO(pred.stringValue(),obj.stringValue());
			break;
		case 5: // subj and obj null
			result = tripleStore.getTriplesByP(pred.stringValue());
			break;
		case 6: // subj and pred null
			result = new ArrayList<KiWiTriple>();
			//result = tripleStore.getTriplesByO(obj.stringValue());
			break;
		case 7: // all null
		default:
			result = new ArrayList<KiWiTriple>();
			break;
		
		}
		if( result == null) log.error("XXXXXXXXXXXXXXXXXXX result null");
		//TODO: add default context
		return result;
	}
	
	   /**
     * Gets all statements with a specific subject, predicate and/or object in
     * the default graph of the repository. All three parameters may be null to
     * indicate wildcards. This is only used in SPARQL queries when no graph
     * names are indicated.
     * 
     * @param subj subject of pattern
     * @param pred predicate of pattern
     * @param obj object of pattern
     * @return iterator over statements
     */
	@Override
	public Iterator getDefaultStatements(Value subj, URI pred, Value obj) {
		checkval(subj,pred,obj);
		final List<KiWiTriple> result = getSPO(subj,pred,obj);
		//TODO: add default context
		return result.iterator();
	}

	   /**
     * Gets all statements with a specific subject, predicate and/or object. All
     * three parameters may be null to indicate wildcards.
     * 
     * @param subj subject of pattern
     * @param pred predicate of pattern
     * @param obj object of pattern
     * @param graph the context with which to match the statements against
     * @return iterator over statements
     */
	@Override
	public Iterator getStatements(Value subj, URI pred, Value obj) {
		checkval(subj,pred,obj);
		final List<KiWiTriple> result = getSPO(subj,pred,obj);
//		final List<KiWiTriple> result = tripleStore.getTriplesBySPO(subj.stringValue(), pred.stringValue(), obj.stringValue());
		return result.iterator();
	}

	 /**
     * Gets all statements with a specific subject, predicate and/or object. All
     * three parameters may be null to indicate wildcards.
     * 
     * @param subj subject of pattern
     * @param pred predicate of pattern
     * @param obj object of pattern
     * @param graph the context with which to match the statements against
     * @return iterator over statements
     */
	@Override
	public Iterator getStatements(Value subj, URI pred, Value obj, URI graphcontext) {
		checkval(subj,pred,obj);
		final List<KiWiTriple> result = getSPO(subj,pred,obj);
//		final List<KiWiTriple> result = tripleStore.getTriplesBySPO(subj.stringValue(), pred.stringValue(), obj.stringValue());
		// TODO: add context
		return result.iterator();
	}

	@Override
	public ValueFactory getValueFactory() {
		return tripleStore.getRepository().getValueFactory();
//		return new ValueFactoryImpl();
	}

	@Override
	public boolean hasDefaultStatement(Value subj, URI pred, Value obj) {
		checkval(subj,pred,obj);
		final List<KiWiTriple> result = getSPO(subj,pred,obj);
//		final List<KiWiTriple> result = tripleStore.getTriplesBySPO(subj.stringValue(), pred.stringValue(), obj.stringValue());
		//TODO: add default context
		return result.isEmpty();
	}

	@Override
	public boolean hasStatement(Value subj, URI pred, Value obj) {
		checkval(subj,pred,obj);
		final List<KiWiTriple> result = getSPO(subj,pred,obj);
//		final List<KiWiTriple> result = tripleStore.getTriplesBySPO(subj.stringValue(), pred.stringValue(), obj.stringValue());
		return result.isEmpty();
	}

	@Override
	public boolean hasStatement(Value subj, URI pred, Value obj, URI graphcontext) {
		checkval(subj,pred,obj);
		final List<KiWiTriple> result = getSPO(subj,pred,obj);
//		final List<KiWiTriple> result = tripleStore.getTriplesBySPO(subj.stringValue(), pred.stringValue(), obj.stringValue());
		//TODO: add context
		return result.isEmpty();
	}

}
