/*
 *
 * Hands-On code of the book Introduction to Reliable Distributed Programming
 * by Christian Cachin, Rachid Guerraoui and Luis Rodrigues
 * Copyright (C) 2005-2011 Luis Rodrigues
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 *
 * Contact
 * 	Address:
 *		Rua Alves Redol 9, Office 605
 *		1000-029 Lisboa
 *		PORTUGAL
 * 	Email:
 * 		ler@ist.utl.pt
 * 	Web:
 *		http://homepages.gsd.inesc-id.pt/~ler/
 * 
 */

package org.vanilladb.comm.protocols.serverClientAppl;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.messages.ChannelType;
import org.vanilladb.comm.messages.NodeFailListener;
import org.vanilladb.comm.messages.P2pMessage;
import org.vanilladb.comm.messages.P2pMessageListener;
import org.vanilladb.comm.messages.TotalOrderMessage;
import org.vanilladb.comm.messages.TotalOrderedMessageListener;
import org.vanilladb.comm.protocols.events.*;
import org.vanilladb.comm.protocols.utils.ProcessSet;

import net.sf.appia.core.*;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.RegisterSocketEvent;

/**
 * Session implementing the sample application.
 * 
 * @author nuno
 */
public class ServerClientApplSession extends Session {

	private Channel channel;

	private P2pMessageListener p2pMessageListener;
	private NodeFailListener nflistener;

	private ProcessSet processes;

	private boolean registerSocket;

	private TotalOrderedMessageListener tomListener;

	public ServerClientApplSession(Layer layer) {
		super(layer);
	}

	public void init(ProcessSet processes,
			P2pMessageListener p2pMessageListener, NodeFailListener nflistener,
			boolean registerSocket) {
		this.processes = processes;
		this.p2pMessageListener = p2pMessageListener;
		this.registerSocket = registerSocket;
		this.nflistener = nflistener;
	}

	public void init(ProcessSet processes,
			TotalOrderedMessageListener tomListener,
			P2pMessageListener p2pMessageListener, NodeFailListener nflistener,
			boolean registerSocket) {
		this.processes = processes;
		this.p2pMessageListener = p2pMessageListener;
		this.registerSocket = registerSocket;
		this.nflistener = nflistener;
		this.tomListener = tomListener;
	}

	public void handle(Event event) {
		if (event instanceof BroadcastEvent)
			handleBroadcastEvent((BroadcastEvent) event);
		else if (event instanceof SendableEvent)
			handleSendableEvent((SendableEvent) event);
		else if (event instanceof Crash)
			handleCrashEvent((Crash) event);
		else if (event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);
		else if (event instanceof ChannelClose)
			handleChannelClose((ChannelClose) event);
		else if (event instanceof RegisterSocketEvent)
			handleRegisterSocket((RegisterSocketEvent) event);
	}

	private void handleBroadcastEvent(BroadcastEvent event) {
		if (event.getDir() == Direction.UP) {
			tomListener.onRecvTotalOrderedMessage((TotalOrderMessage) event
					.getMessage().popObject());
		} else {
			try {
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
	}

	private void handleSendableEvent(SendableEvent event) {
		if (event.getDir() == Direction.UP) {
			Object o = event.getMessage().popObject();
			if (o instanceof P2pMessage) {
				p2pMessageListener.onRecvP2pMessage((P2pMessage) o);
			}
		} else {
			try {
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
	}

	private void handleCrashEvent(Crash event) {
		int crashedProcess = event.getCrashedProcess();
		if (crashedProcess == processes.getSize() - 1) {
			nflistener.onNodeFail(event.getCrashedProcess(),
					ChannelType.CLIENT, event.getChannel());
		} else {
			nflistener.onNodeFail(crashedProcess, ChannelType.SERVER,
					event.getChannel());
		}
	}

	/**
	 * @param event
	 */
	private void handleRegisterSocket(RegisterSocketEvent event) {
		if (event.error) {
			if (Logger.getLogger(ServerClientApplSession.class.getName())
					.isLoggable(Level.SEVERE)) {
				Logger.getLogger(ServerClientApplSession.class.getName())
						.severe("Address already in use!");
			}
			System.exit(2);
		}
	}

	/**
	 * @param init
	 */
	private void handleChannelInit(ChannelInit init) {
		try {
			init.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		channel = init.getChannel();

		try {
			// sends this event to open a socket in the layer that is used has
			// perfect
			// point to point
			// channels or unreliable point to point channels.

			if (registerSocket) {
				RegisterSocketEvent rse = new RegisterSocketEvent(channel,
						Direction.DOWN, this);
				rse.port = ((InetSocketAddress) processes.getSelfProcess()
						.getSocketAddress()).getPort();
				rse.localHost = ((InetSocketAddress) processes.getSelfProcess()
						.getSocketAddress()).getAddress();
				rse.go();
			}

			ProcessInitEvent processInit = new ProcessInitEvent(channel,
					Direction.DOWN, this);
			processInit.setProcessSet(processes);
			processInit.go();
		} catch (AppiaEventException e1) {
			e1.printStackTrace();
		}
		if (Logger.getLogger(ServerClientApplSession.class.getName())
				.isLoggable(Level.INFO)) {
			Logger.getLogger(ServerClientApplSession.class.getName()).info(
					"Channel is open.");
		}
	}

	/**
	 * @param close
	 */
	private void handleChannelClose(ChannelClose close) {
		channel = null;
		if (Logger.getLogger(ServerClientApplSession.class.getName())
				.isLoggable(Level.INFO)) {
			Logger.getLogger(ServerClientApplSession.class.getName()).info(
					"Channel is closed.");
		}
	}
}
