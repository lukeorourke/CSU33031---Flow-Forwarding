import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

public class TermServer {
	
	public final static int port = 55335;
	public final static String localIPaddr = "0.0.0.0";
	public final static String stdreply = "Hi there";

	public static void main(String[] args) {		
		DatagramSocket socket;
		Terminal terminal= new Terminal(args[0]);	
		
		try {
			socket= new DatagramSocket(TermServer.port);
			terminal.println("Listening...");

			while(true) {
				// Receive packet
				byte[] buffer = new byte[65535];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);

				// Interpret packet type and reader header
				byte[] data = packet.getData();
				ByteBuffer header = ByteBuffer.wrap(data,0, Header.HeaderSize);
				byte packetType= header.get();
				switch(packetType) {
					case Header.PUB:
						byte contenttype= header.get();
						int streamID = header.getInt();
						terminal.println("Rcv:"+packet.getSocketAddress()+" == "+contenttype+":"+String.format("0x%08X",streamID));
						break;
					default:
						terminal.println("Rcv: Unknown packet type");
				}

				// Send reply
				buffer= TermServer.stdreply.getBytes();
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				reply.setSocketAddress(packet.getSocketAddress());
				socket.send(reply);
				terminal.println("Snd:"+reply.getAddress());
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("--- Server finish ---");
	}
}
