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
package org.apache.tinkerpop.gremlin.hadoop.process.computer.spark;

import org.apache.tinkerpop.gremlin.process.computer.MessageScope;
import org.apache.tinkerpop.gremlin.process.computer.Messenger;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.VertexStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.StartStep;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class SparkMessenger<M> implements Messenger<M> {

    private Vertex vertex;
    private  Iterable<M> incomingMessages = new ArrayList<>();
    private final List<Tuple2<Object,M>> outgoingMessages = new ArrayList<>();

    public void setVertexAndMessages(final Vertex vertex, final Iterable<M> incomingMessages) {
        this.vertex = vertex;
        this.incomingMessages = incomingMessages;
        this.outgoingMessages.clear();
    }

    public List<Tuple2<Object,M>> getOutgoingMessages() {
        return this.outgoingMessages;
    }


    @Override
    public Iterable<M> receiveMessages(final MessageScope messageScope) {
        return this.incomingMessages;
    }

    @Override
    public void sendMessage(final MessageScope messageScope, final M message) {
        if (messageScope instanceof MessageScope.Local) {
            final MessageScope.Local<M> localMessageScope = (MessageScope.Local) messageScope;
            final Traversal.Admin<Vertex, Edge> incidentTraversal = SparkMessenger.setVertexStart(localMessageScope.getIncidentTraversal().get(), this.vertex);
            final Direction direction = SparkMessenger.getOppositeDirection(incidentTraversal);
            incidentTraversal.forEachRemaining(edge -> this.outgoingMessages.add(new Tuple2<>(edge.vertices(direction).next().id(), message)));
        } else {
            ((MessageScope.Global) messageScope).vertices().forEach(v -> this.outgoingMessages.add(new Tuple2<>(v.id(), message)));
        }
    }

    ///////////

    private static <T extends Traversal.Admin<Vertex, Edge>> T setVertexStart(final Traversal<Vertex, Edge> incidentTraversal, final Vertex vertex) {
        incidentTraversal.asAdmin().addStep(0, new StartStep<>(incidentTraversal.asAdmin(), vertex));
        return (T) incidentTraversal;
    }

    private static Direction getOppositeDirection(final Traversal.Admin<Vertex, Edge> incidentTraversal) {
        final VertexStep step = TraversalHelper.getLastStepOfAssignableClass(VertexStep.class, incidentTraversal).get();
        return step.getDirection().opposite();
    }
}