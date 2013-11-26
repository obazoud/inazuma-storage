package model;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class UserLookupDocument implements StatusMessageObject
{
	private final static Type typeOfMap = new TypeToken<ConcurrentHashMap<String, Long>>() {}.getType();
	
	private final ConcurrentMap<String, Long> lookup;
	
	private int tries = 0;
	private Exception lastException = null;

	public UserLookupDocument()
	{
		lookup = new ConcurrentHashMap<String, Long>();
	}
	
	public UserLookupDocument(final String json)
	{
		lookup = new Gson().fromJson(json, typeOfMap);
	}

	public boolean add(final long created, final String key)
	{
        return lookup.putIfAbsent(key, created) == null;
    }
	
	public void remove(final String key)
	{
		lookup.remove(key);
	}
	
	public int size()
	{
		return lookup.size();
	}

	@Override
	public String toString()
	{
		return toJSON();
	}

	public String toJSON()
	{
		Gson gson = new Gson();
		return gson.toJson(lookup);
	}

	public static UserLookupDocument fromJSON(final String value)
	{
		return new UserLookupDocument(value);
	}
	
	@Override
	public int getTries()
	{
		return tries;
	}

	@Override
	public void incrementTries()
	{
		this.tries++;
	}

	@Override
	public Exception getLastException()
	{
		return lastException;
	}

	@Override
	public void setLastException(Exception lastException)
	{
		this.lastException = lastException;
	}

	@Override
	public void resetStatus()
	{
		tries = 0;
		lastException = null;
	}
}
