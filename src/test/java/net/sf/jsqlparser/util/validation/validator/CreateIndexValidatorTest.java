/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2020 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.util.validation.validator;

import java.util.Arrays;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.util.validation.ValidationTestAsserts;
import net.sf.jsqlparser.util.validation.feature.DatabaseType;
import net.sf.jsqlparser.util.validation.feature.FeaturesAllowed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CreateIndexValidatorTest extends ValidationTestAsserts {

    @Test
    public void testValidateCreateIndex() throws JSQLParserException {
        for (String sql : Arrays.asList(
                "CREATE INDEX idx_american_football_action_plays_1 ON american_football_action_plays USING btree (play_type)",
                "CREATE INDEX idx_func ON american_football_action_plays ((play_type + 1))")) {
            validateNoErrors(sql, 1, DatabaseType.DATABASES);
        }
    }

    @Test
    public void testValidateCreateIndexNotAllowed() throws JSQLParserException {
        for (String sql : Arrays.asList(
                "CREATE INDEX idx_american_football_action_plays_1 ON american_football_action_plays USING btree (play_type)")) {
            validateNotAllowed(sql, 1, 1, FeaturesAllowed.DML, Feature.createIndex);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CREATE INDEX ix ON t ((lower(name)))",
            "CREATE INDEX ix ON t (name text_ops (option = lower('value')))",
            "CREATE INDEX ix ON t (name) WITH (option = lower('value'))",
            "CREATE INDEX ix ON t (name) WHERE lower(name) = 'value'"})
    void validatesExpressionsInAllIndexClauses(String sql) {
        FeaturesAllowed allowed = new FeaturesAllowed(Feature.values());
        validateNoErrors(sql, 1, allowed);
        validateNotAllowed(sql, 1, 1, allowed.remove(Feature.function), Feature.function);
    }

}
