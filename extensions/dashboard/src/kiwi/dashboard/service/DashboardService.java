package kiwi.dashboard.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import kiwi.dashboard.cfg.DashboardConfiguration;
import kiwi.model.dashboard.DashboardModelEntity;
import kiwi.model.user.User;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.primefaces.model.DashboardColumn;
import org.primefaces.model.DefaultDashboardColumn;
import org.primefaces.model.DefaultDashboardModel;

@Name("dashboardService")
@Scope(ScopeType.STATELESS)
@AutoCreate
public class DashboardService {
	
	@In
	EntityManager entityManager;
	
	public DefaultDashboardModel getModel(User user) {
		DashboardModelEntity model = this.getUserSpecificModel(user);
		if( model != null ) return model.getModel();
		else return getDefaultModel();
	}
	
	/**
	 * returns a new model which contains all registered portlets
	 * @return
	 */
	private DefaultDashboardModel getDefaultModel() {
		DefaultDashboardModel model = new DefaultDashboardModel();
		DashboardColumn col1 = new DefaultDashboardColumn();
		DashboardColumn col2 = new DefaultDashboardColumn();
		DashboardColumn col3 = new DefaultDashboardColumn();
		
		//add all portlets to the dashboard
//		int i = 1;
//		for( String s : DashboardConfiguration.PORTLETS ) {
//			switch(i) {
//			case 1:
//				col1.addWidget(s);
//				i = 2;
//				break;
//			case 2:
//				col2.addWidget(s);
//				i = 3;
//				break;
//			case 3:
//				col3.addWidget(s);
//				i = 1;
//				break;
//			}
//		}
		
		//add welcome portlet
		col1.addWidget("welcome");
		
		model.addColumn(col1);
		model.addColumn(col2);
		model.addColumn(col3);
		
		return model;
	}
	
	public void saveModel(DefaultDashboardModel model, User user) {
		DashboardModelEntity m = getUserSpecificModel(user);
		if( m == null ) {
			m = new DashboardModelEntity();
			m.setUserlogin(user.getLogin());
		}
		m.setModel(model);
		entityManager.persist(m);
	}
	
	private DashboardModelEntity getUserSpecificModel(User user) {
		final Query query = entityManager.createNamedQuery("select.dashboardModelFromDatabase");
		query.setParameter("userlogin", user.getLogin());
		try {
			final DashboardModelEntity model =
                (DashboardModelEntity) query.getSingleResult();
			return model;
		} catch (NoResultException noResultException) {
			return null;
		}
	}
	
	public List<String> getPortlets(User user) {
		//TODO personalize
		return DashboardConfiguration.PORTLETS;
	}

}
