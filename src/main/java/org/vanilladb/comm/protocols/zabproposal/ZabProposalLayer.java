package org.vanilladb.comm.protocols.zabproposal;

import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.totalorderappl.TotalOrderMessages;
import org.vanilladb.comm.protocols.totalorderappl.TotalOrderRequest;
import org.vanilladb.comm.protocols.zabacceptance.ZabCommit;
import org.vanilladb.comm.protocols.zabelection.LeaderChanged;
import org.vanilladb.comm.protocols.zabelection.LeaderInit;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

public class ZabProposalLayer extends Layer {
	
	public ZabProposalLayer() {
		// Events that the protocol will create
		evProvide = new Class[] {
			ZabPropose.class,
			TotalOrderMessages.class
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ProcessListInit.class,
			LeaderInit.class,
			TotalOrderRequest.class,
			ZabCommit.class,
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ProcessListInit.class,
			LeaderInit.class,
			LeaderChanged.class,
			TotalOrderRequest.class,
			ZabPropose.class,
			ZabCommit.class,
		};
	}

	@Override
	public Session createSession() {
		return new ZabProposalSession(this);
	}
}
