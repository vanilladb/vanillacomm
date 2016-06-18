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

package org.vanilladb.comm.protocols.utils;

import java.io.Serializable;
import java.net.SocketAddress;

/**
 * Represents a process in the system.
 * 
 * @author nuno
 */
public class SampleProcess implements Serializable {
	private static final long serialVersionUID = 3677871909022936117L;

	private SocketAddress address;

	private int processNumber;

	private boolean self, correct, initialized;

	public SampleProcess(SocketAddress addr, int proc, boolean self) {
		this.address = addr;
		this.processNumber = proc;
		this.self = self;
		correct = true;
		initialized = false;
	}

	/**
	 * Gets the address of the process.
	 * 
	 * @return the address of the process
	 */
	public SocketAddress getSocketAddress() {
		return address;
	}

	/**
	 * Gets the process number.
	 * 
	 * @return the process number
	 */
	public int getProcessNumber() {
		return processNumber;
	}

	/**
	 * Is it my own process.
	 * 
	 * @return true if the process is my self.
	 */
	public boolean isSelf() {
		return self;
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
		if (!(test instanceof SampleProcess))
			return false;
		SampleProcess proc = (SampleProcess) test;
		return address.equals(proc.address)
				&& (processNumber == proc.processNumber) && (self == proc.self);
	}

	@Override
	public int hashCode() {
		int hash = 17;
		hash = hash * 31 + address.hashCode();
		hash = hash * 31 + processNumber;
		hash = hash * 31 + (self ? 1 : 0);
		return hash;
	}

	/**
	 * Is the process correct. Gets the correctness value.
	 * 
	 * @return true if correct, false otherwise
	 */
	public boolean isCorrect() {
		return correct;
	}

	/**
	 * Sets the correctness of the process.
	 * 
	 * @param b
	 *            the correct value
	 */
	public void setCorrect(boolean b) {
		correct = b;
	}

	/**
	 * Is the process initialized. Gets the initialized value.
	 * 
	 * @return true if initialized, false otherwise
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * Sets if the process has initiated.
	 * 
	 * @param initialized
	 *            the initialized value
	 */
	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/**
	 * Creates a clone of the process.
	 * 
	 * @return the process clone
	 */
	public SampleProcess cloneProcess() {
		SampleProcess p = new SampleProcess(address, processNumber, self);
		p.correct = correct;
		p.initialized = initialized;
		return p;
	}

}
