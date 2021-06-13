package org.vanilladb.comm.protocols.p2pcounting;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.protocols.events.ProcessListInit;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;

public class P2pCountingSession extends Session {
	
	private static final long REPORT_PERIOD = 5000;

	private ProcessList processList;
	private Map<SocketAddress, AtomicInteger> froms = new ConcurrentHashMap<SocketAddress, AtomicInteger>();
	private Map<SocketAddress, AtomicInteger> tos = new ConcurrentHashMap<SocketAddress, AtomicInteger>();
	private Map<String, AtomicInteger> types = new ConcurrentHashMap<String, AtomicInteger>();
	
	P2pCountingSession(Layer layer) {
		super(layer);
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				long startTime = System.currentTimeMillis();
				long lastTime = startTime;
				
				while (true) {
					long currentTime = System.currentTimeMillis();
					
					if (currentTime - lastTime > REPORT_PERIOD) {
						lastTime = currentTime;
						
						double t = (currentTime - startTime) / 1000;
						StringBuilder sb = new StringBuilder();
						
						sb.append("===================================\n");
						sb.append(String.format("At %.2f:\n", t));
						
						for (int i = 0; i < processList.getSize(); i++) {
							SocketAddress addr = processList.getProcess(i).getAddress();
							sb.append(String.format("From server %d: %d\n", i, froms.get(addr).getAndSet(0)));
							sb.append(String.format("To server %d: %d\n", i, tos.get(addr).getAndSet(0)));
						}
						for (String name : types.keySet()) {
							sb.append(String.format("Count for %s: %d\n", name, types.get(name).getAndSet(0)));
						}
						
						sb.append("===================================\n");
						System.out.println(sb.toString());
					}
					
					try {
						Thread.sleep(REPORT_PERIOD / 10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		}).start();
	}
	
	@Override
	public void handle(Event event) {
		if (event instanceof ProcessListInit)
			handleProcessListInit((ProcessListInit) event);
		else if (event instanceof SendableEvent)
			handleSendableEvent((SendableEvent) event);
	}
	
	private void handleProcessListInit(ProcessListInit event) {
		// Save the list
		this.processList = event.copyProcessList();
		
		// Initializes the counters
		for (int i = 0; i < processList.getSize(); i++) {
			froms.put(processList.getProcess(i).getAddress(), new AtomicInteger());
			tos.put(processList.getProcess(i).getAddress(), new AtomicInteger());
		}
		
		// Let the event continue
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleSendableEvent(SendableEvent event) {
		if (event.getDir() == Direction.UP) {
			SocketAddress from = (SocketAddress) event.source;
			AtomicInteger count = froms.getOrDefault(from, new AtomicInteger());
			count.incrementAndGet();
		} else {
			SocketAddress to = (SocketAddress) event.dest;
			AtomicInteger count = tos.getOrDefault(to, new AtomicInteger());
			count.incrementAndGet();
		}
		
		// Debug: detect self-destination event
//		if (event.dest.equals(processList.getSelfProcess().getAddress()))
//			throw new RuntimeException("Self sent from " + event.getSourceSession().getClass().getSimpleName());
		
		// Record the type
		String name = event.getClass().getSimpleName();
		AtomicInteger count = types.get(name);
		if (count == null) {
			count = new AtomicInteger(0);
			types.put(name, count);
		}
		count.incrementAndGet();
		
		// Let the event continue
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
}
