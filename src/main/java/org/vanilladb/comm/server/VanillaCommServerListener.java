package org.vanilladb.comm.server;

import java.io.Serializable;

import org.vanilladb.comm.view.ProcessType;

public interface VanillaCommServerListener {
	
	void onServerReady();
	
	void onServerFailed(int failedServerId);
	
	void onReceiveP2pMessage(ProcessType senderType, int senderId, Serializable message);
	
	void onReceiveTotalOrderMessage(long serialNumber, Serializable message);
	
}
