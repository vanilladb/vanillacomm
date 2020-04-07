package org.vanilladb.comm.protocols.p2pappl;

import java.io.Serializable;

public interface P2pMessageListener {
	
    public void onRecvP2pMessage(int senderId, Serializable message);

}
