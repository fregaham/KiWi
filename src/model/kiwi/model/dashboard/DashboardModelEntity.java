package kiwi.model.dashboard;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.primefaces.model.DefaultDashboardModel;

@Entity
@NamedQueries({
    @NamedQuery(name = "select.dashboardModelFromDatabase", 
                query = "SELECT m FROM DashboardModelEntity AS m WHERE " +
                		"m.userlogin=:userlogin")             
})
public class DashboardModelEntity {

	private Long id;
	
	private String userlogin;
	
	private DefaultDashboardModel model;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
	public String getUserlogin() {
		return userlogin;
	}

	public void setUserlogin(String userlogin) {
		this.userlogin = userlogin;
	}

	@Basic
	public DefaultDashboardModel getModel() {
		return model;
	}

	public void setModel(DefaultDashboardModel model) {
		this.model = model;
	}

}

