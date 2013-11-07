package pt.lsts.imc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
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
	private static SimpleDateFormat format = new SimpleDateFormat("[YYYY-MM-dd, HH:mm:ss] ");
	
	@OnWebSocketConnect
	public void onConnect(Session session) {
		console("New connection from " + session.getRemoteAddress());
		synchronized (activeSessions) {
			activeSessions.add(session);	
		}		
	}

	@OnWebSocketClose
	public void onDisconnect(Session session, int statusCode, String reason) {
		console("Connection from " + session.getRemoteAddress()
				+ " has ended: " + reason);
		synchronized (activeSessions) {
			activeSessions.remove(session);
		}
	}

	@OnWebSocketError
	public void onError(Session session, Throwable error) {
		if (session != null)
			console("ERROR: handling " + session.getRemoteAddress()
					+ ": ");
		error.printStackTrace();
	}

	@OnWebSocketMessage
	public void onBinary(Session session, byte buff[], int offset, int length) {
		if (!session.isOpen()) {
			console("ERROR: Session is closed");
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
		console("Got " + message.getAbbrev() + " from "
				+ session.getRemoteAddress());
		try {
			ByteBuffer buff = wrap(message);
			
			HashSet<Session> sessionsCopy = new HashSet<>();
			synchronized (activeSessions) {
				sessionsCopy.addAll(activeSessions);
			}
			for (Session sess : sessionsCopy) {
				if (sess != session) {
					console("\t--> " + sess.getRemoteAddress());
					sess.getRemote().sendBytes(buff);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void console(String text) {
		System.out.println(format.format(new Date())+text);
	}

	public static void main(String[] args) {
		Server server;
		int port = 9090;
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		server = new Server(port);
		server.setHandler(new WebSocketHandler() {
			@Override
			public void configure(WebSocketServletFactory arg0) {
				arg0.register(ImcProxyServer.class);
			}
		});
		try {
			server.start();
			System.out.println(ImcProxyServer.format.format(new Date())+"Server listening on port " + port);
			server.join();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
