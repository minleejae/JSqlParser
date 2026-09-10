/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.util.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.util.validation.feature.FeaturesAllowed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class DataModifyingCteValidationTest {
    static Stream<Arguments> modifyingBodies() {
        return Stream.of(
                Arguments.of("DELETE FROM foo RETURNING id", Feature.delete),
                Arguments.of("UPDATE foo SET id = 1 RETURNING id", Feature.update),
                Arguments.of("INSERT INTO foo VALUES (1) RETURNING id", Feature.insert));
    }

    @ParameterizedTest
    @MethodSource("modifyingBodies")
    void reportsDisallowedDmlInsteadOfCastingToSelect(String body, Feature operation) {
        String sql = "WITH x AS (" + body + ") SELECT * FROM x";
        for (FeaturesAllowed allowed : List.of(FeaturesAllowed.SELECT,
                FeaturesAllowed.SELECT.copy().add(Feature.withItem))) {
            List<ValidationError> errors = Validation.validate(List.of(allowed), sql);
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getParsedStatement()).isNotNull();
            assertThat(errors.get(0).getErrors()).extracting(Throwable::getMessage)
                    .contains(operation + " not allowed.");
        }
    }

    @ParameterizedTest
    @MethodSource("modifyingBodies")
    void allowsDmlWhenItsFeaturesAreEnabled(String body, Feature operation) {
        String sql = "WITH x AS (" + body + ") SELECT * FROM x";
        List<ValidationError> errors =
                Validation.validate(List.of(new FeaturesAllowed(Feature.values())), sql);
        assertThat(errors).as("allowed %s CTE", operation).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "WITH x AS (SELECT id FROM foo) SELECT * FROM x",
            "WITH RECURSIVE x(id) AS (SELECT 1 UNION ALL SELECT id + 1 FROM x WHERE id < 3) SELECT * FROM x",
            "WITH x AS (WITH y AS (SELECT id FROM foo) SELECT * FROM y) SELECT * FROM x"})
    void keepsSelectAndRecursiveCteValidation(String sql) {
        assertThat(Validation.validate(List.of(FeaturesAllowed.SELECT.copy()
                .add(Feature.withItem, Feature.withItemRecursive, Feature.setOperation,
                        Feature.setOperationUnion)),
                sql)).isEmpty();
    }

    @Test
    void collectsErrorsFromMultipleBodiesAndFollowingStatements() {
        String sql = "WITH x AS (DELETE FROM foo RETURNING id), "
                + "y AS (UPDATE bar SET id = 2 RETURNING id) SELECT * FROM x; SELECT 1";
        Validation validation = new Validation(List.of(FeaturesAllowed.SELECT.copy()
                .add(Feature.withItem)), sql);
        List<ValidationError> errors = validation.validate();
        assertThat(validation.getParsedStatements()).hasSize(2);
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getErrors()).extracting(Throwable::getMessage)
                .contains("delete not allowed.", "update not allowed.");
    }

    @Test
    void visitsNestedBodiesAndReturningExpressions() {
        String sql = "WITH x AS (WITH y AS (DELETE FROM foo RETURNING id) "
                + "SELECT id FROM y) SELECT * FROM x";
        List<ValidationError> errors = Validation.validate(List.of(FeaturesAllowed.SELECT.copy()
                .add(Feature.withItem)), sql);
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getErrors()).extracting(Throwable::getMessage)
                .contains("delete not allowed.");

        FeaturesAllowed allowed = new FeaturesAllowed(Feature.values()).remove(Feature.function);
        errors = Validation.validate(List.of(allowed),
                "WITH x AS (DELETE FROM foo RETURNING upper(id)) SELECT * FROM x");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getErrors()).extracting(Throwable::getMessage)
                .containsExactly("function not allowed.");
    }
}
