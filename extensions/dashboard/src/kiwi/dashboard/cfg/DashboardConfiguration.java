package kiwi.dashboard.cfg;

import java.util.LinkedList;
import java.util.List;

public class DashboardConfiguration {
	
	public static final List<String> PORTLETS;
	
	static {
		PORTLETS = new LinkedList<String>();
		PORTLETS.add("activities");
		PORTLETS.add("friends");
		PORTLETS.add("tagcloud");
		PORTLETS.add("history");
		PORTLETS.add("recommendations");
		PORTLETS.add("preference");
		PORTLETS.add("rule");			
		//add new portlets here
	}
	
}
