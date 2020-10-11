package net.Lenni0451.JavaSocketLib.server;

import net.Lenni0451.JavaSocketLib.packets.IPacket;

public interface ServerEventListener {

    default void onSocketPreConnect(final ClientConnection client) {
    }

    default void onSocketConnectionEstablished(final ClientConnection client) {
    }

    default void onSocketDisconnect(final ClientConnection client) {
    }

    default void onRawPacketReceive(final ClientConnection client, final byte[] packet) {
    }

    default void onPacketReceive(final ClientConnection client, final IPacket packet) {
    }

    default void onServerClose() {
    }

}
