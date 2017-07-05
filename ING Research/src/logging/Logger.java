package logging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import database.SQLiteDB;
import logging.Log.Type;
import server.rest.ServerModel;

public class Logger {
    
	public static ArrayList<Log> getLogs(String startDate, String endDate) throws ParseException {
    	long start = parseDateToMillis(startDate);
    	long end = parseDateToMillis(endDate) + 1000 * 3600 * 24 /*add one day so the criteria 'between' can be used*/;

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
				log.setType(rs.getString("type"));
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
    		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    		map.put("timeStamp", df.format(date));
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
	
	public static void addMethodRequestLog(String methodName, Map<String, Object> params) {
		ArrayList<Map.Entry<String, Object>> parameters = new ArrayList<>();
		
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			parameters.add(entry);
		}
		
		Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.INFO, "Requested method: " + methodName + " with parameters: " + parameters);
	}
	
	public static void addMethodSuccessLog(String methodName) {
		Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.SUCCESS, "Successfully responded to requested method: " + methodName);
	}
	
	public static void addMethodErrorLog(String methodName, String err, int code) {		
		String logMessage = "(Code " + code + ") An error occurred when processing the requested method " + methodName + ": " + err;  
		Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.ERROR, logMessage);
	}

	public static void addMethodErrorLog(String err, int code) {
		String logMessage = "(Code " + code + ") " + err;
		Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.ERROR, logMessage);
	}
}
