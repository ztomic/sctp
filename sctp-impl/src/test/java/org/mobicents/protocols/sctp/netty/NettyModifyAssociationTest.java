/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual
 * contributors as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * 
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free 
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.protocols.sctp.netty;

import static org.junit.jupiter.api.Assertions.assertTrue;
import io.netty.buffer.Unpooled;

import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.api.AssociationListener;
import org.mobicents.protocols.api.IpChannelType;
import org.mobicents.protocols.api.PayloadData;
import org.mobicents.protocols.sctp.SctpTransferTest;

/**
 * @author nosach kostiantyn
 * 
 */
public class NettyModifyAssociationTest {

	private static final String SERVER_NAME = "testserver";
	private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT1 = 12354;
    private static final int SERVER_PORT2 = 12355;
    private static final int SERVER_PORT3 = 12356;
    private static final int SERVER_PORT4 = 12357;

	private static final String SERVER_ASSOCIATION_NAME = "serverAsscoiation";
	private static final String CLIENT_ASSOCIATION_NAME = "clientAsscoiation";

	private static final String CLIENT_HOST = "127.0.0.1";
    private static final int CLIENT_PORT1 = 12364;
    private static final int CLIENT_PORT2 = 12365;
    private static final int CLIENT_PORT3 = 12366;
    private static final int CLIENT_PORT4 = 12367;

	private final byte[] CLIENT_MESSAGE = "Client says Hi".getBytes();
	private final byte[] SERVER_MESSAGE = "Server says Hi".getBytes();

	private NettySctpManagementImpl management = null;
	private boolean isModified = false;

	private NettyServerImpl server = null;
	

	private NettyAssociationImpl serverAssociation = null;
	private NettyAssociationImpl clientAssociation = null;

	private volatile boolean clientAssocUp = false;
	private volatile boolean serverAssocUp = false;

	private volatile boolean clientAssocDown = false;
	private volatile boolean serverAssocDown = false;

	private byte[] clientMessage;
	private byte[] serverMessage;

	@BeforeAll
	public static void setUpClass() throws Exception {
	}

	@AfterAll
	public static void tearDownClass() throws Exception {
	}

	public void setUp(IpChannelType ipChannelType, int serverPort, int clientPort) throws Exception {
		this.clientAssocUp = false;
		this.serverAssocUp = false;

		this.clientAssocDown = false;
		this.serverAssocDown = false;

		this.clientMessage = null;
		this.serverMessage = null;

		this.management = new NettySctpManagementImpl("ClientAssociationTest");
		this.management.setSingleThread(true);
		this.management.start();
        this.management.setConnectDelay(1000);
		this.management.removeAllResourses();

		this.server = (NettyServerImpl) this.management.addServer(SERVER_NAME, SERVER_HOST, serverPort, ipChannelType, false, 0, null);
		this.serverAssociation = (NettyAssociationImpl) this.management.addServerAssociation(CLIENT_HOST, clientPort, SERVER_NAME, SERVER_ASSOCIATION_NAME, ipChannelType);
		this.clientAssociation = (NettyAssociationImpl) this.management.addAssociation(CLIENT_HOST, clientPort, SERVER_HOST, serverPort, CLIENT_ASSOCIATION_NAME, ipChannelType, null);
	}

	public void tearDown() throws Exception {
		isModified = false;
		this.management.removeAssociation(CLIENT_ASSOCIATION_NAME);
		this.management.removeAssociation(SERVER_ASSOCIATION_NAME);
		this.management.removeServer(SERVER_NAME);

		this.management.stop();
	}

    @Test
	@Tags({
			@Tag("functional"),
			@Tag("sctp")
	})
    public void testModifyServerAndAssociationSctp() throws Exception {

        if (SctpTransferTest.checkSctpEnabled())
            this.testModifyServerAndAssociation(IpChannelType.SCTP, SERVER_PORT1, CLIENT_PORT1);
    }

    /**
     * Simple test that creates Client and Server Association, exchanges data
     * and brings down association. Finally removes the Associations and Server
     */
    @Test
	@Tags({
			@Tag("functional"),
			@Tag("tcp")
	})
    public void testModifyServerAndAssociationTcp() throws Exception {

        this.testModifyServerAndAssociation(IpChannelType.TCP, SERVER_PORT2, CLIENT_PORT2);
    }

	/**
	 * In this test we modify server port after stop and client association 
	 * 
	 * @throws Exception
	 */
	
	private void testModifyServerAndAssociation(IpChannelType ipChannelType, int serverPort, int clientPort) throws Exception {

		this.setUp(ipChannelType, serverPort, clientPort);

		this.serverAssociation.setAssociationListener(new ServerAssociationListener());
		this.management.startAssociation(SERVER_ASSOCIATION_NAME);

		this.management.startServer(SERVER_NAME);
		
		this.clientAssociation.setAssociationListener(new ClientAssociationListenerImpl());
		this.management.startAssociation(CLIENT_ASSOCIATION_NAME);

		Thread.sleep(1000 * 3);

		assertTrue(clientAssocUp);
		assertTrue(serverAssocUp);
		
		//modify server port and association
		this.management.stopAssociation(SERVER_ASSOCIATION_NAME);
		this.management.stopServer(SERVER_NAME);
		Thread.sleep(1000 * 2);
		this.management.modifyServer(SERVER_NAME, null, 2344, null, null, null, null);
		this.management.startAssociation(SERVER_ASSOCIATION_NAME);
		this.management.startServer(SERVER_NAME);
		
		this.management.modifyAssociation(null, null, null, 2344, CLIENT_ASSOCIATION_NAME, null, null);

		isModified = true;

		Thread.sleep(1000 * 3);

		assertTrue(clientAssocUp);
		assertTrue(serverAssocUp);
	
		this.management.stopAssociation(CLIENT_ASSOCIATION_NAME);

		Thread.sleep(1000);

		this.management.stopAssociation(SERVER_ASSOCIATION_NAME);
		this.management.stopServer(SERVER_NAME);
		
		Thread.sleep(1000 * 2);

		assertTrue(Arrays.equals(SERVER_MESSAGE, clientMessage));
		assertTrue(Arrays.equals(CLIENT_MESSAGE, serverMessage));

		assertTrue(clientAssocDown);
		assertTrue(serverAssocDown);

		this.tearDown();
	}

    @Test
	@Tags({
			@Tag("functional"),
			@Tag("sctp")
	})
    public void testModifyServerAndClientAssociationsSctp() throws Exception {

        if (SctpTransferTest.checkSctpEnabled())
            this.testModifyServerAndClientAssociations(IpChannelType.SCTP, SERVER_PORT3, CLIENT_PORT3);
    }

    /**
     * Simple test that creates Client and Server Association, exchanges data
     * and brings down association. Finally removes the Associations and Server
     */
    @Test
	@Tags({
			@Tag("functional"),
			@Tag("tcp")
	})
    public void testModifyServerAndClientAssociationsTcp() throws Exception {

        this.testModifyServerAndClientAssociations(IpChannelType.TCP, SERVER_PORT4, CLIENT_PORT4);
    }

	/**
	 * In this test we modify port in server association and port of client 
	 * 
	 * @throws Exception
	 */
	
	private void testModifyServerAndClientAssociations(IpChannelType ipChannelType, int serverPort, int clientPort) throws Exception {

		this.setUp(ipChannelType, serverPort, clientPort);

		this.serverAssociation.setAssociationListener(new ServerAssociationListener());
		this.management.startAssociation(SERVER_ASSOCIATION_NAME);

		this.management.startServer(SERVER_NAME);
		
		this.clientAssociation.setAssociationListener(new ClientAssociationListenerImpl());
		this.management.startAssociation(CLIENT_ASSOCIATION_NAME);

		Thread.sleep(1000 * 2);

		assertTrue(clientAssocUp);
		assertTrue(serverAssocUp);
		
		this.management.modifyServerAssociation(SERVER_ASSOCIATION_NAME, null, 2347, null, null);
		this.management.modifyAssociation(null, 2347, null, null, CLIENT_ASSOCIATION_NAME, null, null);
		
		isModified = true;

		Thread.sleep(1000 * 2);

		assertTrue(clientAssocUp);
		assertTrue(serverAssocUp);
	
		this.management.stopAssociation(CLIENT_ASSOCIATION_NAME);

		Thread.sleep(1000);

		this.management.stopAssociation(SERVER_ASSOCIATION_NAME);
		this.management.stopServer(SERVER_NAME);
		
		Thread.sleep(1000 * 2);

		assertTrue(Arrays.equals(SERVER_MESSAGE, clientMessage));
		assertTrue(Arrays.equals(CLIENT_MESSAGE, serverMessage));

		assertTrue(clientAssocDown);
		assertTrue(serverAssocDown);

		this.tearDown();
	}

	private class ClientAssociationListenerImpl implements AssociationListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationUp
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationUp(Association association, int maxInboundStreams, int maxOutboundStreams) {
			System.out.println(this + " onCommunicationUp");

			clientAssocUp = true;

			PayloadData payloadData = new PayloadData(CLIENT_MESSAGE.length, Unpooled.copiedBuffer(CLIENT_MESSAGE), true, false, 3, 1);

			try {
				if(isModified)
					association.send(payloadData);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationShutdown
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationShutdown(Association association) {
			System.out.println(this + " onCommunicationShutdown");
			clientAssocDown = true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationLost
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationLost(Association association) {
			System.out.println(this + " onCommunicationLost");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationRestart
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationRestart(Association association) {
			System.out.println(this + " onCommunicationRestart");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onPayload(org.mobicents
		 * .protocols.sctp.Association,
		 * org.mobicents.protocols.sctp.PayloadData)
		 */
		@Override
		public void onPayload(Association association, PayloadData payloadData) {
			System.out.println(this + " onPayload");

			clientMessage = new byte[payloadData.getDataLength()];
			payloadData.getByteBuf().readBytes(clientMessage);

			System.out.println(this + "received " + new String(clientMessage));

		}

		/* (non-Javadoc)
		 * @see org.mobicents.protocols.api.AssociationListener#inValidStreamId(org.mobicents.protocols.api.PayloadData)
		 */
		@Override
		public void inValidStreamId(PayloadData payloadData) {
			// TODO Auto-generated method stub
			
		}

	}

	private class ServerAssociationListener implements AssociationListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationUp
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationUp(Association association, int maxInboundStreams, int maxOutboundStreams) {
			System.out.println(this + " onCommunicationUp");

			serverAssocUp = true;

			PayloadData payloadData = new PayloadData(SERVER_MESSAGE.length, Unpooled.copiedBuffer(SERVER_MESSAGE), true, false, 3, 1);

			try {
				if(isModified)
					association.send(payloadData);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationShutdown
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationShutdown(Association association) {
			System.out.println(this + " onCommunicationShutdown");
			serverAssocDown = true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationLost
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationLost(Association association) {
			System.out.println(this + " onCommunicationLost");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationRestart
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationRestart(Association association) {
			System.out.println(this + " onCommunicationRestart");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onPayload(org.mobicents
		 * .protocols.sctp.Association,
		 * org.mobicents.protocols.sctp.PayloadData)
		 */
		@Override
		public void onPayload(Association association, PayloadData payloadData) {
			System.out.println(this + " onPayload");

			serverMessage = new byte[payloadData.getDataLength()];
			payloadData.getByteBuf().readBytes(serverMessage);

			System.out.println(this + "received " + new String(serverMessage));
		}

		/* (non-Javadoc)
		 * @see org.mobicents.protocols.api.AssociationListener#inValidStreamId(org.mobicents.protocols.api.PayloadData)
		 */
		@Override
		public void inValidStreamId(PayloadData payloadData) {
			// TODO Auto-generated method stub
			
		}

	}
}
