package jmx;

@SuppressWarnings("unused")
public interface InazumaStorageWrapperMBean
{
	public void insertSingleDocumentForUser(int userID);

	public void insertSingleDocument();

	public void insertThousandDocuments();

	public void insertMultipleDocuments(final int count);

	public String returnRandomDocumentMetadata();

	public String returnDocumentMetadata(final String userID);

	public String returnData(final String key);
}
