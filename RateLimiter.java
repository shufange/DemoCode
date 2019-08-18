package library;

public interface RateLimiter {
	// If or not allow ingested amount of data to go through
	public boolean ingest(long dataSize);
}
