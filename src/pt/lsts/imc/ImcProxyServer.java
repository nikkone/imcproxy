package pt.lsts.imc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@WebSocket(maxMessageSize = 64 * 1024)
public class ImcProxyServer {

	private static HashSet<Session> activeSessions = new HashSet<>();

	@OnWebSocketConnect
	public void onConnect(Session session) {
		System.out.println("New connection from " + session.getRemoteAddress());
		synchronized (activeSessions) {
			activeSessions.add(session);	
		}		
	}

	@OnWebSocketClose
	public void onDisconnect(Session session, int statusCode, String reason) {
		System.out.println("Connection from " + session.getRemoteAddress()
				+ " has ended: " + reason);
		synchronized (activeSessions) {
			activeSessions.remove(session);
		}
	}

	@OnWebSocketError
	public void onError(Session session, Throwable error) {
		if (session != null)
			System.err.println("Error handling " + session.getRemoteAddress()
					+ ": ");
		error.printStackTrace();
	}

	@OnWebSocketMessage
	public void onBinary(Session session, byte buff[], int offset, int length) {
		if (!session.isOpen()) {
			System.err.println("Session is closed");
			return;
		}

		try {
			IMCMessage msg = deserialize(buff, offset, length);
			onMessage(session, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static IMCMessage deserialize(byte[] buff, int offset, int length) throws IOException {
		IMCInputStream iis = new IMCInputStream(new ByteArrayInputStream(
				buff, offset, length));
		IMCMessage msg = iis.readMessage();			
		iis.close();
		return msg;

	}

	public static ByteBuffer wrap(IMCMessage m) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
		IMCOutputStream ios = new IMCOutputStream(baos);
		int size = m.serialize(ios);
		ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray(), 0, size);
		return buffer;
	}

	public void onMessage(Session session, IMCMessage message) {
		System.out.println("Got " + message.getAbbrev() + " from "
				+ session.getRemoteAddress());
		try {
			ByteBuffer buff = wrap(message);
			
			HashSet<Session> sessionsCopy = new HashSet<>();
			synchronized (activeSessions) {
				sessionsCopy.addAll(activeSessions);
			}
			for (Session sess : sessionsCopy) {
				if (sess != session) {
					System.out.println("\t--> " + sess.getRemoteAddress());
					sess.getRemote().sendBytes(buff);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Server server = new Server(9090);
		server.setHandler(new WebSocketHandler() {
			@Override
			public void configure(WebSocketServletFactory arg0) {
				arg0.register(ImcProxyServer.class);
			}
		});
		try {
			server.start();
			System.out.println("Server listening on port " + 9090);
			server.join();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
