package server;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ControlServlet extends HttpServlet {

	private static final long serialVersionUID = -4145614394164254585L;
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		String jReq;
		try {
			jReq = (String) request.getReader().readLine();
			String jResp = RequestHandler.parseJSONRequest(jReq);
			response.getWriter().println(jResp);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
