package storage.messages;

import storage.callbacks.FetchDocumentCallback;

public class FetchDocumentMessage extends UserIDBaseMessage
{
	private final String key;
	private final FetchDocumentCallback callback;

	public FetchDocumentMessage(final String userID, final String key, final FetchDocumentCallback callback)
	{
		super(userID);
		this.key = key;
		this.callback = callback;
	}

	public String getKey()
	{
		return key;
	}

	public FetchDocumentCallback getCallback()
	{
		return callback;
	}
}
