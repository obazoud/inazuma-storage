package storage.messages;

public class PersistLookupDocumentMessage
{
	private final String userID;

	public PersistLookupDocumentMessage(final String userID)
	{
		this.userID = userID;
	}

	public String getUserID()
	{
		return userID;
	}
}
