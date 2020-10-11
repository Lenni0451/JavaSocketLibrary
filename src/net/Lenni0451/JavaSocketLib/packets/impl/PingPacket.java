package net.Lenni0451.JavaSocketLib.packets.impl;

import net.Lenni0451.JavaSocketLib.packets.IPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PingPacket implements IPacket {

    private long systemTime;

    public PingPacket() {
    }

    public PingPacket(final long systemTime) {
        this.systemTime = systemTime;
    }

    @Override
    public void writePacketData(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeLong(this.systemTime);
    }

    @Override
    public void readPacketData(DataInputStream dataInputStream) throws IOException {
        this.systemTime = dataInputStream.readLong();
    }

    public long getSystemTime() {
        return this.systemTime;
    }

}
