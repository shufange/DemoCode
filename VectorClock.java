import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;


public class VectorClock implements Runnable{
	private static VectorClock instance_ = null;
	private static ReentrantLock instanceLock_ = new ReentrantLock();
	private static int[] portList_ = new int[]{8000,8001,8002};
	private static Random random_;
	/* Constants */
	public static int portNum_ = 0;
	public static int pid_ = 0;
	public static int peerNum_ = 0;
	/* Instance vars */
	private static Map<Integer, Long> peersClock_ = null;
	ServerSocket listener_;

	public static VectorClock getInstance()
	{
		if (instance_ == null)
		{
			try
			{
				instanceLock_.lock();
				if (instance_ == null)
				{
					instance_ = new VectorClock();
				}
			}
			finally
			{
				instanceLock_.unlock();
			}
		}
		return instance_;
	}

	public int init()
	{
		try {
			listener_ = new ServerSocket(portNum_);
			peersClock_ = new HashMap();
			peersClock_.put(pid_, 0L);
			random_ = new Random(pid_);
			return 0;
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public void receive() throws IOException
	{
		try
		{
			while (true)
			{
				Socket socket = listener_.accept();
				try
				{
					DataInputStream dis = new DataInputStream(socket.getInputStream());
					Message msg = Message.deserialize(dis);
					peersClock_.put(pid_, peersClock_.get(pid_) + 1);
					System.out.println("msg.peersClock_ : " + printVectorClock(msg.peersClock_));
					System.out.println("my peersClock : " + printVectorClock(peersClock_));
					for (int k : msg.peersClock_.keySet())
					{
						long myClock = peersClock_.get(k) == null ? 0 : peersClock_.get(k);
						long msgClock = msg.peersClock_.get(k);
						long newClock = Math.max(msgClock, myClock);
						peersClock_.put(k, newClock);
						System.out.println("[" + pid_ + "] updated on receive : " + printVectorClock(peersClock_));
					}
				}
				finally
				{
					socket.close();
				}
			}
		}
		finally
		{
			listener_.close();
		}
	}

	public void send()
	{
		peersClock_.put(pid_, peersClock_.get(pid_) + 1);
		Message msg = new Message();
		msg.peersClock_ = peersClock_;
		System.out.println("Before sending my peersClock_:" + printVectorClock(peersClock_));
		int[] otherPorts = new int[2];
		int i = 0;
		for (int port : portList_)
		{
			if (port != portNum_)
				otherPorts[i++] = port;
		}
		Socket sock = null;
		try
		{
			int portIdx = Math.abs(random_.nextInt()) % 2;
			System.out.println("connecting to port:" + otherPorts[portIdx]);
			sock = new Socket("127.0.0.1", otherPorts[portIdx]);
			DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
			Message.serialize(msg, dos);
			dos.flush();
			System.out.println("[" + pid_ + "]" + " Send event happened : " + printVectorClock(peersClock_));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (sock != null)
					sock.close();
			} catch (IOException ex) {
				System.out.println("Couldn't close socket.");
			}
		}
	}

	public void localEvent()
	{
		peersClock_.put(pid_, peersClock_.get(pid_) + 1);
		System.out.println("[" + pid_ + "] Local event happened : " + printVectorClock(peersClock_));
	}
	
	public static String printVectorClock(Map<Integer, Long> vc)
	{
		StringBuilder sb = new StringBuilder();
		TreeSet<Integer> treeSet = new TreeSet();
		treeSet.addAll(vc.keySet());
		for (int k : treeSet)
		{
			sb.append(k + " : " + vc.get(k) + "\t");
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		if (args.length < 1)
		{
			System.out.println("Usage: ./VectorClock <port_num>");
			System.exit(-1);
		}
		int portNum = Integer.parseInt(args[0]);
		VectorClock.portNum_ = portNum;
		VectorClock.pid_ = Integer.parseInt( ManagementFactory.getRuntimeMXBean().getName().split("\\@")[0] );
		
		System.out.println(VectorClock.portNum_ + "," + VectorClock.pid_);
		VectorClock vc = VectorClock.getInstance();
		vc.init();
		Thread t = new Thread(vc);
		try {
			t.start();
			Thread.sleep(5000);
			vc.send();
			vc.localEvent();
			t.join();
			System.out.println("[" + pid_ + "] Final vc:" + printVectorClock(peersClock_));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run() {
		try {
			System.out.println("[" + pid_ + "] Start running receiving on port:" + portNum_);
			receive();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class ApplicationTask implements Runnable
	{
		public void run() {
			
		}
	}
	
}



class Message
{
	public Map<Integer, Long> peersClock_;
	public Message() {}
	
	public static void serialize(Message msg, DataOutputStream dos) throws IOException
	{
		dos.writeInt(msg.peersClock_.size());
		for (Map.Entry<Integer, Long> entry : msg.peersClock_.entrySet())
		{
			dos.writeInt(entry.getKey());
			dos.writeLong(entry.getValue());
		}
	}
	
	public static Message deserialize(DataInputStream dis) throws IOException
	{
		Message msg = new Message();
		int len = dis.readInt();
		msg.peersClock_ = new HashMap();
		for (int i=0; i<len; i++)
		{
			msg.peersClock_.put(dis.readInt(), dis.readLong()) ;
		}
		return msg;
	}
}
