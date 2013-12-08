/*
 *  This file is part of hijack-infinity.
 *
 *  hijack-infinity is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  hijack-infinity is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with hijack-infinity.  If not, see <http://www.gnu.org/licenses/>.
 */

package umich.hijack.core;

import java.util.ArrayList;

public class PacketDispatch implements PktRecvCb, PktSentCb {

	// Callbacks that want different packet types
	private final ArrayList<ArrayList<PktRecvCb>> _recvListeners;
	private final static int MAX_PACKET_TYPES = 16;

	private PktTransmitter _pktTx;



	public PacketDispatch() {
		// Create the data structure for callbacks
		_recvListeners = new ArrayList<ArrayList<PktRecvCb>>(MAX_PACKET_TYPES);
		for (int i=0; i<MAX_PACKET_TYPES; i++) {
			_recvListeners.add(new ArrayList<PktRecvCb>());
		}
	}

	///////////////////////////////
	// Register Callback Functions
	///////////////////////////////

	// Register with the dispatcher what should actually trasmit packets.
	public void registerPacketTransmitter (PktTransmitter ptx) {
		_pktTx = ptx;
	}

	// Register a callback handler for a particular packet type. Each packet
	// type can have multiple handlers in case multiple services want to know
	// about a given packet type.
	public void registerIncomingPacketListener(PktRecvCb listener, int packetTypeID) {
		if (packetTypeID < 0 ||  packetTypeID > MAX_PACKET_TYPES) {
			// throw an exception?
			System.out.println("Registering incoming listener: Bad packet type ID.");
			return;
		}
		_recvListeners.get(packetTypeID).add(listener);
	}

	public void sendPacket (Packet p) {
		_pktTx.sendPacket(p);
	}


	// The insertion point for packets into the dispatch layer. After being
	// decoded and detected as valid packets, received packets enter the
	// dispatch layer here.
	@Override
	public void recvPacket (Packet p) {

		if (p.ackRequested) {
			Packet ack = new Packet();
			ack.length = 1;
			ack.ackRequested = false;
			ack.powerDown = false;
			ack.sentCount = 0;
			ack.data[0] = 0xBB;
			_pktTx.sendPacket(ack);
		}

		// Pass packet to all waiting listeners
		for (PktRecvCb l : _recvListeners.get(p.typeId)) {
			l.recvPacket(p);
		}
	}

	@Override
	public void sentPacket () {
		//packet was sent
	}

}
