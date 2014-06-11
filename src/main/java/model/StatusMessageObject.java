package model;

@SuppressWarnings("unused")
interface StatusMessageObject
{
	public int getTries();

	public void incrementTries();

	public Exception getLastException();

	public void setLastException(Exception lastException);

	public void resetStatus();
}
