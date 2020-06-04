package org.vanilladb.comm.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessStateListener;
import org.vanilladb.comm.protocols.beb.BestEffortBroadcastLayer;
import org.vanilladb.comm.protocols.p2pappl.P2pApplicationLayer;
import org.vanilladb.comm.protocols.p2pappl.P2pMessage;
import org.vanilladb.comm.protocols.p2pappl.P2pMessageListener;
import org.vanilladb.comm.protocols.p2pcounting.P2pCountingLayer;
import org.vanilladb.comm.protocols.tcpfd.TcpFailureDetectionLayer;
import org.vanilladb.comm.protocols.totalorderappl.TotalOrderApplicationLayer;
import org.vanilladb.comm.protocols.totalorderappl.TotalOrderMessageListener;
import org.vanilladb.comm.protocols.totalorderappl.TotalOrderRequest;
import org.vanilladb.comm.protocols.zabacceptance.ZabAcceptanceLayer;
import org.vanilladb.comm.protocols.zabelection.ZabElectionLayer;
import org.vanilladb.comm.protocols.zabproposal.ZabProposalLayer;
import org.vanilladb.comm.view.ProcessType;
import org.vanilladb.comm.view.ProcessView;

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
import net.sf.appia.core.Session;
import net.sf.appia.protocols.tcpcomplete.TcpCompleteLayer;

public class VanillaCommServer implements P2pMessageListener, ProcessStateListener,
		TotalOrderMessageListener, Runnable {
	private static Logger logger = Logger.getLogger(VanillaCommServer.class.getName());
	
	private VanillaCommServerListener listener;
	private Channel zabChannel;
	private Channel p2pChannel;
	private Session commonTcpSession, commonFdSession;
	
	public VanillaCommServer(int selfId, VanillaCommServerListener listener) {
		int globalSelfId = ProcessView.toGlobalId(ProcessType.SERVER, selfId);
		this.listener = listener;
		createCommonSessions();
		setupZabChannel(globalSelfId);
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
	
	public void sendTotalOrderMessage(Serializable message) {
		try {
			List<Serializable> messages = new ArrayList<Serializable>();
			messages.add(message);
			TotalOrderRequest total = new TotalOrderRequest(messages);
			total.asyncGo(zabChannel, Direction.DOWN);
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	public void sendTotalOrderMessages(List<Serializable> messages) {
		try {
			TotalOrderRequest total = new TotalOrderRequest(messages);
			total.asyncGo(zabChannel, Direction.DOWN);
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onRecvP2pMessage(int senderId, Serializable message) {
		listener.onReceiveP2pMessage(ProcessView.toProcessType(senderId),
				ProcessView.toLocalId(senderId), message);
	}

	@Override
	public void onRecvTotalOrderMessage(long serialNumber, Serializable message) {
		listener.onReceiveTotalOrderMessage(serialNumber, message);
	}

	@Override
	public void onAllProcessesReady() {
		if (logger.isLoggable(Level.INFO))
			logger.info("All processes are ready.");
		listener.onServerReady();
	}

	@Override
	public void onProcessFailed(int failedProcessId) {
		if (logger.isLoggable(Level.SEVERE))
			logger.severe("Server " + failedProcessId + " failed");
		if (ProcessView.toProcessType(failedProcessId) == ProcessType.SERVER)
			listener.onServerFailed(ProcessView.toLocalId(failedProcessId));
	}
	
	public int getServerCount() {
		return ProcessView.SERVER_COUNT;
	}
	
	public int getClientCount() {
		return ProcessView.CLIENT_COUNT;
	}
	
	private void createCommonSessions() {
		Layer layer; 
		
		layer = new TcpCompleteLayer();
		commonTcpSession = layer.createSession();
		
		layer = new TcpFailureDetectionLayer();
		commonFdSession = layer.createSession();
	}
	
	private void setupZabChannel(int globalSelfId) {
		try {
			ProcessList processList = ProcessView.buildServersProcessList(globalSelfId);
			Layer[] layers = new Layer[] {
				new TcpCompleteLayer(),
				new TcpFailureDetectionLayer(),
//				new P2pCountingLayer(), // Debug Layer
				new BestEffortBroadcastLayer(),
				new ZabElectionLayer(),
				new ZabAcceptanceLayer(),
				new ZabProposalLayer(),
				new TotalOrderApplicationLayer(this, this, processList, true)
			};
			QoS qos = new QoS("Zab QoS", layers);
			zabChannel = qos.createUnboundChannel("Zab Channel");
			
			// Set common sessions
			try {
				ChannelCursor cc = zabChannel.getCursor();
				cc.bottom();
				cc.setSession(commonTcpSession);
				cc.up();
				cc.setSession(commonFdSession);
			} catch (AppiaCursorException ex) {
				ex.printStackTrace();
			}

			zabChannel.start();

		} catch (AppiaInvalidQoSException e) {
			e.printStackTrace();
		} catch (AppiaDuplicatedSessionsException e) {
			e.printStackTrace();
		}
	}
	
	private void setupP2pChannel(int globalSelfId) {
		try {
			ProcessList processList = ProcessView.buildAllProcessList(globalSelfId);
			Layer[] layers = new Layer[] {
				new TcpCompleteLayer(),
				new TcpFailureDetectionLayer(),
				new P2pApplicationLayer(this, processList, false)
			};
			QoS qos = new QoS("P2P QoS", layers);
			p2pChannel = qos.createUnboundChannel("P2P Channel");
			
			// Set common sessions
			try {
				ChannelCursor cc = p2pChannel.getCursor();
				cc.bottom();
				cc.setSession(commonTcpSession);
				cc.up();
				cc.setSession(commonFdSession);
			} catch (AppiaCursorException ex) {
				ex.printStackTrace();
			}

			p2pChannel.start();

		} catch (AppiaInvalidQoSException e) {
			e.printStackTrace();
		} catch (AppiaDuplicatedSessionsException e) {
			e.printStackTrace();
		}
	}

}
