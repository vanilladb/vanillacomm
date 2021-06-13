package org.vanilladb.comm.view;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.vanilladb.comm.process.CommProcess;
import org.vanilladb.comm.process.ProcessList;

public class ProcessView {
	
	public static final ProcessList SERVER_LIST;
	public static final ProcessList CLIENT_LIST;
	public static final int SERVER_COUNT;
	public static final int CLIENT_COUNT;
	
	static {
		// read config file
		String path = System.getProperty("org.vanilladb.comm.config.file");
		if (path != null && !path.isEmpty()) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(path);
				System.getProperties().load(fis);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				try {
					if (fis != null)
						fis.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		} else {
			throw new RuntimeException("Cannot find the view configuration file in system properties.");
		}
		
		String serverListStr = System.getProperty(ProcessView.class.getName() + ".SERVER_VIEW");
		String clientListStr = System.getProperty(ProcessView.class.getName() + ".CLIENT_VIEW");
		
		SERVER_LIST = buildProcessList(serverListStr);
		CLIENT_LIST = buildProcessList(clientListStr);
		SERVER_COUNT = SERVER_LIST.getSize();
		CLIENT_COUNT = CLIENT_LIST.getSize();
	}
	
	public static int toGlobalId(ProcessType type, int id) {
		if (type == ProcessType.SERVER)
			return id;
		else
			return SERVER_LIST.getSize() + id;
	}
	
	public static ProcessType toProcessType(int globalId) {
		if (globalId < SERVER_LIST.getSize()) {
			return ProcessType.SERVER;
		} else {
			return ProcessType.CLIENT;
		}
	}
	
	public static int toLocalId(int globalId) {
		if (globalId < SERVER_LIST.getSize()) {
			return globalId;
		} else {
			return globalId - SERVER_LIST.getSize();
		}
	}
	
	public static ProcessList buildServersProcessList(int selfGlobalId) {
		ProcessList.Builder builder = new ProcessList.Builder();
		for (int serverId = 0; serverId < SERVER_LIST.getSize(); serverId++) {
			CommProcess process = SERVER_LIST.getProcess(serverId);
			builder.addProcess(new CommProcess(process.getAddress(), serverId,
					(process.getId() == selfGlobalId)));
		}
		return builder.build();
	}
	
	public static ProcessList buildAllProcessList(int selfGlobalId) {
		ProcessList.Builder builder = new ProcessList.Builder();
		for (int serverId = 0; serverId < SERVER_LIST.getSize(); serverId++) {
			CommProcess process = SERVER_LIST.getProcess(serverId);
			builder.addProcess(new CommProcess(process.getAddress(), serverId,
					(process.getId() == selfGlobalId)));
		}
		for (int clientId = 0; clientId < CLIENT_LIST.getSize(); clientId++) {
			CommProcess process = CLIENT_LIST.getProcess(clientId);
			int id = clientId + SERVER_LIST.getSize();
			builder.addProcess(new CommProcess(process.getAddress(), id,
					(id == selfGlobalId)));
		}
		return builder.build();
	}

	private static ProcessList buildProcessList(String viewStr) {
		ProcessList.Builder builder = new ProcessList.Builder();
		StringTokenizer processSetSpliter = new StringTokenizer(viewStr, ",");

		while (processSetSpliter.hasMoreTokens()) {
			String processStr = processSetSpliter.nextToken();
			try {
				StringTokenizer tokenizer = new StringTokenizer(processStr);
				if (tokenizer.countTokens() != 3) {
					throw new RuntimeException("Machine view format error: "
							+ tokenizer.countTokens());
				}
				int processId = Integer.parseInt(tokenizer.nextToken());
				InetAddress addr = InetAddress.getByName(tokenizer.nextToken());
				int portNumber = Integer.parseInt(tokenizer.nextToken());
				CommProcess process = new CommProcess(
						new InetSocketAddress(addr, portNumber), processId, false);
				builder.addProcess(process);
			} catch (UnknownHostException e) {
				throw new RuntimeException("cannot build parse: " + processStr, e);
			}
		}

		return builder.build();
	}

}
