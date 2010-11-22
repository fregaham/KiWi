package rrs.service;

import java.sql.SQLException;
import java.util.List;

public interface RrsDb {
	static class Publication {
		private long id;
		private String title;
		public void setTitle(String title) {
			this.title = title;
		}
		public String getTitle() {
			return title;
		}
		public void setId(long id) {
			this.id = id;
		}
		public long getId() {
			return id;
		}
	}
	
	List<Publication> getPublicationsByLikeTitle(String title) throws SQLException;
}
