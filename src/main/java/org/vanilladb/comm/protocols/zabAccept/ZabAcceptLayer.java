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

package org.vanilladb.comm.protocols.zabAccept;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelInit;

import org.vanilladb.comm.protocols.events.Crash;
import org.vanilladb.comm.protocols.events.Nack;
import org.vanilladb.comm.protocols.events.PaxosPropose;
import org.vanilladb.comm.protocols.events.PaxosReturn;
import org.vanilladb.comm.protocols.events.ProcessInitEvent;
import org.vanilladb.comm.protocols.events.Write;
import org.vanilladb.comm.protocols.events.WriteAck;
import org.vanilladb.comm.protocols.events.ZabCacheTom;

/**
 * The basic paxos Algorithm.
 * 
 * @author DBN
 */
public class ZabAcceptLayer extends Layer {

	public ZabAcceptLayer() {

		/* events that the protocol will create */
		evProvide = new Class[] { ProcessInitEvent.class, Nack.class,
				PaxosReturn.class, WriteAck.class, ZabCacheTom.class };

		/*
		 * events that the protocol require to work. This is a subset of the
		 * accepted events
		 */
		// XXX ChannelInit might not be required
		evRequire = new Class[] { ChannelInit.class, ProcessInitEvent.class,
				PaxosPropose.class };

		/* events that the protocol will accept */
		evAccept = new Class[] { ProcessInitEvent.class, PaxosPropose.class,
				Nack.class, Write.class, WriteAck.class, Crash.class };
	}

	/**
	 * @see appia.Layer#createSession()
	 */
	public Session createSession() {
		return new ZabAcceptSession(this);
	}
}
