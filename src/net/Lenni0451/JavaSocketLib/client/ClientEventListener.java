package net.Lenni0451.JavaSocketLib.client;

import net.Lenni0451.JavaSocketLib.packets.IPacket;

public interface ClientEventListener {

    default void onPreConnect() {
    }

    default void onConnectionEstablished() {
    }

    default void onDisconnect() {
    }

    default void onRawPacketReceive(final byte[] packet) {
    }

    default void onPacketReceive(final IPacket packet) {
    }

}
