/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.planner.projection.builder;

import io.crate.analyze.symbol.Function;
import io.crate.analyze.symbol.InputColumn;
import io.crate.analyze.symbol.Literal;
import io.crate.analyze.symbol.Symbol;
import io.crate.test.integration.CrateUnitTest;
import io.crate.testing.SqlExpressions;
import io.crate.testing.T3;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;

public class InputCreatingVisitorTest extends CrateUnitTest {

    @Test
    public void testNonDeterministicFunctionsReplacement() throws Exception {
        SqlExpressions sqlExpressions = new SqlExpressions(T3.SOURCES, T3.TR_1);
        Function fn1 = (Function) sqlExpressions.asSymbol("random()");
        Function fn2 = (Function) sqlExpressions.asSymbol("random()");

        List<Symbol> inputSymbols = Arrays.<Symbol>asList(
            Literal.BOOLEAN_FALSE,
            sqlExpressions.asSymbol("upper(a)"),
            fn1,
            fn2
        );

        Function newSameFn = (Function) sqlExpressions.asSymbol("random()");
        Function newDifferentFn = (Function) sqlExpressions.asSymbol("random()");
        InputCreatingVisitor.Context context = new InputCreatingVisitor.Context(inputSymbols);

        Symbol replaced1 = InputCreatingVisitor.INSTANCE.process(fn1, context);
        assertThat(replaced1, is(instanceOf(InputColumn.class)));
        assertThat(((InputColumn) replaced1).index(), is(2));

        Symbol replaced2 = InputCreatingVisitor.INSTANCE.process(fn2, context);
        assertThat(replaced2, is(instanceOf(InputColumn.class)));
        assertThat(((InputColumn) replaced2).index(), is(3));

        Symbol replaced3 = InputCreatingVisitor.INSTANCE.process(newSameFn, context);
        assertThat(replaced3, is(equalTo((Symbol) newSameFn))); // not replaced

        Symbol replaced4 = InputCreatingVisitor.INSTANCE.process(newDifferentFn, context);
        assertThat(replaced4, is(equalTo((Symbol) newDifferentFn))); // not replaced
    }
}
