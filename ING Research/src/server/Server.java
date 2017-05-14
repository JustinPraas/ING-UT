package server;

import java.io.IOException;
import java.net.*;

import com.sun.net.httpserver.HttpServer;

public class Server {
	
	private static HttpServer server;
	private static int port = 1337;
	private static Socket client;
	public static String address;
	public static final String HTTPPOST = "POST / HTTP/1.1\nHost: 127.0.0.1";
	private static void listen() {
		try {
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", 80), 20);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Server started listening on port " + port);
		server.start();
	}
	
	public static void main(String[] args) {
		listen();
	}
}
