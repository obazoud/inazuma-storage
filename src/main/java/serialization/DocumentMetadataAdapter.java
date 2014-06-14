package serialization;

import com.google.gson.*;
import model.DocumentMetadata;

import java.lang.reflect.Type;

class DocumentMetadataAdapter implements JsonSerializer<DocumentMetadata>, JsonDeserializer<DocumentMetadata>
{
	private final String userID;

	public DocumentMetadataAdapter(final String userID)
	{
		this.userID = userID;
	}

	@Override
	public JsonElement serialize(final DocumentMetadata documentMetadata, final Type type, final JsonSerializationContext jsonSerializationContext)
	{
		final JsonObject object = new JsonObject();

		object.addProperty("c", documentMetadata.getCreated());
		object.addProperty("r", documentMetadata.isRead() ? 1 : 0);

		return object;
	}

	@Override
	public DocumentMetadata deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext jsonDeserializationContext) throws JsonParseException
	{
		final JsonObject object = jsonElement.getAsJsonObject();
		return new DocumentMetadata(
				userID,
				object.get("c").getAsLong(),
				object.get("r").getAsBoolean()
		);
	}
}
