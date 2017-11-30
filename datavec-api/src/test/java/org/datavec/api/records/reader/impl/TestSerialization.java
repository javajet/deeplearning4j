/*
 *  * Copyright 2017 Skymind, Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 */

package org.datavec.api.records.reader.impl;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.*;
import org.datavec.api.records.reader.impl.jackson.FieldSelection;
import org.datavec.api.records.reader.impl.jackson.JacksonLineRecordReader;
import org.datavec.api.records.reader.impl.jackson.JacksonRecordReader;
import org.datavec.api.records.reader.impl.misc.LibSvmRecordReader;
import org.datavec.api.records.reader.impl.misc.SVMLightRecordReader;
import org.datavec.api.records.reader.impl.regex.RegexLineRecordReader;
import org.datavec.api.records.reader.impl.regex.RegexSequenceRecordReader;
import org.datavec.api.records.reader.impl.transform.TransformProcessRecordReader;
import org.datavec.api.records.reader.impl.transform.TransformProcessSequenceRecordReader;
import org.datavec.api.transform.MathFunction;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Text;
import org.junit.Test;
import org.nd4j.shade.jackson.core.JsonFactory;
import org.nd4j.shade.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Record readers need to be serializable for spark.
 * Note however that not all are used/usable with spark (such as Collection[Sequence]RecordReader
 * and the rest are generally used without being initialized on a particular dataset
 */
public class TestSerialization {

    @Test
    public void testRR() throws Exception {

        List<RecordReader> rrs = new ArrayList<>();

        rrs.add(new CSVNLinesSequenceRecordReader(10));
        rrs.add(new CSVRecordReader(10, ','));
        rrs.add(new CSVSequenceRecordReader(1, ","));
        rrs.add(new CSVVariableSlidingWindowRecordReader(5));
        rrs.add(new CSVRegexRecordReader(0, ",", null, new String[] {null, "(.+) (.+) (.+)"}));
        rrs.add(new JacksonRecordReader(new FieldSelection.Builder().addField("a").addField(new Text("MISSING_B"), "b")
                .addField(new Text("MISSING_CX"), "c", "x").build(), new ObjectMapper(new JsonFactory())));
        rrs.add(new JacksonLineRecordReader(new FieldSelection.Builder().addField("value1")
        		.addField("value2").build(), new ObjectMapper(new JsonFactory())));
        rrs.add(new LibSvmRecordReader());
        rrs.add(new SVMLightRecordReader());
        rrs.add(new RegexLineRecordReader("(.+) (.+) (.+)", 0));
        rrs.add(new RegexSequenceRecordReader("(.+) (.+) (.+)", 0));
        rrs.add(new TransformProcessRecordReader(new CSVRecordReader(), getTp()));
        rrs.add(new TransformProcessSequenceRecordReader(new CSVSequenceRecordReader(), getTp()));
        rrs.add(new LineRecordReader());

        for(RecordReader r : rrs){
            System.out.println(r.getClass().getName());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(r);
            byte[] bytes = baos.toByteArray();

            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));

            RecordReader r2 = (RecordReader) ois.readObject();
        }
    }

    private static TransformProcess getTp(){
        Schema s = new Schema.Builder().addColumnDouble("d").build();
        TransformProcess tp = new TransformProcess.Builder(s)
                .doubleMathFunction("d", MathFunction.ABS)
                .build();
        return tp;
    }

}
