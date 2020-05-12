package org.vanilladb.comm.client;

import java.io.Serializable;

import org.vanilladb.comm.view.ProcessType;
import org.vanilladb.comm.view.ProcessView;

public class ClientDemo implements VanillaCommClientListener {
	
	private static final int REQ_ID_SHIFT = 100000;
	
	public static void main(String[] args) {
		int selfId = Integer.parseInt(args[0]);
		VanillaCommClient client = new VanillaCommClient(selfId, new ClientDemo());
		new Thread(client).start();
		int serverCount = ProcessView.buildServersProcessList(-1).getSize();
		sendRequestPeriodically(selfId, client, serverCount);
	}

	private static void sendRequestPeriodically(final int selfId,
			final VanillaCommClient client, final int serverCount) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				int count = REQ_ID_SHIFT * selfId;
				int targetServerId = selfId % serverCount;

				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					String message = String.format("Request #%d from client %d", count,
							selfId);
					client.sendP2pMessage(ProcessType.SERVER, targetServerId, message);
					
					count++;

				}
			}

		}).start();
	}

	@Override
	public void onReceiveP2pMessage(ProcessType senderType, int senderId, Serializable message) {
		System.out.println("Received a P2P message from " + senderType + " " + senderId
				+ ", message: " + message);
	}
}
