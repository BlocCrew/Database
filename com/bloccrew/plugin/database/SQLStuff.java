package com.bloccrew.plugin.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SQLStuff {
		
	String url, database, user, pass;
	
	public SQLStuff(String l, String db, String u, String p){
		url = l;
		user = u;
		pass = p;
		database = db;
		
		//Create table if it doesn't exist
		execute("CREATE TABLE IF NOT EXISTS database_econ (uuid VARCHAR(255) UNIQUE, player VARCHAR(255), balance DOUBLE)");
		execute("CREATE TABLE IF NOT EXISTS database_plugins (plugin VARCHAR(255) UNIQUE, version VARCHAR(255), website VARCHAR(255))");
		execute("CREATE TABLE IF NOT EXISTS database_players (uuid VARCHAR(255) UNIQUE, player VARCHAR(255), firstJoined VARCHAR(255), recentlyJoined VARCHAR(255))");
	}
	
	public boolean execute(String query){
		try {
	      // create a mysql database connection
	      //String myDriver = "org.gjt.mm.mysql.Driver";
	      String myUrl = "jdbc:mysql://"+url+"/"+database;
	      Connection con = DriverManager.getConnection(myUrl, user, pass);
	      	      	      
	      /* create a sql date object so we can use it in our INSERT statement
	      Calendar calendar = Calendar.getInstance();
	      java.sql.Date startDate = new java.sql.Date(calendar.getTime().getTime());*/
	 
	      // the mysql statement
	      //String query = " insert into database_econ(playername, balance) values(norway240, 0)";
	      
	      Statement stmt = con.createStatement();
	      stmt.executeUpdate(query);
	      
	      stmt.close();
	      con.close();
	      return true;
	    } catch(Exception e) {
	      e.printStackTrace();
	      return false;
	    }
	
	}
	
}
