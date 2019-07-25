package me.neznamy.tab.shared.packets;

import me.neznamy.tab.bukkit.packets.EnumAPI;
import me.neznamy.tab.bukkit.packets.method.MethodAPI;
import me.neznamy.tab.shared.Shared;
import me.neznamy.tab.shared.Shared.ServerType;
import net.md_5.bungee.protocol.packet.Chat;

public class PacketPlayOutChat extends UniversalPacketPlayOut{

	private Object component;
	private ChatMessageType type;
	
	public PacketPlayOutChat(String json) {
		component = Shared.servertype == ServerType.BUKKIT ? MethodAPI.getInstance().ICBC_fromString(json) : json;
		this.type = ChatMessageType.CHAT;
	}
	public PacketPlayOutChat(String message, ChatMessageType type) {
		component = Shared.mainClass.createComponent(message);
		this.type = type;
	}
	public Object toNMS() throws Exception {
		if (versionNumber >= 12) {
			return MethodAPI.getInstance().newPacketPlayOutChat(component, type.toNMS());
		} else {
			return MethodAPI.getInstance().newPacketPlayOutChat(component, type.toByte());
		}
	}
	public Object toBungee(int clientVersion) {
		return new Chat((String) component, type.toByte());
	}
	
	public enum ChatMessageType{

		CHAT((byte)0, EnumAPI.ChatMessageType_since_1_12_R1_CHAT), 
		SYSTEM((byte)1, EnumAPI.ChatMessageType_since_1_12_R1_SYSTEM), 
		GAME_INFO((byte)2, EnumAPI.ChatMessageType_since_1_12_R1_GAME_INFO);

		private byte byteEquivalent;
		private Object nmsEquivalent;

		private ChatMessageType(byte byteEquivalent, Object nmsEquivalent) {
			this.byteEquivalent = byteEquivalent;
			this.nmsEquivalent = nmsEquivalent;
		}
		public byte toByte() {
			return byteEquivalent;
		}
		public Object toNMS() {
			return nmsEquivalent;
		}
	}
}