package com.mobico.resmpprest.smpp.pdu;

import java.nio.ByteBuffer;

public class BasePDU {
	
	 private int commandLength = 0;
	 private int commandId = 0;
	 private int commandStatus = 0;
	 private int sequenceNumber = 0;
	
	public BasePDU() {
		
	}
	
	public ByteBuffer getBytes() {
		return null;
	}

}
