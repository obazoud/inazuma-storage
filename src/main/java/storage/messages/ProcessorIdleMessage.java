package storage.messages;

public class ProcessorIdleMessage
{
	private final String userID;

	public ProcessorIdleMessage(final String userID)
	{
		this.userID = userID;
	}

	public String getUserID()
	{
		return userID;
	}
}
