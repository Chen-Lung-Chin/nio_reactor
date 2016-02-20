package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

public class SocketProcessor implements Runnable {

	private Queue<Socket>  inboundSocketQueue   = null;
	
	private ByteBuffer readByteBuffer  = ByteBuffer.allocate(6);
    
	Stream<String> stream_channel1 = null;
	Stream<String> stream_channel2 = null;
	
	List<String> list_channel1 = null;
	List<String> list_channel2 = null;
	
	private Selector   readSelector    = null;
    Integer channelId = null;
    StringBuilder output_string_builder = null;
    String output = new String();
  //start incoming socket ids from 16K - reserve bottom ids for pre-defined sockets (servers).
    //private long              nextSocketId = 16 * 1024; 

    
	public SocketProcessor(Queue<Socket> socketQueue) {
		// TODO Auto-generated constructor stub
		this.inboundSocketQueue = socketQueue;
		channelId = new Integer(1);
		output_string_builder = new StringBuilder();
		list_channel1 = new ArrayList<String>();
		list_channel2 = new ArrayList<String>();
			    
		try {
			this.readSelector         = Selector.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true){
            try{
                executeCycle();
            } catch(IOException e){
                e.printStackTrace();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
	}
	
	private void executeCycle() throws IOException {
        takeNewSockets();
        int old_len = output.length();
        readFromSockets();
        int new_len = output.length();
        if(new_len > old_len){
        	Runtime.getRuntime().exec("clear");
        	System.out.println(output);
        }
    }
	
	private void takeNewSockets() throws IOException {
        Socket newSocket = this.inboundSocketQueue.poll();

        channelId++;
        while(newSocket != null){
            newSocket.get_socket_channel().configureBlocking(false);

            
            
            SelectionKey key = newSocket.get_socket_channel().register(this.readSelector, SelectionKey.OP_READ);
            key.attach(newSocket);

            newSocket = this.inboundSocketQueue.poll();
        }
    }
	
	public void readFromSockets() throws IOException {
        int readReady = this.readSelector.selectNow();
        
        if(readReady > 0){
            Set<SelectionKey> selectedKeys = this.readSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while(keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                readFromSocket(key);

                keyIterator.remove();
            }
            selectedKeys.clear();
        }
        
        
    }
	
	private void readFromSocket(SelectionKey key) throws IOException {
        Socket socket = (Socket) key.attachment();
        int num_bytes = socket.read(this.readByteBuffer);
        if(num_bytes >0){
        	String out = socket.getOut();
        	List<String> new_data = Arrays.asList(out.split(" "));
        	char channelId = new_data.get(0).charAt(1);
        	if(channelId == '1'){
        		list_channel1.addAll(new_data);
        		
        	}
        	if(channelId == '2'){
        		list_channel2.addAll(new_data);
        		
        		
        	}
        }
        
        if(list_channel1.size() > 0 && list_channel2.size() > 0){
        	
        	generate_output();
        }
        
        
        
        output = output_string_builder.toString();
        
    }

	private void generate_output() {
		
		
		Channel ch1 = new Channel(list_channel1);
		Channel ch2 = new Channel(list_channel2);
		Integer count1 = new Integer(ch1.getQ().size());
		Integer count2 = new Integer(ch2.getQ().size());
		while((ch1.getQ().size()>0 && count1>0) && (ch2.getQ().size()>0 && count2>0)){
			ch1.match(ch2, output_string_builder);
			count1--;
			count2--;
			
			
		}
		
		
		
		
	}

	

	
	
	

}
