import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Server {

	public final static int port = 55335;
	public static int key = 1234;
	public final static String localIPaddr = "0.0.0.0";
	public final static String stdreply = "Hello World";
	public final static String stdresponse = "response";
	public final static String helloMessage = "Hello from Server";
	public static byte[] identifier;

	static DatagramSocket socket;
	static SocketAddress prevAddress;


	public static void main(String[] args) {

		Map<Integer, SocketAddress> routingTable = new HashMap<>();

		try {
			socket= new DatagramSocket(Server.port);
			ArrayList<String> routerAddresses = discoverRouterAddresses();
			identifier = generateRandomIdentifier();
			sendHelloToAllNetworks(routerAddresses);
			System.out.println("Listening...");
			String prevAddress;

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
						InetAddress address = packet.getAddress();
						prevAddress = address.getHostAddress();
						System.out.println("received PUB packet from : " + prevAddress);

						int identifier = getIdentifier(packet, data); // Get the next 4 bytes as an integer
						int identifierOffset = 1;
						byte[] byteIdentifier = new byte[4];
						System.arraycopy(data, identifierOffset, byteIdentifier, 0, 4);
						System.out.println((" destination identifier : " + identifier));

						// Retrieve the destination from the routing table
						SocketAddress destination = routingTable.get(identifier);
						byte[] byteArray = new byte[]{1, 2, 3, 4, 5};
						if (destination == null) {
							broadcastPacketWithIdentifier(routerAddresses, byteIdentifier, prevAddress);
							System.out.println("broadcasted");
						}
						else {
							forward(destination, byteArray);
							System.out.println("sent to address with identifier : " + identifier);
						}
						System.out.println("got address from routing table :" + destination);

						break;
					case Header.RESPONSE:
						System.out.println("Rcv response:" + packet.getSocketAddress());
						break;
					case Header.RECEIVERHELLO:
						System.out.println("entered receiverHello");
						SocketAddress receiverAddress = packet.getSocketAddress();
						int receiverIdentifier = getIdentifier(packet, data); // Get the next 4 bytes as an integer
						System.out.println(" receiver identifier : " + receiverIdentifier);
						System.out.println(" receiver address : " + receiverAddress);
						routingTable.put(receiverIdentifier, receiverAddress);
						System.out.println("put address with identifier : " + receiverIdentifier);
						break;
					case Header.HELLO:
						System.out.println("Received hello from: " + packet.getSocketAddress());
						sendHelloResponse(packet.getSocketAddress());  // Sending response
						break;
					case Header.HELLORESPONSE:
						SocketAddress senderAddress = packet.getSocketAddress();
						System.out.println("discovered address : " + senderAddress);

						int routerIdentifier = getIdentifier(packet, data); // Get the next 4 bytes as an integer

						// Debugging: Print the raw bytes of the identifier
						System.out.println("Identifier bytes: " + Arrays.toString(Arrays.copyOfRange(data, 1, 5)));

						// Use the identifier as the key in the routing table
						routingTable.put(routerIdentifier, senderAddress);
						System.out.println("put address : " + senderAddress + " with identifier key : " + routerIdentifier);

						break;
					default:
						System.out.println("Rcv: Unknown packet type");
				}

			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("--- Server finish ---");
	}

	private static void send(SocketAddress address, byte[] data, int length) throws IOException {
		byte packetType = Header.PUB;
		byte[] header = ByteBuffer.allocate(Header.HeaderSize).put(packetType).array();

		// Ensure the length does not exceed the data's length
		length = Math.min(data.length, length);

		// Create buffer for the entire packet (header + provided data)
		byte[] buffer = new byte[header.length + length];
		System.arraycopy(header, 0, buffer, 0, header.length);
		System.arraycopy(data, 0, buffer, header.length, length);

		// Create the DatagramPacket with the buffer
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

		DatagramPacket forwardPacket = new DatagramPacket(buffer, buffer.length);
		forwardPacket.setSocketAddress(address);

		// Send the packet
		socket.send(packet);
	}

	private static void forward(SocketAddress target, byte[] data) {
		try {
			byte packetType = Header.PUB;
			byte[] header = ByteBuffer.allocate(Header.HeaderSize).put(packetType).array();
			byte[] payload = data;
			byte[] buffer = new byte[header.length+payload.length];
			System.arraycopy(header, 0, buffer,0, header.length);
			System.arraycopy(payload, 0, buffer, header.length, payload.length);
			DatagramPacket response = new DatagramPacket(buffer, buffer.length);
			response.setSocketAddress(target);
			socket.send(response);
			System.out.println("forwarded packet ");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void sendHelloResponse(SocketAddress target) {
		try {
			byte packetType = Header.HELLORESPONSE;
			byte[] header = ByteBuffer.allocate(Header.HeaderSize).put(packetType).put(identifier).array();
			byte[] payload = Server.stdresponse.getBytes();
			byte[] buffer = new byte[header.length+payload.length];
			System.arraycopy(header, 0, buffer,0, header.length);
			System.arraycopy(payload, 0, buffer, header.length, payload.length);
			DatagramPacket response = new DatagramPacket(buffer, buffer.length);
			response.setSocketAddress(target);
			socket.send(response);
			System.out.println("sent response ");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void sendHelloToAllNetworks(ArrayList<String> addresses) throws IOException {
		byte packetType = Header.HELLO;
		byte[] header = ByteBuffer.allocate(Header.HeaderSize).put(packetType).put(identifier).array();
		byte[] payload = Server.helloMessage.getBytes();
		byte[] buffer = new byte[header.length + payload.length];
		System.arraycopy(header, 0, buffer, 0, header.length);
		System.arraycopy(payload, 0, buffer, header.length, payload.length);

		InetAddress localHost = InetAddress.getLocalHost();
		String localHostAddress = localHost.getHostAddress();
		System.out.println("local host address : " + addresses.get(0));
		System.out.println("local host address : " + addresses.get(1));
		int i = 0;
		for (String baseAddr : addresses) {
			String localAddress = addresses.get(i);
			i++;
			String networkPart = baseAddr.substring(0, baseAddr.lastIndexOf(".") + 1);
			for (int host = 1; host <= 254; host++) {
				String fullAddr = networkPart + host;
				if (!fullAddr.equals(localAddress)) {
					DatagramPacket helloPacket = new DatagramPacket(buffer, buffer.length,
							new InetSocketAddress(fullAddr, Server.port));
					socket.send(helloPacket);
					System.out.println("Sent hello to: " + fullAddr);
				}
			}
		}
	}

	public static ArrayList<String> discoverRouterAddresses() {
		ArrayList<String> routerAddresses = new ArrayList<>();
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				while (inetAddresses.hasMoreElements()) {
					InetAddress inetAddress = inetAddresses.nextElement();
					String[] split = inetAddress.getHostAddress().toString().split("\\.");
					String front = split[0]+"."+ split[1] + "." + split[2];
					if(!front.equals("127.0.0")){
						System.out.println("IP Address: " + inetAddress.getHostAddress());
						routerAddresses.add(inetAddress.getHostAddress());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return routerAddresses;
	}

	public static void broadcastPacketWithIdentifier(ArrayList<String> addresses, byte[] destIdentifier, String prevAddress) throws IOException {


		// Create the header with the identifier
		byte[] header = ByteBuffer.allocate(Header.HeaderSize).put(Header.PUB).put(destIdentifier).array();
		byte[] buffer = new byte[header.length];

		System.arraycopy(header, 0, buffer, 0, header.length);

		InetAddress localHost = InetAddress.getLocalHost();
		String localHostAddress = localHost.getHostAddress();
		int i = 0;
		for (String baseAddr : addresses) {
			String localAddress = addresses.get(i);
			System.out.println("local adddress : " + localAddress);
			i++;
			String networkPart = baseAddr.substring(0, baseAddr.lastIndexOf(".") + 1);
			for (int host = 1; host <= 254; host++) {
				String fullAddr = networkPart + host;
				if (!fullAddr.equals(localAddress) && !fullAddr.equals(prevAddress)) {
					DatagramPacket pubPacket = new DatagramPacket(buffer, buffer.length,
							new InetSocketAddress(fullAddr, Server.port));
					socket.send(pubPacket);
					System.out.println(" broadcasted to : " + fullAddr);
				}
			}
		}
	}

	// Generate a random 4-byte identifier
	private static byte[] generateRandomIdentifier() {
		Random random = new Random();
		byte[] identifier = new byte[4];  // 4 bytes
		random.nextBytes(identifier);
		return identifier;
	}

	// Method to get hello message with identifier
	public static String getHelloMessage() {
		byte[] identifier = generateRandomIdentifier();
		return "Hello from Server - ID: " + bytesToHex(identifier);
	}

	// Helper method to convert bytes to hex string (for readability)
	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private static int getIdentifier(DatagramPacket packet, byte[] data){
		byte[] dataNew = packet.getData();
		ByteBuffer wrapped = ByteBuffer.wrap(data); // big-endian by default
		wrapped.get(); // Skip the packet type byte
		return wrapped.getInt(); // Get the next 4 bytes as an integer
	}

}
