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
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A list of processes.
 * 
 * @author nuno, yslin
 */
public class ProcessList implements Serializable {
	
    private static final long serialVersionUID = 20200318001L;
    
    public static class Builder {
    	List<CommProcess> processes;
    	
    	public Builder() {
    		processes = new ArrayList<CommProcess>();
    	}
    	
    	public void addProcess(CommProcess process) {
    		if (process.getId() != processes.size())
    			throw new IllegalArgumentException("Process must be added with increasing ids");
    		processes.add(process);
    	}
    	
    	public ProcessList build() {
    		return new ProcessList(processes.toArray(new CommProcess[processes.size()]));
    	}
    }

    private CommProcess[] processes;
    private int selfId = -1;
    private Map<SocketAddress, Integer> addressToId = new HashMap<SocketAddress, Integer>();
    
    private ProcessList(CommProcess[] processes) {
        this.processes = processes;
        for (CommProcess process : processes) {
        	// Find self
        	if (process.isSelf()) {
        		selfId = process.getId();
        	}
        	addressToId.put(process.getAddress(), process.getId());
        }
    }
    
    /**
     * Clone the given process list.
     * 
     * @param processList the process list to be cloned
     */
    public ProcessList(ProcessList processList) {
    	CommProcess[] targets = processList.processes;
    	this.processes = new CommProcess[targets.length];
        for (int i = 0; i < targets.length; i++)
        	processes[i] = new CommProcess(targets[i]);
        selfId = processList.selfId;
        addressToId = new HashMap<SocketAddress, Integer>(processList.addressToId);
    }

    /**
     * Gets the number of processes.
     * 
     * @return number of processes
     */
    public int getSize() {
        return processes.length;
    }

    /**
     * Gets the id of the process with the specified address.
     * 
     * @param addr
     *            the address of the process
     * @return the id of the process
     */
    public int getId(SocketAddress addr) {
    	Integer id = addressToId.get(addr);
    	if (id == null)
    		return -1;
    	return id;
    }

    /**
     * Gets the process with the specified address.
     * 
     * @param addr
     *            the address of the process
     * @return the process
     */
    public CommProcess getProcess(SocketAddress addr) {
        int i = getId(addr);
        if (i == -1)
            return null;
        else
            return processes[i];
    }

    /**
     * Gets the process with the given id
     * 
     * @param id
     *            the id of the process
     * @return the process
     */
    public CommProcess getProcess(int id) {
        return processes[id];
    }

    /**
     * Gets the self id.
     * 
     * @return my id. -1 if there is no self process.
     */
    public int getSelfId() {
        return selfId;
    }

    /**
     * Gets the self process.
     * 
     * @return My process
     */
    public CommProcess getSelfProcess() {
    	if (selfId == -1)
    		return null;
        return processes[selfId];
    }
    
    /**
     * Checks if all the processes are correct.
     * 
     * @return true, if they are correct. False, otherwise.
     */
    public boolean areAllCorrect() {
    	for (int id = 0; id < processes.length; id++) {
    		if (!processes[id].isCorrect())
    			return false;
    	}
    	return true;
    }
    
    public Set<Integer> getCorrectProcessIds() {
    	Set<Integer> corrects = new HashSet<Integer>();
    	for (int id = 0; id < processes.length; id++) {
    		if (processes[id].isCorrect())
    			corrects.add(id);
    	}
    	return corrects;
    }
    
    public int getCorrectCount() {
    	int count = 0;
    	for (int id = 0; id < processes.length; id++) {
    		if (processes[id].isCorrect())
    			count++;
    	}
    	return count;
    }
}
