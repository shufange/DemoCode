package src;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import library.LeakyBucket;
import library.RateLimiter;
import library.TokenBucket;

public class RateLimitTest {
	public static final long MB_ = 1024 * 1024L;
	public static RateLimiter limiter_;
	public static void main(String[] args) {
		limiter_ = new TokenBucket(450 * MB_);
		try
		{
			Timer timer = new Timer();
			timer.schedule(new Metrics(), 0, 1000);
			ExecutorService es = Executors.newFixedThreadPool(64);
			for (int i=0;i<64;i++)
				es.execute(new Worker(limiter_, 1 * 60 * 1000));
			es.awaitTermination(2, TimeUnit.MINUTES);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		System.out.println("Shut down test.");
	}
}

class Metrics extends TimerTask
{
	public static AtomicLong aggregateThru_ = new AtomicLong(0L);
	public static AtomicLong aggregateTotal_ = new AtomicLong(0L);
	
	@Override
	public void run() {
		long rateThru = aggregateThru_.get();
		long rateTotal = aggregateTotal_.get();
		aggregateThru_.lazySet(0);
		aggregateTotal_.lazySet(0);
		System.out.println("Thru rate:" + rateThru / RateLimitTest.MB_ + "MB/s");
		System.out.println("Total rate:" + rateTotal / RateLimitTest.MB_ + "MB/s\n");
	}
	
	public static void recordThru(long dataSize)
	{
		aggregateThru_.addAndGet(dataSize);
	}
	
	public static void recordTotal(long dataSize)
	{
		aggregateTotal_.addAndGet(dataSize);
	}
	
}


class Worker implements Runnable
{
	private static Random random_ = new Random();
	private RateLimiter limiter_;
	private long startTime_;
	private long runTime_;
	public Worker(RateLimiter limiter, long runTime)
	{
		startTime_ = System.currentTimeMillis();
		limiter_ = limiter;
		runTime_ = runTime;
	}
	
	@Override
	public void run() {
		try {
			System.out.println("thread id:" + Thread.currentThread().getId() + " start.");
			while(System.currentTimeMillis() - startTime_ < runTime_)
			{
				long dataSize = Math.abs(random_.nextLong()) % (100 * RateLimitTest.MB_);

				Thread.sleep(Math.abs(random_.nextInt()) % 1000);
				
				if (limiter_.ingest(dataSize))
					Metrics.recordThru(dataSize);
				Metrics.recordTotal(dataSize);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("thread id:" + Thread.currentThread().getId() + " end.");
	}
	
}
