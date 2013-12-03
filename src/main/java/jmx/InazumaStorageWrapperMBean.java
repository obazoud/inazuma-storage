package jmx;

public interface InazumaStorageWrapperMBean
{
	public void insertSingleDocument();
    public void insertThousandDocuments();
    public void insertMultipleDocuments(int count);

    public String returnRandomKeys();
    public String returnKeys(int userID);

    public String returnData(String key);
}
