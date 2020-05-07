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


package org.vanilladb.comm.process;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * Represents a process in the system.
 * 
 * @author nuno, yslin
 */
public class CommProcess implements Serializable {
	
	private static final long serialVersionUID = 20200318001L;
	
	// Identifiers
	private int id;
	private InetSocketAddress address;
	private boolean isSelf;
	
	// Machine States
	private ProcessState state;

	public CommProcess(InetSocketAddress addr, int id, boolean isSelf) {
		this.address = addr;
		this.id = id;
		this.isSelf = isSelf;
		if (isSelf)
			this.state = ProcessState.CORRECT;
		else
			this.state = ProcessState.UNINITIALIZED;
	}

    /**
     * Clone the given process.
     * 
     * @param process the process to be cloned
     */
	public CommProcess(CommProcess process) {
		this.address = process.address;
		this.id = process.id;
		this.isSelf = process.isSelf;
		this.state = process.state;
	}

	/**
	 * Gets the address of the process.
	 * 
	 * @return the address of the process
	 */
	public InetSocketAddress getAddress() {
		return address;
	}

	/**
	 * Gets the process id.
	 * 
	 * @return the process id
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Check if the process is self.
	 * 
	 * @return true if it is self, false otherwise
	 */
	public boolean isSelf() {
		return isSelf;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object test) {
		if (test == this)
			return true;
		if (test == null)
			return false;
		if (!(test instanceof CommProcess))
			return false;
		CommProcess proc = (CommProcess) test;
		return address.equals(proc.address)
				&& (id == proc.id);
	}

	@Override
	public int hashCode() {
		int hash = 17;
		hash = hash * 31 + address.hashCode();
		hash = hash * 31 + id;
		return hash;
	}
	
	public void setState(ProcessState state) {
		this.state = state;
	}

	/**
	 * Is the process initialized.
	 * 
	 * @return true if initialized, false otherwise
	 */
	public boolean isInitialized() {
		return state != ProcessState.UNINITIALIZED;
	}

	/**
	 * Is the process correct.
	 * 
	 * @return true if correct, false otherwise
	 */
	public boolean isCorrect() {
		return state == ProcessState.CORRECT;
	}
}
