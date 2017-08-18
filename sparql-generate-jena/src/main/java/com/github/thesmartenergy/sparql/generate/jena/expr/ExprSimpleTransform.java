/*
 * Copyright 2017 Ecole des Mines de Saint-Etienne.
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
package com.github.thesmartenergy.sparql.generate.jena.expr;

import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprFunction0;
import org.apache.jena.sparql.expr.ExprFunction1;
import org.apache.jena.sparql.expr.ExprFunction2;
import org.apache.jena.sparql.expr.ExprFunction3;
import org.apache.jena.sparql.expr.ExprFunctionN;
import org.apache.jena.sparql.expr.ExprFunctionOp;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.NodeValue;

/**
 *
 * @author maxime.lefrancois
 */
public interface ExprSimpleTransform {

    public Expr transform(ExprFunction0 func) ;
    public Expr transform(ExprFunction1 func) ;
    public Expr transform(ExprFunction2 func) ;
    public Expr transform(ExprFunction3 func) ;
    public Expr transform(ExprFunctionN func) ;
    public Expr transform(ExprFunctionOp funcOp) ;
    public Expr transform(NodeValue nv) ;
    public Expr transform(ExprVar nv) ;
    public Expr transform(ExprAggregator eAgg) ;    
}
