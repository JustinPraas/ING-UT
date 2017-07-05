package logging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import database.SQLiteDB;
import logging.Log.Type;
import server.rest.ServerModel;

public class Logger {
    
	public static ArrayList<Log> getLogs(String startDate, String endDate) throws ParseException {
    	long start = parseDateToMillis(startDate);
    	long end = parseDateToMillis(endDate) + 3600000 /*add one day so the criteria 'between' can be used*/;

		ArrayList<Log> result = new ArrayList<>();
    	Connection c = SQLiteDB.openConnection();	
		ResultSet rs;
		try {
			PreparedStatement s = c.prepareStatement("SELECT * FROM logs WHERE timestamp >= ? AND timestamp <= ? ORDER BY timestamp ASC;");
			s.setLong(1, start);
			s.setLong(2, end);
			rs = s.executeQuery();
			while (rs.next()) {
				Log log = new Log();
				log.setMessage(rs.getString("message"));
				log.setTimestamp(Long.parseLong(rs.getString("timestamp")));
				log.setType(Type.valueOf(rs.getString("type")));
				result.add(log);
			}
		} catch (NumberFormatException | SQLException e) {
			e.printStackTrace();
		}
		
		return result;		
    }
    
    public static ArrayList<HashMap<String, Object>> getEventLogs(String startDate, String endDate) throws ParseException {
    	ArrayList<Log> logs = getLogs(startDate, endDate);
    	ArrayList<HashMap<String, Object>> result = new ArrayList<>();
    	
    	for (int i = 0; i < logs.size(); i++) {
    		HashMap<String, Object> map = new HashMap<>();
    		Log log = logs.get(i);
    		Date date = new Date();
    		date.setTime(log.getTimestamp());
    		map.put("timeStamp", date.toString());
    		map.put("eventLog", "(" + log.getType() + ") " + log.getMessage());
    		result.add(map);    		
    	}
    	
    	return result;
    }

	public static long parseDateToMillis(String date) throws ParseException {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date parsedDate = df.parse(date);
		return parsedDate.getTime();
	}
	
	public static void addLogToDB(long timestamp, Type type, String message) {
		Log log = new Log(timestamp, type, message);
		log.saveToDB();
	}
	
	public static void addMethodRequestLog(String methodName) {
		Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.INFO, "Requested method: " + methodName);
	}
	
	public static void addMethodSuccessLog(String methodName) {
		Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.SUCCESS, "Successfully responded to requested method: " + methodName);
	}
	
	public static void addMethodErrorLog(String methodName, String err) {		
		String logMessage = "An error occurred when processing the requested method " + methodName + ": " + err;  
		Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.ERROR, logMessage);
	}

	public static void addMethodErrorLog(String err) {
		Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.ERROR, err);
	}
}
