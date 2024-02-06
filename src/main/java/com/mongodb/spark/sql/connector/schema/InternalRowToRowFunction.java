/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.mongodb.spark.sql.connector.schema;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.analysis.SimpleAnalyzer$;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.catalyst.expressions.Attribute;
import org.apache.spark.sql.types.StructType;
import scala.collection.JavaConverters;
import scala.collection.Seq;

/**
 * An InternalRow to Row function that uses a resolved and bound encoder for the given schema.
 *
 * <p>A concrete {@code Function} implementation that is {@code Serializable}, so it can be
 * serialized and sent to executors.
 */
final class InternalRowToRowFunction implements Function<InternalRow, Row>, Serializable {
  private static final long serialVersionUID = 1L;

  private final ExpressionEncoder.Deserializer<Row> deserializer;

  InternalRowToRowFunction(final StructType schema) {
    List<Attribute> attributesList = Arrays.stream(schema.fields())
        .map(new StructFieldToAttributeFunction())
        .collect(Collectors.toList());
    Seq<Attribute> attributeSeq = JavaConverters.asScalaBuffer(attributesList).toSeq();
    ExpressionEncoder<Row> rowEncoder = ExpressionEncoder.apply(schema);
    this.deserializer =
        rowEncoder.resolveAndBind(attributeSeq, SimpleAnalyzer$.MODULE$).createDeserializer();
  }

  @Override
  public Row apply(final InternalRow internalRow) {
    return deserializer.apply(internalRow);
  }
}
