package com.mobico.resmpprest.smpp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.mobico.resmpprest.smpp.pdu.BasePDU;

public class SmppClient {
	private final long DEFAULT_READ_TIMEOUT = 60000L;
	private final int DEFAULT_READ_BUFFER_SIZE = 4096;
	private final int MAX_PENDING_BUFFER_SIZE = 64*1024;

	private BuilderImpl conf;
	private AsynchronousSocketChannel channel;
	private ConcurrentLinkedQueue<BasePDU> output_queue=new ConcurrentLinkedQueue();
	private ConcurrentLinkedQueue<BasePDU> input_queue=new ConcurrentLinkedQueue();
	
	private  SmppClient() {
	}
	
	protected  SmppClient(BuilderImpl conf) {
		this.conf=conf;
	}
	
	public interface Builder  {
		public Builder  bindIp(String ip);
		public Builder  host(String host);
		public Builder  port(int port);
		public Builder  username(String name);
		public Builder  password(String pswd);
		public Builder  systype(String type);
		public Builder  timeout(int timeout);
		public Builder  maxmps(int mps);
		public SmppClient newClient();
	}
	
	public static Builder builder() {
		return new BuilderImpl(); 
	}
	
	public boolean connect() {
		try {
		    channel = AsynchronousSocketChannel.open();
			channel.connect(new InetSocketAddress(conf.host,conf.port)).get(conf.time,TimeUnit.MILLISECONDS);
			SocketAddress  raddr=channel.getRemoteAddress();
			if (raddr==null)
				return false;
			return channel.isOpen()?true:false;
		}catch(IOException  
				| InterruptedException 
				| ExecutionException 
				| TimeoutException e) {
			e.printStackTrace();
		} 
		return false;
	}
	
	public void close() {
		try  {
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void try_pdu(ByteBuffer buffer) {
		input_queue.add(new BasePDU());
	}
	
	public void processing() {
		final ByteBuffer pending =  ByteBuffer.allocate(MAX_PENDING_BUFFER_SIZE);
		final ByteBuffer buff = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
		ExecutorService read_executor = Executors.newSingleThreadExecutor();
		read_executor.execute(() -> {
			while(channel.isOpen()) {
				if (pending.position()>0) {
					try_pdu(pending);
				}
				channel.read( buff,DEFAULT_READ_TIMEOUT,
						TimeUnit.MILLISECONDS, channel, 
						new CompletionHandler<Integer, AsynchronousSocketChannel>(){
				            @Override
				            public void completed(Integer result, AsynchronousSocketChannel channel) {   
				            	if (result<=0) {
				            		return;
				            	}
				            	buff.flip();
				            	try_pdu(buff);
				            	if (buff.hasRemaining()) {
				            		byte[] bytes = new byte[buff.remaining()];
				            		buff.get(bytes, 0, bytes.length);
				            	    pending.put(bytes);
				            	}
				            }
				            @Override
				            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
				            	exc.printStackTrace();
	                             try {
	                            	 channel.close();
	                             } catch (IOException e) {
	                                 e.printStackTrace();
	                             }
				            }
					});
		    }
		});
		
		ScheduledExecutorService write_executor = Executors.newScheduledThreadPool(1);

		Runnable write_task = () -> {
			BasePDU pdu=output_queue.poll();
			if (pdu!=null) {
				channel.write(pdu.getBytes(), channel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
                     @Override
                     public void completed(Integer result, AsynchronousSocketChannel channel) {

                     }
                     @Override
                     public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                         exc.printStackTrace();
                         try {
                        	 channel.close();
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                     }
                 });
			}
		};

		int period = 1;
		write_executor.scheduleWithFixedDelay(write_task, 0, 1000/conf.mps, TimeUnit.MILLISECONDS);
		
	}
	
	public void send(BasePDU pdu) {
		output_queue.add(pdu);
	}
	
	public BasePDU get_next_pdu() {
		return input_queue.poll();
	}
	
	public BasePDU get_next_pdu(int timeout_ms) {
		BasePDU res=null;
		long start=System.currentTimeMillis();
		while((res=input_queue.poll())==null) {
			try {  
				Thread.sleep(1);
			}catch(InterruptedException e) {
				e.printStackTrace();
			}
			if (System.currentTimeMillis()>=start+timeout_ms)
				break;
		}	
		return res;
	}

}
