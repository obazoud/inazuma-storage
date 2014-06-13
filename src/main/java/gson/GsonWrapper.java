package gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.DocumentMetadata;

import java.lang.reflect.Type;
import java.util.Collection;

public class GsonWrapper
{
	private static final Type typeOfCollection;

	private final Gson gson;

	static
	{
		TypeToken<Collection<DocumentMetadata>> typeToken = new TypeToken<Collection<DocumentMetadata>>()
		{
		};
		typeOfCollection = typeToken.getType();
	}

	public GsonWrapper(final String userID)
	{
		final GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(DocumentMetadata.class, new DocumentMetadataAdapter(userID));

		this.gson = builder.create();
	}

	public Collection<DocumentMetadata> getDocumentMetadataCollection(final String json)
	{
		return gson.fromJson(json, typeOfCollection);
	}

	public String toJson(final Collection<DocumentMetadata> documentMetadataCollection)
	{
		return gson.toJson(documentMetadataCollection, typeOfCollection);
	}
}
