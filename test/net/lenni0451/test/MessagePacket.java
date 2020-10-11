package net.lenni0451.test;

import net.Lenni0451.JavaSocketLib.packets.IPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MessagePacket implements IPacket {

    public String message;

    public MessagePacket() {
    }

    public MessagePacket(String message) {
        this.message = message;
    }

    @Override
    public void writePacketData(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeUTF(this.message);
    }

    @Override
    public void readPacketData(DataInputStream dataInputStream) throws IOException {
        this.message = dataInputStream.readUTF();
    }

}
