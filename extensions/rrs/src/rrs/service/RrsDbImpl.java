package rrs.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.sql.DataSource;

import org.jboss.seam.annotations.Name;

@Stateless
@Name("rrs.db")
@TransactionManagement(TransactionManagementType.BEAN)
public class RrsDbImpl implements RrsDbLocal {

	@Resource(mappedName = "java:/RrsDatasource")
	DataSource rrsDS;
	
	@Override
	public List<Publication> getPublicationsByLikeTitle(String title) throws SQLException {
		
		List<Publication> ret = new LinkedList<Publication>();
		
		Connection conn = rrsDS.getConnection();
		try{
			PreparedStatement stmt = conn.prepareStatement("SELECT id, title from data.publication WHERE title LIKE ?");
			stmt.setString(1, title);
		
			
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				Publication p = new Publication();
				p.setId(res.getLong(1));
				p.setTitle(res.getString(2));
				ret.add(p);
			}
		}
		finally {
			conn.close();
		}
		
		return ret;
	}
	
	/*public String filePathToText(String path) {
		
	}*/
	/*
	public Publication getPublication(long id) {
		Connection conn = rrsDS.getConnection();
		try{
			PreparedStatement stmt = conn.prepareStatement("SELECT p.id, p.title, f.path ");
			stmt.setString(1, title);
		
			
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				Publication p = new Publication();
				p.setId(res.getLong(1));
				p.setTitle(res.getString(2));
				ret.add(p);
			}
		}
		finally {
			conn.close();
		}
	}*/
}
