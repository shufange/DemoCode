package library;

import java.util.concurrent.locks.ReentrantLock;

import src.RateLimitTest;

public class TokenBucket {

	private long ratePerSec_ = 10;
	private long capactiy_;
	private long lastUpdate_ = 0L;
	private ReentrantLock lock_ = new ReentrantLock();
	private long availableTokens_;
	
	public TokenBucket(long ratePerSec)
	{
		ratePerSec_ = ratePerSec;
		capactiy_ = ratePerSec_;
		availableTokens_ = capactiy_;
		lastUpdate_ = System.currentTimeMillis();
	}
	
	public boolean getTokens(long tokenNum)
	{
		try
		{
			lock_.lock();
			refill();
			if (availableTokens_ > tokenNum)
			{
				availableTokens_ -= tokenNum;
				return true;
			}
			else
				return false;
		}
		finally
		{
			lock_.unlock();
		}	
	}
	
	// Assume caller got the lock already
	public void refill()
	{
		if (availableTokens_ >= capactiy_)
			return;
		long curTime = System.currentTimeMillis();
		long toFill = (long)((( curTime - lastUpdate_ ) / (double)1000) * ratePerSec_);
		if (availableTokens_ + toFill >= capactiy_)
			availableTokens_ = capactiy_;
		else
			availableTokens_ += toFill;
		lastUpdate_ = curTime;
	}
}


