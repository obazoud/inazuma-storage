package serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.DocumentMetadata;

import java.lang.reflect.Type;
import java.util.Map;

public class GsonController
{
	private static final Type typeOfMap;

	private final Gson gson;

	static
	{
		typeOfMap = new TypeToken<Map<String, DocumentMetadata>>()
		{
		}.getType();
	}

	public GsonController(final String userID)
	{
		final GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(DocumentMetadata.class, new DocumentMetadataAdapter(userID));

		this.gson = builder.create();
	}

	public Map<String, DocumentMetadata> getDocumentMetadataMap(final String json)
	{
		return gson.fromJson(json, typeOfMap);
	}

	public String toJson(final Map<String, DocumentMetadata> documentMetadataMap)
	{
		return gson.toJson(documentMetadataMap, typeOfMap);
	}
}
