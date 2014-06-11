package jmx;

@SuppressWarnings("unused")
interface InazumaStorageWrapperMBean
{
	public void insertSingleDocumentForUser(int userID);

	public void insertSingleDocument();

	public void insertThousandDocuments();

	public void insertMultipleDocuments(final int count);

	public String returnRandomKeys();

	public String returnKeys(final String userID);

	public String returnData(final String key);
}
