/*
 * Copyright 2020 MINES Saint-Étienne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.mines_stetienne.ci.sparql_generate.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionBase1;
import fr.mines_stetienne.ci.sparql_generate.utils.LogUtils;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/JSONListKeys">iter:JSONListKeys</a>
 * iterates over the keys of a JSON object.
 *
 * <p>
 * See
 * <a href="https://w3id.org/sparql-generate/playground.html#ex=example/generate/Example-JSONListKeys">Live
 * example</a></p>
 * 
 * <ul>
 * <li>Param 1: (json): a JSON object.</li>
 * </ul>
 *
 * @author Maxime Lefrançois
 */
public class ITER_JSONListKeys extends IteratorFunctionBase1 {

    public static final String URI = SPARQLExt.ITER + "JSONListKeys";

    private static final Logger LOG = LoggerFactory.getLogger(ITER_JSONListKeys.class);

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/application/json";

    private static final Gson GSON = new Gson();

    @Override
    public List<List<NodeValue>> exec(NodeValue json) {
        if(json == null) {
        	String msg = "No JSON provided";
            LOG.debug(msg);
        	throw new ExprEvalException(msg);
        }
        if (json.getDatatypeURI() != null
                && !json.getDatatypeURI().equals(datatypeUri)
                && !json.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST have been"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>."
                    + " Got <" + json.getDatatypeURI() + ">"
            );
        }
        try {
            Set<String> keys = GSON.fromJson(json.asNode().getLiteralLexicalForm(), Map.class).keySet();
            List<List<NodeValue>> listNodeValues = new ArrayList<>(keys.size());
            for (String key : keys) {
                NodeValue nodeValue
                        = NodeValue.makeNode(NodeFactory.createLiteral(key));
                listNodeValues.add(Collections.singletonList(nodeValue));
            }
            LOG.trace("end JSONListKeys");
            return listNodeValues;
        } catch (Exception ex) {
            if(LOG.isDebugEnabled()) {
                Node compressed = LogUtils.compress(json.asNode());
                LOG.debug("No evaluation for " + compressed, ex);
            }
            throw new ExprEvalException("No evaluation", ex);
        }
    }
	
}
