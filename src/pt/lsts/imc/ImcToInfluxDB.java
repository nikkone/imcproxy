package pt.lsts.imc;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import pt.lsts.imc.IMCMessage;

@WebSocket
public class ImcToInfluxDB extends ImcClientSocket {
	protected static SimpleDateFormat format = new SimpleDateFormat("[YYYY-MM-dd, HH:mm:ss] ");
	protected static String influxhost = "http://localhost";
	protected static int influxport = 8086;
	protected static String dbname = "imc";

    public void httpPostImcToInfluxDB(IMCMessage message) {
		String messageLineProtocol = ImcToInfluxDB.imcToInfluxLineProtocol(message);
		if(messageLineProtocol != "") {
	    	try {
				System.out.println("Writing to DB: " + message.getAbbrev());
				// Connect
				URL url = new URL(influxhost + ":" + influxport + "/write?db=" + dbname + "&precision=ms");
				URLConnection con = url.openConnection();
				HttpURLConnection http = (HttpURLConnection)con;
				http.setRequestMethod("POST"); // PUT is another valid option
				http.setDoOutput(true);
				// make
				byte[] out = messageLineProtocol.getBytes(StandardCharsets.UTF_8);
				int length = out.length;
				// send
				http.setFixedLengthStreamingMode(length);
				http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
				http.connect();
				try(OutputStream os = http.getOutputStream()) {
				    os.write(out);
				} catch (Exception e) {
					System.out.println("err while writing db first");
					return;
				}
			} catch (Exception e) {
				System.out.println("err while writing db");
				return;
			}
		}/* else {
			System.out.println("Empty");
		}*/
    }
    
    public static String imcToInfluxLineProtocol(IMCMessage message) {
    	String out;
    	switch(message.getAbbrev()) {
    		// Add case case for messages to be stored
    		case "Rpm":
    		case "Voltage":
    		case "Current":
    		case "Temperature":
    		case "FuelLevel":
    		case "SetThrusterActuation":
    		case "GpsFix":
    		case "TBRFishTag":
    		case "TBRSensor":
			case "EulerAngles":
			case "DesiredSpeed":
			case "DesiredHeading":
			case "CpuUsage":
			case "AngularVelocity":
			case "RemoteSensorInfo":
    		case "EstimatedState":        	
    			
				//Add src and entity as tags (indexed)
        		out = message.getAbbrev() + ",src="+ message.getSrc() + ",ent=" + message.getSrcEnt();
				Map<String, Object> mp = message.getValues();

				// Special tags
				if(mp.containsKey("id")) {
					System.out.println("Contains id");
					out += ",id=" + mp.get("id");
				}
				//End tag section
        		out += " ";

				// Add all other variables of the message as fields(non-indexed)
				Boolean first = true;
				
        		for (Map.Entry<String, Object> entry : mp.entrySet()) {
        			if(first) {
        				first = false;
        			} else {
        				out += ",";
        			}
        			out += entry.getKey() + "=" + entry.getValue();
					// Radians to Degrees  for lat and lon
        			if(entry.getKey().equals("lat") || entry.getKey().equals("lon")) {
        				System.out.println("Lat or Lon added");
        				out += "," + entry.getKey() + "deg=" + (double)(entry.getValue())*(180/Math.PI);
        				
        			}
        		}

				// Add timestamp to message
        		out += "  " + message.getTimestampMillis();
        		System.out.println("InfluxDB message: " + message.getAbbrev());
        		break;

        	// Return empty for all messages not explicitly mentioned
    		default: 
    			out = "";
    	}
    	return out;
    }
    
    
	@Override
	public void onMessage(IMCMessage message) {
		httpPostImcToInfluxDB(message);
	}
	
	public ImcToInfluxDB(String serverHost, int serverPort) throws Exception {
		connect(new URI("ws://"+serverHost+":"+serverPort));
	}
	
	public static void console(String text) {
		System.out.println(format.format(new Date())+text);
	}
	public static void main(String[] args) throws Exception {
		String host = "otter.itk.ntnu.no";
		int port = 9090;
		
		if (args.length == 5) {
			try {
				port = Integer.parseInt(args[1]);
				host = args[0];
				influxport = Integer.parseInt(args[3]);
				influxhost = args[2];
				influxhost = args[4];
			}
			catch (Exception e) {
				System.out.println("Usage: ./imcplot <host> <port> or ");
				System.out.println("Usage: ./imcplot <host> <port> <influxhost> <influxport> <dbname>");
				return;
			}
		} else if (args.length == 2) {
			try {
				port = Integer.parseInt(args[1]);
				host = args[0];
			}
			catch (Exception e) {
				System.out.println("Usage: ./imcplot <host> <port> or ");
				System.out.println("Usage: ./imcplot <host> <port> <influxhost> <influxport> <dbname>");
				return;
			}
		} 
		ImcToInfluxDB.console("Connecting to IMCProxy server at "+host+":"+port);
		ImcToInfluxDB.console("Using InfluxDB server at "+influxhost+":"+influxport + ",db:" + dbname);
		new ImcToInfluxDB(host, port);		
	}		
}
