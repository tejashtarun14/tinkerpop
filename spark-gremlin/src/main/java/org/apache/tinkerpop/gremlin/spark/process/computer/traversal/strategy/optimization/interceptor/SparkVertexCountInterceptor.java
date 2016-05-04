/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.tinkerpop.gremlin.spark.process.computer.traversal.strategy.optimization.interceptor;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.VertexWritable;
import org.apache.tinkerpop.gremlin.process.computer.traversal.TraversalVertexProgram;
import org.apache.tinkerpop.gremlin.process.traversal.Step;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.util.TraverserSet;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkMemory;
import org.apache.tinkerpop.gremlin.spark.process.computer.traversal.strategy.SparkVertexProgramInterceptor;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class SparkVertexCountInterceptor implements SparkVertexProgramInterceptor<TraversalVertexProgram> {

    public SparkVertexCountInterceptor() {

    }

    @Override
    public JavaPairRDD<Object, VertexWritable> apply(final TraversalVertexProgram vertexProgram, final JavaPairRDD<Object, VertexWritable> inputRDD, final SparkMemory memory) {
        vertexProgram.setup(memory);
        final Traversal.Admin<Vertex, Long> traversal = (Traversal.Admin) vertexProgram.getTraversal();
        final TraverserSet<Long> haltedTraversers = new TraverserSet<>();
        haltedTraversers.add(traversal.getTraverserGenerator().generate(inputRDD.count(), (Step) traversal.getEndStep(), 1l));
        memory.set(TraversalVertexProgram.HALTED_TRAVERSERS, haltedTraversers);
        memory.incrIteration();
        return inputRDD;
    }

}
