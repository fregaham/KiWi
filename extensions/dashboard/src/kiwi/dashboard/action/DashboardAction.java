package kiwi.dashboard.action;

import kiwi.dashboard.service.DashboardService;
import kiwi.model.user.User;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.primefaces.event.CloseEvent;
import org.primefaces.model.DashboardColumn;
import org.primefaces.model.DefaultDashboardModel;

@Name("dashboardAction")
@Scope(ScopeType.PAGE)
public class DashboardAction {
	
	private DefaultDashboardModel model;
	
	@Logger
	private Log log;
	
	@In(create=true)
	private User currentUser;
	
	@In
	private DashboardService dashboardService;
	
	@Create
	public void init() {
		log.info("init dashboard");
		model = dashboardService.getModel(currentUser);
	}
	
	public void save() {
		log.info("save dashboard");
		dashboardService.saveModel(model, currentUser);
	}
	
	public String addPortlet(String name) {
		for( int i = 0; i < model.getColumnCount(); i++ ) {
			DashboardColumn c = model.getColumn(i);
			for( int j = 0; j < c.getWidgetCount(); j++ ) {
				if( c.getWidget(j).equals(name) ) {
					return null;
				}
			}
		}
		log.info("add widget '#0'", name);
		model.getColumn(0).addWidget(name);
		save();
		return "/dashboard/home.seam";
	}
	
	public void removePortlet(CloseEvent event) {
		for( int i = 0; i < model.getColumnCount(); i++ ) {
			DashboardColumn c = model.getColumn(i);
			for( int j = 0; j < c.getWidgetCount(); j++ ) {
				if( c.getWidget(j).equals(event.getComponent().getId()) ) {
					String name = c.getWidget(j);
					c.removeWidget(name);
					log.info("remove widget '#0'", name);
					save();
					return;
				}
			}
		}
	}

	public void setModel(DefaultDashboardModel model) {
		this.model = model;
	}

	public DefaultDashboardModel getModel() {
		return model;
	}

}
