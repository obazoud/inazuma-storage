package storage.messages;

public class DeleteDocumentMessage extends UserIDBaseMessage
{
	private final String key;

	public DeleteDocumentMessage(final String userID, final String key)
	{
		super(userID);
		this.key = key;
	}

	public String getKey()
	{
		return key;
	}
}
