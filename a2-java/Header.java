import java.nio.ByteBuffer;

public class Header {
    public static byte HeaderSize = 6;

    public static final byte PUB = 0x01;
    public static final byte SUB = 0x02;
    public static final byte RESET = 0x0f;
    public static final byte HELLO = 0x60;
    public static final byte HELLORESPONSE = 0x70;
    public static final byte RECEIVERHELLO = 0x30;

    public static final byte VIDEO = 0x10;

    // This method parses the destination from a received header.
    public static int getDestination(byte[] header) {
        ByteBuffer buffer = ByteBuffer.wrap(header);
        buffer.get(); // Skip the type byte.
        return buffer.getInt(); // Read the destination address.
    }

    // This method creates a header given the type and destination.
    public static byte[] createHeader(byte type, int destination) {
        ByteBuffer buffer = ByteBuffer.allocate(HeaderSize);
        buffer.put(type);
        buffer.putInt(destination); // Destination address is 4 bytes.
        return buffer.array();
    }

    public static final byte RESPONSE = 0x03; // Indicate this is a response message
    // Additional fields for routing
    public static final byte BROADCAST = 0x20; // Indicate this is a broadcast message
}






