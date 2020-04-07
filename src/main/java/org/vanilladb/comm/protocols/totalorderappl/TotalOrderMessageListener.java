package org.vanilladb.comm.protocols.totalorderappl;

import java.io.Serializable;

public interface TotalOrderMessageListener {
	
    void onRecvTotalOrderMessage(int serialNumber, Serializable message);
	
}
