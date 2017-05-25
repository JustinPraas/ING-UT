package testing;

import java.util.ArrayList;
import java.util.HashMap;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class Test {
	String info1;
	String info2;
	String info3;
	
	public Test(String info1, String info2, String info3) {
		this.info1 = info1; this.info2 = info2; this.info3 = info3;
	}
	
	public static void main(String[] args) {
		ArrayList<HashMap> data = new ArrayList<>();
		HashMap<String, String> stuff = new HashMap<>();
		stuff.put("info1", "stuff1");
		stuff.put("info2", "things1");
		stuff.put("info3", "bombs1");
		data.add(stuff);
		
		stuff = new HashMap<>();
		stuff.put("info1", "stuff2");
		stuff.put("info2", "things2");
		stuff.put("info3", "bombs2");
		data.add(stuff);
		
		stuff = new HashMap<>();
		stuff.put("info1", "stuff3");
		stuff.put("info2", "things3");
		stuff.put("info3", "bombs3");
		data.add(stuff);
		
		JSONRPC2Response jResp = new JSONRPC2Response(data, "testresponse");
		System.out.println(jResp.toJSONString());
	}
}
