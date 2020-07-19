<%@ page import="com.thedeveloperfriend.websocketserver.memoryinfo.*"%>

<!-- 
This JSP page will ask a user to enter the unique client name. 
Upon receiving the client’s name it will initiate the process to send a request to the same client for memory information. 
It will wait for the response to be available then display the memory information to the user. 
 -->

<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>MEMORY INFO RETRIEVAL WEBSOCKET APPLICATION</title>
<link
	href="https://fonts.googleapis.com/css2?family=Roboto:wght@100;400&display=swap"
	rel="stylesheet">
<style>
body {
	font-family: 'Roboto', sans-serif;
}
</style>
</head>
<body>

	<H1>MEMORY INFO RETRIEVAL WEBSOCKET APPLICATION</H1>

	<form action="index.jsp">
		<label for="client_name">Enter Client Name:</label><br> <input
			type="text" id="client_name" name="client_name"><br> <input
			type="submit" value="Submit">
	</form>

	<%!MemoryInfoWebSocketEndPoint memoryInfoWebSocketEndPoint = new MemoryInfoWebSocketEndPoint();%>


	<%
		String clientName = request.getParameter("client_name");
	
		if (clientName != null && !clientName.equals("")) {
			//The client’s name is appended with the system time( in nanoseconds) to differentiate the requests made to the same client in short intervals.
			String clientRequestIdentifier = clientName + ":" + System.nanoTime();
			//Place a request for memory information of a particular client by putting an entry into RequestResponseMap
			memoryInfoWebSocketEndPoint.getRequestResponseMap().put(clientRequestIdentifier, "EMPTY");
			long millisecondCount = 0;
			//Keep polling the RequestResponseMap for 10 seconds to response
			while (millisecondCount < 10000) {
				Thread.sleep(100);
				String responseString = memoryInfoWebSocketEndPoint.getRequestResponseMap()
						.get(clientRequestIdentifier);
				System.out.println("clientRequestIdentifier = " + clientRequestIdentifier + " : responseString = "
						+ responseString);

				if (responseString != null && ((!responseString.equalsIgnoreCase("EMPTY"))
						&& (!responseString.equalsIgnoreCase("SENT"))
						&& (!responseString.equalsIgnoreCase("COMPLETED")))) {

					if (responseString.equalsIgnoreCase("CLIENT_NOT_CONNECTED")) {
						memoryInfoWebSocketEndPoint.getRequestResponseMap().put(clientRequestIdentifier,
								"COMPLETED");
						out.println("<p>CLIENT IS NOT CONNECTED !!!</p>");
						break;
					} else {
						String[] responseArray = responseString.split("\\|");
						String totoalMemory = responseArray[1];
						String availableMemory = responseArray[2];
	%>
	<br>
	<table border="1">
		<tr>
			<th>CLIENT ID</th>
			<th>TOTAL JVM MEMORY</th>
			<th>AVAILABLE JVM MEMORY</th>
		</tr>
		<tr>
			<td><%=clientName%></td>
			<td><%=totoalMemory%> MB</td>
			<td><%=availableMemory%> MB</td>
		</tr>
	</table>

	<%
		break;
					}

				}

				millisecondCount += 100;
			}
			
			if (!(millisecondCount < 10000)) {
				out.println("<p>RESPONSE TIMEOUT !</p>");
			}
		}
	%>


</body>
</html>