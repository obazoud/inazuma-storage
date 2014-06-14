package jmx;

@SuppressWarnings("unused")
public interface InazumaStorageWrapperMBean
{
	public void insertSingleDocumentForUser(int userID);

	public void insertSingleDocument();

	public void insertThousandDocuments();

	public void insertMultipleDocuments(int count);

	public String returnRandomDocumentMetadata();

	public String returnDocumentMetadata(String userID);

	public String returnDocument(String userID, String key);

	void deleteDocument(String userID, String key);
}
