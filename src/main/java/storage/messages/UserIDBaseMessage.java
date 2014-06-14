package storage.messages;

import java.io.Serializable;

public abstract class UserIDBaseMessage implements Serializable
{
	private final String userID;

	public UserIDBaseMessage(final String userID)
	{
		this.userID = userID;
	}

	public String getUserID()
	{
		return userID;
	}
}
