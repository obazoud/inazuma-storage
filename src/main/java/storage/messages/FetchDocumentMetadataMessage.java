package storage.messages;

import storage.callbacks.FetchDocumentMetadataCallback;

public class FetchDocumentMetadataMessage extends UserIDBaseMessage
{
	private final FetchDocumentMetadataCallback callback;

	public FetchDocumentMetadataMessage(final String userID, final FetchDocumentMetadataCallback callback)
	{
		super(userID);
		this.callback = callback;
	}

	public FetchDocumentMetadataCallback getCallback()
	{
		return callback;
	}
}
