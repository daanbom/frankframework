/*
   Copyright 2021, 2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.mongodb;

import org.bson.AbstractBsonWriter;
import org.bson.BsonBinary;
import org.bson.BsonContextType;
import org.bson.BsonDbPointer;
import org.bson.BsonRegularExpression;
import org.bson.BsonTimestamp;
import org.bson.json.Converter;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.json.StrictCharacterStreamJsonWriterSettings;
import org.bson.json.StrictJsonWriter;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import org.frankframework.documentbuilder.INodeBuilder;

/**
 * BSON writer to write to FF DocumentBuilder, to generate JSON or XML.
 * Based on org.bson.json.JsonWriter, called in Document.toJson();
 *
 * @author Gerrit van Brakel
 */
public class JsonDocumentWriter extends AbstractBsonWriter {
	private final JsonWriterSettings settings;
	private final StrictJsonDocumentWriter strictJsonWriter;

	/**
	 * Creates a new instance which uses {@code writer} to write JSON to.
	 *
	 * @param nodeBuilder the writer to write JSON to.
	 */
	public JsonDocumentWriter(final INodeBuilder nodeBuilder) {
		this(nodeBuilder, JsonWriterSettings.builder().build());
	}

	/**
	 * Creates a new instance which uses {@code writer} to write JSON to and uses
	 * the given settings.
	 *
	 * @param nodeBuilder   the handler to write JSON to.
	 * @param settings the settings to apply to this writer.
	 */
	public JsonDocumentWriter(final INodeBuilder nodeBuilder, final JsonWriterSettings settings) {
		super(settings);
		this.settings = settings;
		setContext(new Context(null, BsonContextType.TOP_LEVEL));
		strictJsonWriter = new StrictJsonDocumentWriter(nodeBuilder,
				StrictCharacterStreamJsonWriterSettings.builder().indent(settings.isIndent())
						.newLineCharacters(settings.getNewLineCharacters())
						.indentCharacters(settings.getIndentCharacters()).maxLength(settings.getMaxLength()).build());
	}


	@Override
	protected Context getContext() {
		return (Context) super.getContext();
	}

	@Override
	protected void doWriteName(final String name) {
		strictJsonWriter.writeName(name);
	}

	@Override
	protected void doWriteStartDocument() {
		strictJsonWriter.writeStartObject();

		BsonContextType contextType = getState() == State.SCOPE_DOCUMENT ? BsonContextType.SCOPE_DOCUMENT
				: BsonContextType.DOCUMENT;
		setContext(new Context(getContext(), contextType));
	}

	@Override
	protected void doWriteEndDocument() {
		strictJsonWriter.writeEndObject();
		if (getContext().getContextType() == BsonContextType.SCOPE_DOCUMENT) {
			setContext(getContext().getParentContext());
			writeEndDocument();
		} else {
			setContext(getContext().getParentContext());
		}
	}

	@Override
	protected void doWriteStartArray() {
		strictJsonWriter.writeStartArray();
		setContext(new Context(getContext(), BsonContextType.ARRAY));
	}

	@Override
	protected void doWriteEndArray() {
		strictJsonWriter.writeEndArray();
		setContext(getContext().getParentContext());
	}

	@Override
	protected void doWriteBinaryData(final BsonBinary binary) {
		settings.getBinaryConverter().convert(binary, strictJsonWriter);
	}

	@Override
	public void doWriteBoolean(final boolean value) {
		settings.getBooleanConverter().convert(value, strictJsonWriter);
	}

	@Override
	protected void doWriteDateTime(final long value) {
		settings.getDateTimeConverter().convert(value, strictJsonWriter);
	}

	@Override
	protected void doWriteDBPointer(final BsonDbPointer value) {
		if (settings.getOutputMode() == JsonMode.EXTENDED) {
			new Converter<BsonDbPointer>() {
				@Override
				public void convert(final BsonDbPointer value1, final StrictJsonWriter writer) {
					writer.writeStartObject();
					writer.writeStartObject("$dbPointer");
					writer.writeString("$ref", value1.getNamespace());
					writer.writeName("$id");
					doWriteObjectId(value1.getId());
					writer.writeEndObject();
					writer.writeEndObject();
				}
			}.convert(value, strictJsonWriter);
		} else {
			new Converter<BsonDbPointer>() {
				@Override
				public void convert(final BsonDbPointer value1, final StrictJsonWriter writer) {
					writer.writeStartObject();
					writer.writeString("$ref", value1.getNamespace());
					writer.writeName("$id");
					doWriteObjectId(value1.getId());
					writer.writeEndObject();
				}
			}.convert(value, strictJsonWriter);
		}
	}

	@Override
	protected void doWriteDouble(final double value) {
		settings.getDoubleConverter().convert(value, strictJsonWriter);
	}

	@Override
	protected void doWriteInt32(final int value) {
		settings.getInt32Converter().convert(value, strictJsonWriter);
	}

	@Override
	protected void doWriteInt64(final long value) {
		settings.getInt64Converter().convert(value, strictJsonWriter);
	}

	@Override
	protected void doWriteDecimal128(final Decimal128 value) {
		settings.getDecimal128Converter().convert(value, strictJsonWriter);
	}

	@Override
	protected void doWriteJavaScript(final String code) {
		settings.getJavaScriptConverter().convert(code, strictJsonWriter);
	}

	@Override
	protected void doWriteJavaScriptWithScope(final String code) {
		writeStartDocument();
		writeString("$code", code);
		writeName("$scope");
	}

	@Override
	protected void doWriteMaxKey() {
		settings.getMaxKeyConverter().convert(null, strictJsonWriter);
	}

	@Override
	protected void doWriteMinKey() {
		settings.getMinKeyConverter().convert(null, strictJsonWriter);
	}

	@Override
	public void doWriteNull() {
		settings.getNullConverter().convert(null, strictJsonWriter);
	}

	@Override
	public void doWriteObjectId(final ObjectId objectId) {
		settings.getObjectIdConverter().convert(objectId, strictJsonWriter);
	}

	@Override
	public void doWriteRegularExpression(final BsonRegularExpression regularExpression) {
		settings.getRegularExpressionConverter().convert(regularExpression, strictJsonWriter);
	}

	@Override
	public void doWriteString(final String value) {
		settings.getStringConverter().convert(value, strictJsonWriter);
	}

	@Override
	public void doWriteSymbol(final String value) {
		settings.getSymbolConverter().convert(value, strictJsonWriter);
	}

	@Override
	public void doWriteTimestamp(final BsonTimestamp value) {
		settings.getTimestampConverter().convert(value, strictJsonWriter);
	}

	@Override
	public void doWriteUndefined() {
		settings.getUndefinedConverter().convert(null, strictJsonWriter);
	}

	@Override
	public void flush() {
		// strictJsonWriter.flush();
	}

	/**
	 * Return true if the output has been truncated due to exceeding the length
	 * specified in {@link JsonWriterSettings#getMaxLength()}.
	 *
	 * @return true if the output has been truncated
	 * @since 3.7
	 * @see JsonWriterSettings#getMaxLength()
	 */
	public boolean isTruncated() {
		return strictJsonWriter.isTruncated();
	}

	@Override
	protected boolean abortPipe() {
		return strictJsonWriter.isTruncated();
	}

	/**
	 * The context for the writer, inheriting all the values from
	 * {@link org.bson.AbstractBsonWriter.Context}, and additionally providing
	 * settings for the indentation level and whether there are any child elements
	 * at this level.
	 */
	public class Context extends AbstractBsonWriter.Context {

		/**
		 * Creates a new context.
		 *
		 * @param parentContext the parent context that can be used for going back up to
		 *                      the parent level
		 * @param contextType   the type of this context
		 */
		public Context(final Context parentContext, final BsonContextType contextType) {
			super(parentContext, contextType);
		}

		@Override
		public Context getParentContext() {
			return (Context) super.getParentContext();
		}
	}
}
