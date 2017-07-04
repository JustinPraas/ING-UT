package logging;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import database.DataManager;

public class Logger {
    
    @SuppressWarnings("unchecked")
	public static ArrayList<Log> getLogs(String startDate, String endDate) throws ParseException {
    	long start = parseDateToMillis(startDate);
    	long end = parseDateToMillis(endDate) + 3600000 /*add one day so the criteria 'between' can be used*/;
    	
    	ArrayList<Criterion> cr = new ArrayList<>();
		cr.add(Restrictions.between("timestamp", start, end));
		
		return (ArrayList<Log>) DataManager.getObjectsFromDB(Log.CLASSNAME, cr);		
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
    		map.put("eventLog", log.getType() + ": " + log.getMessage());
    		result.add(map);    		
    	}
    	
    	return result;
    }

	public static long parseDateToMillis(String date) throws ParseException {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date parsedDate = df.parse(date);
		return parsedDate.getTime();
	}

}
