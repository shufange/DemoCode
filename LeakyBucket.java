package library;

import java.util.concurrent.locks.ReentrantLock;

public class LeakyBucket
{
	private long ratePerSec_ = 10;
	private ReentrantLock lock_ = new ReentrantLock();
	private long lastUpdate_ = 0L;
	public long allowSize_;
	
	public LeakyBucket(long ratePerSec)
	{
		ratePerSec_ = ratePerSec;
		allowSize_ = ratePerSec_;
		lastUpdate_ = System.currentTimeMillis();
	}
	
	public boolean allow(long dataSize)
	{
		boolean ret = false;
		try
		{
			lock_.lock();
			long curTime = System.currentTimeMillis();
			if (curTime - lastUpdate_ >= 1000)
			{
				allowSize_ = ratePerSec_;
				lastUpdate_ = curTime;
			}
			ret = allowSize_ > dataSize;
			if (ret)
				allowSize_ -= dataSize;
		}
		finally
		{
			lock_.unlock();
		}
		return ret;
	}
}
