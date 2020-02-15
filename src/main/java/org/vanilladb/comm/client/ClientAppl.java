/*******************************************************************************
 * Copyright 2016 vanilladb.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.vanilladb.comm.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.appia.core.Appia;
import net.sf.appia.core.AppiaCursorException;
import net.sf.appia.core.AppiaDuplicatedSessionsException;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaInvalidQoSException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.ChannelCursor;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Layer;
import net.sf.appia.core.QoS;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.protocols.tcpcomplete.TcpCompleteLayer;

import org.vanilladb.comm.messages.ChannelType;
import org.vanilladb.comm.messages.NodeFailListener;
import org.vanilladb.comm.messages.P2pMessage;
import org.vanilladb.comm.messages.P2pMessageListener;
import org.vanilladb.comm.protocols.serverClientAppl.ServerClientApplLayer;
import org.vanilladb.comm.protocols.serverClientAppl.ServerClientApplSession;
import org.vanilladb.comm.protocols.tcpBasedPFD.PFDStartEvent;
import org.vanilladb.comm.protocols.tcpBasedPFD.TcpBasedPFDLayer;
import org.vanilladb.comm.protocols.utils.ProcessSet;
import org.vanilladb.comm.protocols.utils.SampleProcess;
import org.vanilladb.comm.server.ServerAppl;

public class ClientAppl extends Thread implements P2pMessageListener,
		NodeFailListener {

	private final boolean IS_STANDALONE_SEQUENCER;

	private Channel clientChannel;
	private ProcessSet dbServerProcessSet;
	private ProcessSet clientProcessSet;
	private ProcessSet clientParticipatedProcessSet;

	private int selfId;
	private int leaderId = 0;

	private ClientP2pMessageListener cP2pMListener;
	private ClientNodeFailListener nfListener;

	public ClientAppl(int selfId, ClientP2pMessageListener cP2pMListener,
			ClientNodeFailListener nfListener) {

		if (cP2pMListener == null || nfListener == null) {
			throw new IllegalArgumentException(
					"Must implement TotalOrderedMessageListener and P2pMessageListener");
		}

		this.selfId = selfId;
		this.cP2pMListener = cP2pMListener;
		this.nfListener = nfListener;

		// read config file
		String path = System.getProperty("org.vanilladb.comm.config.file");
		String prop;
		if (path != null) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(path);
				System.getProperties().load(fis);
			} catch (IOException e) {
				// do nothing
			} finally {
				try {
					if (fis != null)
						fis.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
		String serverView = System.getProperty(ServerAppl.class.getName()
				+ ".SERVER_VIEW");
		String clientView = System.getProperty(ClientAppl.class.getName()
				+ ".CLIENT_VIEW");
		dbServerProcessSet = buildProcessSet(serverView, selfId);
		clientProcessSet = buildProcessSet(clientView, selfId);

		prop = System.getProperty(ServerAppl.class.getName()
				+ ".STAND_ALONE_SEQUENCER");
		IS_STANDALONE_SEQUENCER = (prop != null ? Boolean.parseBoolean(prop)
				: false);
		
		// standalone leader
		if (IS_STANDALONE_SEQUENCER) {
			leaderId = dbServerProcessSet.getSize() - 1;
		} else {
			leaderId = 0;
		}
	}

	@Override
	public void run() {
		ProcessSet ps = new ProcessSet();
		for (int i = 0; i < dbServerProcessSet.getSize(); ++i) {
			ps.addProcess(new SampleProcess(dbServerProcessSet.getProcess(i)
					.getSocketAddress(), i, false), i);
		}
		// ps.addProcess(new SampleProcess(clientProcessSet.getSelfProcess()
		// .getSocketAddress(), dbServerProcessSet.getSize(), true),
		// dbServerProcessSet.getSize());
		for (int i = 0; i < clientProcessSet.getAllProcesses().length; ++i) {
			ps.addProcess(new SampleProcess(clientProcessSet.getProcess(i)
					.getSocketAddress(), dbServerProcessSet.getSize() + i,
					i == selfId ? true : false), dbServerProcessSet.getSize()
					+ i);
		}

		this.clientParticipatedProcessSet = ps;
		clientChannel = getServerClientChannel(clientParticipatedProcessSet);

		try {
			clientChannel.start();
		} catch (AppiaDuplicatedSessionsException e) {
			e.printStackTrace();
		}

		/* All set. Appia main class will handle the rest */
		Logger.getLogger(ClientAppl.class.getName()).info("Starting Appia...");
		Appia.run();
	}

	public void sendRequest(Object[] req) {
		P2pMessage p2pm = new P2pMessage(req, leaderId, ChannelType.CLIENT);
		sendP2pMessage(p2pm);
	}

	public void sendMessageToClientNode(int clientId, Object message) {
		int nodeId = dbServerProcessSet.getSize() + clientId;
		P2pMessage p2pm = new P2pMessage(message, nodeId, ChannelType.CLIENT);
		sendP2pMessage(p2pm);
	}

	/**
	 * Sends a {@link P2pMessage}
	 * 
	 * @param c
	 *            the p2pMessage to be sent
	 */
	public void sendP2pMessage(P2pMessage c) {
		if (Logger.getLogger(ClientAppl.class.getName()).isLoggable(Level.FINE)) {
			Logger.getLogger(ClientAppl.class.getName()).fine(
					"Client " + selfId + " sends message to server "
							+ c.getReceiver());
		}
		try {
			SendableEvent ev = new SendableEvent();
			ev.getMessage().pushObject(c);
			ev.source = clientParticipatedProcessSet.getSelfProcess()
					.getSocketAddress();
			ev.dest = clientParticipatedProcessSet.getProcess(c.getReceiver())
					.getSocketAddress();
			ev.setSourceSession(null);
			ev.asyncGo(this.clientChannel, Direction.DOWN);
		} catch (AppiaEventException ex) {
			ex.printStackTrace();
		}

	}

	/**
	 * Should be called when all connections are established, and perfect
	 * failure detector should start working.
	 */
	public void startPFD() {
		PFDStartEvent pfdStart = new PFDStartEvent();
		try {
			pfdStart.asyncGo(this.clientChannel, Direction.DOWN);
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onNodeFail(int id, ChannelType group, Channel channel) {
		// TODO Auto-generated method stub
		nfListener.onNodeFail(id, group);
	}

	@Override
	public void onRecvP2pMessage(P2pMessage p2pm) {
		if (Logger.getLogger(ClientAppl.class.getName()).isLoggable(Level.FINE)) {
			Logger.getLogger(ClientAppl.class.getName()).fine(
					"Client " + selfId + " receives message from server ");
		}
		// System.out.println("client recv p2p");
		this.cP2pMListener.onRecvClientP2pMessage(p2pm);

	}
	
	public int getServerCount() {
		return dbServerProcessSet.getSize();
	}
	
	public int getClientCount() {
		return clientProcessSet.getSize();
	}

	private ProcessSet buildProcessSet(String str, int selfProc) {
		StringTokenizer st;
		ProcessSet set = new ProcessSet();
		String[] machines = str.split(",");

		for (String m : machines) {
			try {
				st = new StringTokenizer(m);
				if (st.countTokens() != 3) {
					Logger.getLogger(ClientAppl.class.getName()).severe(
							"Wrong line in file: " + st.countTokens());
					continue;
				}
				int procNumber = Integer.parseInt(st.nextToken());
				InetAddress addr = InetAddress.getByName(st.nextToken());
				int portNumber = Integer.parseInt(st.nextToken());
				boolean self = (procNumber == selfProc);
				SampleProcess process = new SampleProcess(
						new InetSocketAddress(addr, portNumber), procNumber,
						self);
				set.addProcess(process, procNumber);
			} catch (IOException e) {

			}
		}

		return set;
	}

	private Channel getServerClientChannel(ProcessSet processes) {
		/* Create layers and put them on a array */
		Layer[] qos = { new TcpCompleteLayer(), new TcpBasedPFDLayer(),
				// new BasicBroadcastLayer(), // new EagerRBLayer(),
				new ServerClientApplLayer() };

		/* Create a QoS */
		QoS myQoS = null;
		try {
			myQoS = new QoS("ServerClient QoS for client " + "-1", qos);
		} catch (AppiaInvalidQoSException ex) {
			Logger.getLogger(ClientAppl.class.getName()).severe("Invalid QoS");
			Logger.getLogger(ClientAppl.class.getName())
					.severe(ex.getMessage());
			System.exit(1);
		}
		/* Create a channel. Uses default event scheduler. */
		Channel channel = myQoS
				.createUnboundChannel("ServerClient channel for client " + "-1");
		/*
		 * Application Session requires special arguments: filename and . A
		 * session is created and binded to the stack. Remaining ones are
		 * created by default
		 */
		ServerClientApplSession sas = (ServerClientApplSession) qos[qos.length - 1]
				.createSession();
		sas.init(processes, this, this, true);

		ChannelCursor cc = channel.getCursor();

		/*
		 * Application is the last session of the array. Positioning in it is
		 * simple
		 */
		try {
			cc.top();
			cc.setSession(sas);

		} catch (AppiaCursorException ex) {
			Logger.getLogger(ClientAppl.class.getName()).severe(
					"Unexpected exception in main. Type code:" + ex.type);
			System.exit(1);
		}
		return channel;
	}
}
