package com.thedeveloperfriend.websocketserver.memoryinfo;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import javax.websocket.*;
import javax.websocket.server.*;

/**
 * Annotated WebSocket Server EndPoint to handle all the communication with
 * WebSocket clients. All the life cycle annotated methods are implemented here
 * such as @OnMessage, @OnOpen etc..
 */
@ServerEndpoint("/MemoryInfoWebSocketEndPoint/{unique_client_name}")
public class MemoryInfoWebSocketEndPoint {

	// HashMap to hold all the session objects for the clients established
	// connection with the server. If a client got disconnected then the
	// corresponding entry will be removed from this HashMap.
	private static ConcurrentHashMap<Integer, Object[]> sessionHashMap = new ConcurrentHashMap<Integer, Object[]>();

	// HashMap to maintain the request messages(Key) from the server and their
	// corresponding response messages(Value) from the client.
	private static ConcurrentHashMap<String, String> requestResponseMap = new ConcurrentHashMap<String, String>();

	public ConcurrentHashMap<String, String> getRequestResponseMap() {
		return requestResponseMap;
	}

	// This thread will be used to send request messages to clients and receive a
	// response from the same. This thread will keep updating the requestResponseMap
	// with request and response messages.
	private static Thread validationRequestThread;

	// Executing the thread in the static block so that it will start as soon as the
	// server endpoint is initialized
	static {
		validationRequestThread = new Thread() {
			public void run() {
				while (true) {

					for (String reqString : requestResponseMap.keySet()) {

						String value = requestResponseMap.get(reqString);

						// if the request is already processed then do not proceed to send the request
						if (value.equalsIgnoreCase("SENT") || value.equalsIgnoreCase("COMPLETED")
								|| value.equalsIgnoreCase("CLIENT_NOT_CONNECTED")
								|| value.contains("RESPONSE MESSAGE")) {
							continue;
						}

						try {
							// Send the request message to WebSocket client
							boolean statusFlag = sendRequestToClient(reqString);
							// If client is not reachable the update the response as CLIENT_NOT_CONNECTED
							if (!statusFlag) {
								requestResponseMap.put(reqString, "CLIENT_NOT_CONNECTED");
							}

							System.out.println("VALIDATION REQUEST THREAD : REQUEST =  " + reqString + " : STATUS : "
									+ statusFlag);

						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					// Remove the entries from sessionHashMap if the session or connection with the
					// client is closed
					removeClosedConnections();
				}
			};
		};
		validationRequestThread.start();

	}

	/**
	 * This method will be automatically called whenever the server receives a
	 * message from client which is already connected
	 */
	@OnMessage
	public void onMessage(Session session, String msg) {
		try {
			System.out.println("Inside onMessage: msg = " + msg);
			// Client will be sending the message with both request and response separated
			// with ^ symbol
			String[] reqResArray = msg.split("\\^");
			String requestString = reqResArray[0];
			String responseString = reqResArray[1];
			requestResponseMap.put(requestString, responseString);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method will be automatically called whenever a client establishes a new
	 * connection with server.
	 * 
	 * @param uniqueClientName - String that will identify the client
	 * @param session          - Session object represents the connection with
	 *                         client
	 */
	@OnOpen
	public void open(@PathParam("unique_client_name") String uniqueClientName, Session session) {

		Object[] sessionArr = { session, uniqueClientName };
		sessionHashMap.put(Integer.valueOf(session.getId()), sessionArr);
		System.out
				.println("NEW SESSION OPENED WITH ID : " + session.getId() + " : FOR CLIENT ID = " + uniqueClientName);

	}

	/**
	 * This method will be automatically called if any issue occurs with the
	 * communication with the client
	 */
	@OnError
	public void error(Session session, Throwable t) {
		sessionHashMap.remove(Integer.valueOf(session.getId()));
		System.out.println("ERROR METHOD CALLED ON SESSION ID =  " + session.getId());
	}

	/**
	 * This method will be automatically called if client explicitly closes the
	 * connection
	 */
	@OnClose
	public void closedConnection(Session session) throws IOException {
		if (session != null && !isNumeric(session.getId())) {
			System.out.println("SESSION CLOSED : " + session.getId());
			session.close();
		} else {
			// Whenever client connection is closed we should remove the corresponding entry
			// from sessionHashMap
			sessionHashMap.remove(Integer.valueOf(session.getId()));
			System.out.println("SESSION CLOSED : " + session.getId());
			session.close();
		}

	}

	/**
	 * Send request message to the WebSocket client
	 */
	private static boolean sendRequestToClient(String requestString) throws IOException {

		boolean statusFlag = false;
		String uniqueClientName = requestString.split(":")[0];
		Session session = getSessionByUniqueClientName(uniqueClientName);
		// If client is disconnected this method will return false
		if (session == null || !session.isOpen()) {
			return statusFlag;
		}
		session.getBasicRemote().sendText(requestString);

		statusFlag = true;
		// Once the request is successfully send then update the response text as SENT
		requestResponseMap.put(requestString, "SENT");

		return statusFlag;

	}

	/**
	 * Search and return the session object by unique name assigned to each client
	 * if client is disconnected the this will return null
	 */
	private static Session getSessionByUniqueClientName(String uniqueClientName) {

		for (Integer sessionId : sessionHashMap.keySet()) {
			Object[] sessionArr = sessionHashMap.get(sessionId);
			Session session = (Session) sessionArr[0];
			String sessionUniqueClientName = (String) sessionArr[1];
			if (uniqueClientName.equalsIgnoreCase(sessionUniqueClientName)) {
				return session;
			}
		}

		return null;

	}

	/**
	 * Remove the entries from sessionHashMap if the session or connection with the
	 * client is closed
	 */
	private static void removeClosedConnections() {
		ArrayList<Integer> closedSessions = new ArrayList<Integer>();
		for (Integer sessionId : sessionHashMap.keySet()) {
			Object[] sessionArr = sessionHashMap.get(sessionId);
			if (sessionArr == null || sessionArr[0] == null) {
				closedSessions.add(sessionId);
				continue;

			}
			Session session = (Session) sessionArr[0];
			if (!session.isOpen()) {
				closedSessions.add(sessionId);
			}

		}
		sessionHashMap.keySet().removeAll(closedSessions);
	}

	public static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
