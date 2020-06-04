package org.vanilladb.comm.client;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.protocols.p2pappl.P2pApplicationLayer;
import org.vanilladb.comm.protocols.p2pappl.P2pMessage;
import org.vanilladb.comm.protocols.p2pappl.P2pMessageListener;
import org.vanilladb.comm.protocols.tcpfd.TcpFailureDetectionLayer;
import org.vanilladb.comm.view.ProcessType;
import org.vanilladb.comm.view.ProcessView;

import net.sf.appia.core.Appia;
import net.sf.appia.core.AppiaDuplicatedSessionsException;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaInvalidQoSException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Layer;
import net.sf.appia.core.QoS;
import net.sf.appia.protocols.tcpcomplete.TcpCompleteLayer;

public class VanillaCommClient implements P2pMessageListener, Runnable {
	private static Logger logger = Logger.getLogger(VanillaCommClient.class.getName());

	private VanillaCommClientListener listener;
	private Channel p2pChannel;

	public VanillaCommClient(int selfId, VanillaCommClientListener listener) {
		int globalSelfId = ProcessView.toGlobalId(ProcessType.CLIENT, selfId);
		this.listener = listener;
		setupP2pChannel(globalSelfId);
		
		// Disable Log4j Logging which is the default logger of Appia
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
	}

	@Override
	public void run() {
		if (logger.isLoggable(Level.INFO))
			logger.info("Starts the network service");
		
		Appia.run();
	}

	public void sendP2pMessage(ProcessType receiverType, int receiverId, Serializable message) {
		try {
			int globalId = ProcessView.toGlobalId(receiverType, receiverId);
			P2pMessage p2p = new P2pMessage(message, globalId);
			p2p.asyncGo(p2pChannel, Direction.DOWN);
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onRecvP2pMessage(int senderId, Serializable message) {
		listener.onReceiveP2pMessage(ProcessView.toProcessType(senderId), ProcessView.toLocalId(senderId), message);
	}
	
	public int getServerCount() {
		return ProcessView.SERVER_COUNT;
	}
	
	public int getClientCount() {
		return ProcessView.CLIENT_COUNT;
	}

	private void setupP2pChannel(int globalSelfId) {
		try {
			ProcessList processList = ProcessView.buildAllProcessList(globalSelfId);
			Layer[] layers = new Layer[] {
				new TcpCompleteLayer(),
				new TcpFailureDetectionLayer(),
				new P2pApplicationLayer(this, processList, true)
			};
			QoS qos = new QoS("P2P QoS", layers);
			p2pChannel = qos.createUnboundChannel("P2P Channel");
			p2pChannel.start();
		} catch (AppiaInvalidQoSException e) {
			e.printStackTrace();
		} catch (AppiaDuplicatedSessionsException e) {
			e.printStackTrace();
		}
	}

}
