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
package org.vanilladb.comm.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.client.ClientAppl;
import org.vanilladb.comm.messages.ChannelType;
import org.vanilladb.comm.messages.NodeFailListener;
import org.vanilladb.comm.messages.P2pMessage;
import org.vanilladb.comm.messages.P2pMessageListener;
import org.vanilladb.comm.messages.TotalOrderMessage;
import org.vanilladb.comm.messages.TotalOrderedMessageListener;
import org.vanilladb.comm.protocols.basicBroadcast.BasicBroadcastLayer;
import org.vanilladb.comm.protocols.events.ZabRequest;
import org.vanilladb.comm.protocols.serverClientAppl.ServerClientApplLayer;
import org.vanilladb.comm.protocols.serverClientAppl.ServerClientApplSession;
import org.vanilladb.comm.protocols.tcpBasedPFD.PFDStartEvent;
import org.vanilladb.comm.protocols.tcpBasedPFD.TcpBasedPFDLayer;
import org.vanilladb.comm.protocols.tcpBasedPFD.TcpBasedPFDSession;
import org.vanilladb.comm.protocols.utils.ProcessSet;
import org.vanilladb.comm.protocols.utils.SampleProcess;
import org.vanilladb.comm.protocols.zabAccept.ZabAcceptLayer;
import org.vanilladb.comm.protocols.zabAppl.ZabApplLayer;
import org.vanilladb.comm.protocols.zabAppl.ZabApplSession;
import org.vanilladb.comm.protocols.zabTotalOrder.ZabTOBLayer;

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
import net.sf.appia.protocols.tcpcomplete.TcpCompleteSession;

public class ServerAppl extends Thread implements TotalOrderedMessageListener, P2pMessageListener, NodeFailListener {

	private final boolean IS_STANDALONE_SEQUENCER;
	private ProcessSet dbServerProcessSet;
	private ProcessSet[] clientParticipatedProcessSets;

	private Channel zabChannel;
	private Channel clientChannel;
	private ProcessSet clientParticipatedProcessSet;
	private Channel[] serverChannels;
	private Channel[] clientChannels;

	private String serverView, clientView;

	private ServerTotalOrderedMessageListener svTomListener;
	private ServerP2pMessageListener svP2pMListener;
	private ServerNodeFailListener nfListener;

	private int selfId;
	private int leaderId = 0;
	private long tomTime = 0;

	public ServerAppl(int selfId,
			ServerTotalOrderedMessageListener svTomListener,
			ServerP2pMessageListener svP2pMListener,
			ServerNodeFailListener nfListener) {

		this.selfId = selfId;
		this.svTomListener = svTomListener;
		this.svP2pMListener = svP2pMListener;
		this.nfListener = nfListener;

		if (svTomListener == null || svP2pMListener == null
				|| nfListener == null) {
			throw new IllegalArgumentException(
					"Must implement RequestListener, TupleSetListener and NodeFailListener");
		}

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
		prop = System.getProperty(ServerAppl.class.getName() + ".SERVER_VIEW");
		this.serverView = prop;
		prop = System.getProperty(ClientAppl.class.getName() + ".CLIENT_VIEW");
		this.clientView = prop;
		prop = System.getProperty(ServerAppl.class.getName()
				+ ".STAND_ALONE_SEQUENCER");
		IS_STANDALONE_SEQUENCER = (prop != null ? Boolean.parseBoolean(prop)
				: false);

	}

	public void run() {

		ProcessSet dbServerProcessSet = buildProcessSet(serverView, selfId);
		this.dbServerProcessSet = dbServerProcessSet;
		ProcessSet clientProcessSet = buildProcessSet(clientView, selfId);

		// XXX
		// standalone leader
		if (IS_STANDALONE_SEQUENCER) {
			leaderId = dbServerProcessSet.getSize() - 1;
		} else {
			leaderId = 0;
		}

		try {
			this.zabChannel = getZabChannel(dbServerProcessSet);
			zabChannel.start();
			this.serverChannels = new Channel[dbServerProcessSet.getSize()];
			for (int i = 0; i < dbServerProcessSet.getSize(); ++i) {
				this.serverChannels[i] = getServersChannel(
						this.dbServerProcessSet, i);
				// this.serverChannels[i].start();
			}

			this.clientChannels = new Channel[clientProcessSet.getSize()];
			this.clientParticipatedProcessSets = new ProcessSet[clientProcessSet
					.getSize()];

			clientParticipatedProcessSet = dbServerProcessSet.cloneProcessSet();
			for (int i = 0; i < clientProcessSet.getSize(); ++i) {
				ProcessSet ps = dbServerProcessSet.cloneProcessSet();
				ps.addProcess(new SampleProcess(clientProcessSet.getProcess(i)
						.getSocketAddress(), dbServerProcessSet.getSize(),
						false), dbServerProcessSet.getSize());
				clientParticipatedProcessSets[i] = ps;
				clientChannels[i] = getServerClientChannel(ps, i);
				// clientChannels[i].start();
				clientParticipatedProcessSet.addProcess(new SampleProcess(
						clientProcessSet.getProcess(i).getSocketAddress(),
						dbServerProcessSet.getSize() + i, false),
						dbServerProcessSet.getSize() + i);
			}

			clientChannel = getServerClientChannel(
					clientParticipatedProcessSet, -1);
			clientChannel.start();
		} catch (AppiaDuplicatedSessionsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/* All set. Appia main class will handle the rest */
		Logger.getLogger(ServerAppl.class.getName()).info("Starting Appia...");
		Appia.run();
	}

	/**
	 * Sends a total order message.
	 * 
	 * @param spcs
	 *            the TotalOrderedMessage to be sent
	 */
	public void sendTotalOrderRequest(Object[] spcs) {
		sendTotalOrderRequest(spcs, false);
	}
	
	public void sendTotalOrderRequest(Object[] spcs, boolean isAppiaThread) {
		TotalOrderMessage tom = new TotalOrderMessage(spcs);
		try {
			ZabRequest ev = new ZabRequest(this.zabChannel, Direction.DOWN,
					null, tom);
			if (isAppiaThread)
				ev.go();
			else
				ev.asyncGo(zabChannel, Direction.DOWN);
		} catch (AppiaEventException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Sends a {@link P2pMessage}
	 * 
	 * @param c
	 *            the p2pMessage to be sent
	 */
	public void sendP2pMessage(P2pMessage c) {
		if (c.getGroup() == ChannelType.SERVER) {
			if (Logger.getLogger(ServerAppl.class.getName()).isLoggable(
					Level.FINE)) {
				Logger.getLogger(ServerAppl.class.getName()).fine(
						"Server " + selfId + " sends message to server "
								+ c.getReceiver());
			}
			// System.out.println("server send p2p to server " +
			// c.getReceiver());
			try {
				SendableEvent ev = new SendableEvent();
				ev.getMessage().pushObject(c);
				ev.source = dbServerProcessSet.getSelfProcess()
						.getSocketAddress();
				ev.dest = dbServerProcessSet.getProcess(c.getReceiver())
						.getSocketAddress();
				ev.setSourceSession(null);
				// ev.asyncGo(this.serverChannels[c.getReceiver()],
				// Direction.DOWN);
				ev.asyncGo(this.zabChannel, Direction.DOWN);
			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}
		} else {
			if (Logger.getLogger(ServerAppl.class.getName()).isLoggable(
					Level.FINE)) {
				Logger.getLogger(ServerAppl.class.getName()).fine(
						"Server " + selfId + " sends message to client "
								+ c.getReceiver());
			}
			try {
				// System.out.println("server send p2p to client "
				// + c.getReceiver());
				SendableEvent ev = new SendableEvent();
				ev.getMessage().pushObject(c);
				// ev.source = clientParticipatedProcessSets[c.getReceiver()]
				// .getSelfProcess().getSocketAddress();
				// ev.dest = clientParticipatedProcessSets[c.getReceiver()]
				// .getProcess(dbServerProcessSet.getSize())
				// .getSocketAddress();
				// ev.setSourceSession(null);
				// ev.asyncGo(this.clientChannels[c.getReceiver()],
				// Direction.DOWN);

				ev.source = clientParticipatedProcessSet.getSelfProcess()
						.getSocketAddress();
				ev.dest = clientParticipatedProcessSet.getProcess(
						dbServerProcessSet.getSize() + c.getReceiver())
						.getSocketAddress();
				ev.setSourceSession(null);
				ev.asyncGo(this.clientChannel, Direction.DOWN);

			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Should be called when all connections are established, and perfect
	 * failure detector should start working.
	 */
	public void startPFD() {
		PFDStartEvent pfdStart = new PFDStartEvent();
		try {
			pfdStart.asyncGo(this.zabChannel, Direction.DOWN);
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onRecvTotalOrderedMessage(TotalOrderMessage tom) {
		if (IS_STANDALONE_SEQUENCER && this.selfId == leaderId) {
			return;
		}
		// long time = System.nanoTime();
		// System.out.println("server recv tom " + (time - tomTime) +
		// " tom size: " + tom.getMessages().length);
		// tomTime = time;
		this.svTomListener.onRecvServerTotalOrderedMessage(tom);
	}

	@Override
	public void onRecvP2pMessage(P2pMessage p2pm) {
		// System.out.println("server recv p2p");
		if (p2pm.getGroup() == ChannelType.SERVER) {
			this.svP2pMListener.onRecvServerP2pMessage(p2pm);
		} else if (p2pm.getGroup() == ChannelType.CLIENT) {
			TotalOrderMessage tom = new TotalOrderMessage(
					(Object[]) p2pm.getMessage());
			try {
				ZabRequest ev = new ZabRequest(this.zabChannel, Direction.DOWN,
						null, tom);
				ev.go();
			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}
		} else {
			throw new IllegalArgumentException();
		}
	}

	// XXX
	@Override
	public void onNodeFail(int id, ChannelType channelType, Channel c) {
		dbServerProcessSet.getProcess(id).setCorrect(false);
		nfListener.onNodeFail(id, channelType);
	}

	private ProcessSet buildProcessSet(String str, int selfProc) {
		StringTokenizer st;
		ProcessSet set = new ProcessSet();
		String[] machines = str.split(",");

		for (String m : machines) {
			try {
				st = new StringTokenizer(m);
				if (st.countTokens() != 3) {
					Logger.getLogger(ServerAppl.class.getName()).severe(
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

	private Channel getZabChannel(ProcessSet processes) {
		/* Create layers and put them on a array */
		Layer[] qos = { new TcpCompleteLayer(), new TcpBasedPFDLayer(),
				new BasicBroadcastLayer(), // new EagerRBLayer(),
				new ZabAcceptLayer(), new ZabTOBLayer(),

				new ZabApplLayer() };

		/* Create a QoS */
		QoS myQoS = null;
		try {
			myQoS = new QoS("ZAB QoS", qos);
		} catch (AppiaInvalidQoSException ex) {
			Logger.getLogger(ServerAppl.class.getName()).severe("Invalid QoS");
			Logger.getLogger(ServerAppl.class.getName())
					.severe(ex.getMessage());
			System.exit(1);
		}
		/* Create a channel. Uses default event scheduler. */
		Channel channel = myQoS.createUnboundChannel("Zab Channel");

		/*
		 * Application Session requires special arguments: filename and . A
		 * session is created and binded to the stack. Remaining ones are
		 * created by default
		 */
		ZabApplSession sas = (ZabApplSession) qos[qos.length - 1]
				.createSession();
		sas.init(processes, this, this, this);

		ChannelCursor cc = channel.getCursor();

		TcpCompleteSession tcpsession = (TcpCompleteSession) qos[0]
				.createSession();
		TcpBasedPFDSession tcppfdsession = (TcpBasedPFDSession) qos[1]
				.createSession();

		/*
		 * Application is the last session of the array. Positioning in it is
		 * simple
		 */
		try {
			cc.top();
			cc.setSession(sas);
			cc.bottom();
			cc.setSession(tcpsession);
			cc.up();
			cc.setSession(tcppfdsession);
		} catch (AppiaCursorException ex) {
			Logger.getLogger(ServerAppl.class.getName()).severe(
					"Unexpected exception in main. Type code:" + ex.type);
			System.exit(1);
		}

		return channel;
	}

	private Channel getServersChannel(ProcessSet processes, int id) {
		/* Create layers and put them on a array */
		Layer[] qos = { new TcpCompleteLayer(), new TcpBasedPFDLayer(),
				new BasicBroadcastLayer(), // new EagerRBLayer(),
				new ServerClientApplLayer() };

		/* Create a QoS */
		QoS myQoS = null;
		try {
			myQoS = new QoS("ServerClient QoS for " + "server " + id, qos);
		} catch (AppiaInvalidQoSException ex) {
			Logger.getLogger(ServerAppl.class.getName()).severe("Invalid QoS");
			Logger.getLogger(ServerAppl.class.getName())
					.severe(ex.getMessage());
			System.exit(1);
		}
		/* Create a channel. Uses default event scheduler. */
		Channel channel = myQoS
				.createUnboundChannel("ServerClient channel for " + "server "
						+ id);
		/*
		 * Application Session requires special arguments: filename and . A
		 * session is created and binded to the stack. Remaining ones are
		 * created by default
		 */
		ServerClientApplSession sas = (ServerClientApplSession) qos[qos.length - 1]
				.createSession();
		sas.init(processes, this, this, this, false);

		ChannelCursor cc = channel.getCursor();

		/*
		 * Application is the last session of the array. Positioning in it is
		 * simple
		 */
		try {
			cc.top();
			cc.setSession(sas);
			cc.bottom();
			ChannelCursor ccc = this.zabChannel.getCursor();
			ccc.bottom();
			cc.setSession(ccc.getSession());
			ccc.up();
			cc.up();
			cc.setSession(ccc.getSession());
		} catch (AppiaCursorException ex) {
			Logger.getLogger(ServerAppl.class.getName()).severe(
					"Unexpected exception in main. Type code:" + ex.type);
			System.exit(1);
		}
		return channel;
	}

	private Channel getServerClientChannel(ProcessSet processes, int id) {
		/* Create layers and put them on a array */
		Layer[] qos = { new TcpCompleteLayer(), new TcpBasedPFDLayer(),
				// new BasicBroadcastLayer(), // new EagerRBLayer(),
				new ServerClientApplLayer() };

		/* Create a QoS */
		QoS myQoS = null;
		try {
			myQoS = new QoS("ServerClient QoS for " + "client " + id, qos);
		} catch (AppiaInvalidQoSException ex) {
			Logger.getLogger(ServerAppl.class.getName()).severe("Invalid QoS");
			Logger.getLogger(ServerAppl.class.getName())
					.severe(ex.getMessage());
			System.exit(1);
		}
		/* Create a channel. Uses default event scheduler. */
		Channel channel = myQoS
				.createUnboundChannel("ServerClient channel for " + "client "
						+ id);
		/*
		 * Application Session requires special arguments: filename and . A
		 * session is created and binded to the stack. Remaining ones are
		 * created by default
		 */
		ServerClientApplSession sas = (ServerClientApplSession) qos[qos.length - 1]
				.createSession();
		sas.init(processes, this, this, false);

		ChannelCursor cc = channel.getCursor();

		/*
		 * Application is the last session of the array. Positioning in it is
		 * simple
		 */
		try {
			cc.top();
			cc.setSession(sas);
			cc.bottom();
			ChannelCursor ccc = this.zabChannel.getCursor();
			ccc.bottom();
			cc.setSession(ccc.getSession());
			ccc.up();
			cc.up();
			cc.setSession(ccc.getSession());
		} catch (AppiaCursorException ex) {
			Logger.getLogger(ServerAppl.class.getName()).severe(
					"Unexpected exception in main. Type code:" + ex.type);
			System.exit(1);
		}
		return channel;
	}

}
