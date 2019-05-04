package net.Lenni0451.JavaSocketLib.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface IPacket {

	public void writePacketData(final DataOutputStream dataOutputStream) throws IOException;
	public void readPacketData(final DataInputStream dataInputStream) throws IOException;
	
}
