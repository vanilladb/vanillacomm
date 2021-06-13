package org.vanilladb.comm.client;

import java.io.Serializable;

import org.vanilladb.comm.view.ProcessType;

public interface VanillaCommClientListener {
	
	void onReceiveP2pMessage(ProcessType senderType, int senderId, Serializable message);
	
}
