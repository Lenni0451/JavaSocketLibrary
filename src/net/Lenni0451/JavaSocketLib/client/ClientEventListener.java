package net.Lenni0451.JavaSocketLib.client;

import net.Lenni0451.JavaSocketLib.packets.IPacket;

public abstract class ClientEventListener {
	
	public void onPreConnect() {}
	public void onConnectionEstablished() {}
	public void onDisconnect() {}
	
	public void onRawPacketReceive(final byte[] packet) {}
	public void onPacketReceive(final IPacket packet) {}
	
}
