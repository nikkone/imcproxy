package pt.lsts.imc;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

@WebSocket
public class ImcClientSocket {

	protected Session remote;
	protected WebSocketClient client = new WebSocketClient();
	
	@OnWebSocketConnect
	public void onConnect(Session remote) {
		this.remote = remote;
	}
	
	@OnWebSocketMessage
	public void onBinary(Session session, byte buff[], int offset, int length) {
		try {
			IMCMessage msg = ImcProxyServer.deserialize(buff, offset, length);
			onMessage(msg);
		}
		catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void onMessage(IMCMessage msg) {
		msg.dump(System.out);
	}
	
	public void sendMessage(IMCMessage msg) throws IOException {
		if (remote == null || !remote.isOpen())
			throw new IOException("Error sending message: not connected");
		
		remote.getRemote().sendBytes(ImcProxyServer.wrap(msg));
	}
	
	public Future<Session> connect(URI server) throws Exception {
		client.start();
		return client.connect(this, server, new ClientUpgradeRequest());
	}
	
	public void close() throws Exception {
		client.stop();
	}
	
	public static void main(String[] args) throws Exception {
		ImcClientSocket socket = new ImcClientSocket();		
		Future<Session> future = socket.connect(new URI("ws://localhost:9090"));
		System.out.printf("Connecting...");
		future.get();
		socket.sendMessage(new Temperature(10.67f));
		socket.close();
	}
}
