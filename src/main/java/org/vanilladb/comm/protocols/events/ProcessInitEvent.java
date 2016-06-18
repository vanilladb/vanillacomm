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

package org.vanilladb.comm.protocols.events;

import org.vanilladb.comm.protocols.utils.ProcessSet;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

/**
 * Distributes the process set to the interested Sessions.
 * 
 * @author nuno
 */
public class ProcessInitEvent extends Event {

    private ProcessSet processSet;

    /**
     * Default constructor.
     */
    public ProcessInitEvent() {
        super();
    }

    /**
     * Constructor of the event.
     * 
     * @param ps
     *            The process set.
     */
    public ProcessInitEvent(ProcessSet ps) {
        super();
        processSet = ps;
    }

    /**
     * Constructor of the event.
     * 
     * @param channel
     *            The Appia Channel.
     * @param dir
     *            The direction of the event.
     * @param source
     *            the session that created the event.
     * @throws AppiaEventException
     */
    public ProcessInitEvent(Channel channel, int dir, Session source)
            throws AppiaEventException {
        super(channel, dir, source);
    }

    /**
     * Gets a copy of the process set.
     * 
     * @return a copy of the process set.
     */
    public ProcessSet getProcessSet() {
        return processSet.cloneProcessSet();
    }

    /**
     * Sets the process set.
     * 
     * @param set
     *            the process set.
     */
    public void setProcessSet(ProcessSet set) {
        processSet = set;
    }

}
