package jmx;

import main.Main;
import model.SerializedData;

import java.util.Random;
import java.util.UUID;

public class InazumaStorageWrapper implements InazumaStorageWrapperMBean
{
    private static final Random generator = new Random();

    @Override
    public void insertSingleDocument()
    {
        final SerializedData serializedData = createRandomSerializedData();
        Main.getStorageController().addData(serializedData);
    }

    @Override
    public void insertThousandDocuments()
    {
        insertMultipleDocuments(1000);
    }

    @Override
    public void insertMultipleDocuments(int count)
    {
        while (count-- > 0)
        {
            insertSingleDocument();
        }
    }

    @Override
    public String returnRandomKeys()
    {
        return returnKeys(createRandomUserID());
    }

    @Override
    public String returnKeys(final int userID)
    {
        return Main.getStorageController().getKeys(userID);
    }

    @Override
    public String returnData(final String key)
    {
        return Main.getStorageController().getData(key);
    }

    private int createRandomUserID()
    {
        return generator.nextInt(10) + 1;
    }

    private SerializedData createRandomSerializedData()
    {
        final int userID = createRandomUserID();
        final long created = (System.currentTimeMillis() / 1000) - generator.nextInt(86400);
        final String json = "{\"userID\":" + userID + ",\"created\":" + created + "}";
        return new SerializedData(userID, created, UUID.randomUUID().toString(), json);
    }
}
