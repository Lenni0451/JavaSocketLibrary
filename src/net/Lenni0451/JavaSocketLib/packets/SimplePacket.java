package net.Lenni0451.JavaSocketLib.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.Lenni0451.JavaSocketLib.utils.ObjectConverter;

public abstract class SimplePacket implements IPacket {

	@Override
	public void writePacketData(DataOutputStream dataOutputStream) throws IOException {
		List<Object> list = new ArrayList<>();
		this.writePacketData(list);
		byte[] rawData = ObjectConverter.objectToByteArray(list);
		dataOutputStream.writeInt(rawData.length);
		dataOutputStream.write(rawData);
	}

	@Override
	public void readPacketData(DataInputStream dataInputStream) throws IOException {
		byte[] rawData = new byte[dataInputStream.readInt()];
		dataInputStream.read(rawData);
		List<Object> list = ObjectConverter.byteArrayToObject(rawData);
		this.readPacketData(list);
	}
	

	public abstract void writePacketData(final List<Object> list);
	public abstract void readPacketData(final List<Object> list);
	
}
