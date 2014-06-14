package storage.messages;

public abstract class UserIDMessage
{
	private final String userID;

	public UserIDMessage(final String userID)
	{
		this.userID = userID;
	}

	public String getUserID()
	{
		return userID;
	}
}
