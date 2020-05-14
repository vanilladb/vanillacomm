package org.vanilladb.comm.process;

public interface ProcessStateListener {
	
    void onAllProcessesReady();
	
    void onProcessFailed(int failedProcessId);

}
