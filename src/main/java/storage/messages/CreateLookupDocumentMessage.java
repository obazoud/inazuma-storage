package storage.messages;

public class CreateLookupDocumentMessage
{
	private final String userID;

	public CreateLookupDocumentMessage(final String userID)
	{
		this.userID = userID;
	}

	public String getUserID()
	{
		return userID;
	}
}
