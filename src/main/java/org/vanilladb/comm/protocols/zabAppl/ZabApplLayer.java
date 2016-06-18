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

package org.vanilladb.comm.protocols.zabAppl;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.RegisterSocketEvent;

import org.vanilladb.comm.protocols.events.BroadcastEvent;
import org.vanilladb.comm.protocols.events.ProcessInitEvent;
import org.vanilladb.comm.protocols.events.ZabRequest;
import org.vanilladb.comm.protocols.events.ZabTomResult;
import org.vanilladb.comm.protocols.tcpBasedPFD.PFDStartEvent;

/**
 * Layer of the application protocol.
 * 
 * @author nuno
 */
public class ZabApplLayer extends Layer {

	public ZabApplLayer() {
		/* events that the protocol will create */
		evProvide = new Class[] { ProcessInitEvent.class,
				RegisterSocketEvent.class, PFDStartEvent.class,
				SendableEvent.class, ZabRequest.class };

		/*
		 * events that the protocol require to work. This is a subset of the
		 * accepted events
		 */
		evRequire = new Class[] { ChannelInit.class };

		/* events that the protocol will accept */
		evAccept = new Class[] { ChannelInit.class, ChannelClose.class,
				RegisterSocketEvent.class, SendableEvent.class,
				BroadcastEvent.class,  ZabTomResult.class };

	}

	/**
	 * Creates a new session to this protocol.
	 * 
	 * @see appia.Layer#createSession()
	 */
	public Session createSession() {
		return new ZabApplSession(this);
	}

}
