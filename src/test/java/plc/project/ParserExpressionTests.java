package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 */
final class ParserExpressionTests {

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Statement.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Function("name", Arrays.asList()))
                ),
                Arguments.of("Function With Multiple Parameters",
                        Arrays.asList(
                                //func(butt, cheek, pen15);
                                new Token(Token.Type.IDENTIFIER, "func", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "butt", 5),
                                new Token(Token.Type.OPERATOR, ",", 9),
                                new Token(Token.Type.IDENTIFIER, "cheek", 11),
                                new Token(Token.Type.OPERATOR, ",", 16),
                                new Token(Token.Type.IDENTIFIER, "pen15", 18),
                                new Token(Token.Type.OPERATOR, ")", 23),
                                new Token(Token.Type.OPERATOR, ";", 24)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Function("func", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "butt"),
                                new Ast.Expression.Access(Optional.empty(), "cheek"),
                                new Ast.Expression.Access(Optional.empty(), "pen15")
                        )))),
                Arguments.of("Variable Expression",
                        Arrays.asList(
                                //expr;
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, ";", 4)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "expr"))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFailExpressionStatement(String test, List<Token> tokens, ParseException expected) {
        test(tokens, null, Parser::parseStatement);
    }

    private static Stream<Arguments> testFailExpressionStatement() {
        return Stream.of(
                Arguments.of("Missing Semicolon",
                        Arrays.asList(
                                //f
                                new Token(Token.Type.IDENTIFIER, "f", 0)
                        ),
                        new ParseException("parse exception", 0)
                ),
                Arguments.of("WAS IST DAS?????",
                        Arrays.asList(
                                //?
                                new Token(Token.Type.IDENTIFIER, "?", 0)
                        ),
                        new ParseException("parse exception", 0)
                ),
                Arguments.of("Missing Closing Parenthesis",
                        Arrays.asList(
                                //(expr
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                        ),
                        new ParseException("parse exception", 5)
                ),
                Arguments.of("Invalid Closing Parenthesis",
                        Arrays.asList(
                                //(expr]
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, "]", 5)
                        ),
                        new ParseException("parse exception", 5)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Statement.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                //name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "name"),
                                new Ast.Expression.Access(Optional.empty(), "value")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFailAssignmentStatement(String test, List<Token> tokens, ParseException expected) {
        test(tokens, null, Parser::parseStatement);
    }

    private static Stream<Arguments> testFailAssignmentStatement() {
        return Stream.of(
                Arguments.of("Missing Value",
                        Arrays.asList(
                                //name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, ";", 7)
                        ),
                        new ParseException("parse exception", 7)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expression.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expression.Literal(Boolean.TRUE)
                ),
                Arguments.of("Integer Literal",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expression.Literal(new BigInteger("1"))
                ),
                Arguments.of("Decimal Literal",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expression.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                        new Ast.Expression.Literal('c')
                ),
                Arguments.of("String Literal",
                        Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                        new Ast.Expression.Literal("string")
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expression.Literal("Hello,\nWorld!")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expression.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Grouped Binary",
                        Arrays.asList(
                                //(expr1 + expr2)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        ))
                ),
                Arguments.of("Grouped Grouped Grouped Binary",
                        Arrays.asList(
                                //(((expr1 ^ expr2)))
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.OPERATOR, "(", 1),
                                new Token(Token.Type.OPERATOR, "(", 2),
                                new Token(Token.Type.IDENTIFIER, "expr1", 3),
                                new Token(Token.Type.OPERATOR, "^", 9),
                                new Token(Token.Type.IDENTIFIER, "expr2", 11),
                                new Token(Token.Type.OPERATOR, ")", 16),
                                new Token(Token.Type.OPERATOR, ")", 17),
                                new Token(Token.Type.OPERATOR, ")", 18)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Group(new Ast.Expression.Group(new Ast.Expression.Binary("^",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")))
                        ))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFailGroupExpression(String test, List<Token> tokens, ParseException expected) {
        test(tokens, null, Parser::parseExpression);
    }

    private static Stream<Arguments> testFailGroupExpression() {
        return Stream.of(
                Arguments.of("Missing Closing Parenthesis",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                        ),
                        new ParseException("parse exception", 5)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expression.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And",
                        Arrays.asList(
                                //expr1 && expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Addition",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Minus Sign",
                        Arrays.asList(
                                //expr1 - expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "-", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("-",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Not Equals Sign",
                        Arrays.asList(
                                //expr1 != expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "!=", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Or Sign",
                        Arrays.asList(
                                //expr1 || expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "||", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Front Slash Sign",
                        Arrays.asList(
                                //expr1 / expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "/", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("/",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Carrot Sign",
                        Arrays.asList(
                                //expr1 ^ expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "^", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Priority + then *",
                        Arrays.asList(
                                //expr1 + expr2 * expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "*", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "expr2"),
                                        new Ast.Expression.Access(Optional.empty(), "expr3")
                                )
                        )
                ),
                Arguments.of("Priority * then +",
                        Arrays.asList(
                                //expr1 + expr2 * expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "+", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Priority && then ||",
                        Arrays.asList(
                                //expr1 + expr2 * expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "||", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11)
                        ),
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Binary("&&",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Priority && then &&",
                        Arrays.asList(
                                //expr1 + expr2 * expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "&&", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Binary("&&",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Priority || then &&",
                        Arrays.asList(
                                //expr1 + expr2 * expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "||", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "&&", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Binary("||",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Priority == then !=",
                        Arrays.asList(
                                //expr1 + expr2 * expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "!=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11)
                        ),
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Binary("==",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Priority == then != then >",
                        Arrays.asList(
                                //expr1 == expr2 != expr3 == expr4
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "!=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11),
                                new Token(Token.Type.OPERATOR, ">", 13),
                                new Token(Token.Type.IDENTIFIER, "expr4", 15)
                        ),
                        new Ast.Expression.Binary(">",
                                new Ast.Expression.Binary("!=",
                                        new Ast.Expression.Binary("==",
                                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                                new Ast.Expression.Access(Optional.empty(), "expr2")
                                        ),
                                        new Ast.Expression.Access(Optional.empty(), "expr3")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr4")
                        )

                ),
                Arguments.of("Priority + then * then &&",
                        Arrays.asList(
                                //expr1 + expr2 * expr3 && expr4
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "*", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11),
                                new Token(Token.Type.OPERATOR, "&&", 13),
                                new Token(Token.Type.IDENTIFIER, "expr4", 15)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Binary("*",
                                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                                new Ast.Expression.Access(Optional.empty(), "expr3")
                                        )
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr4")
                        )

                ),
                Arguments.of("Priority == then != then !=",
                        Arrays.asList(
                                //expr1 == expr2 != expr3 != expr4
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "!=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11),
                                new Token(Token.Type.OPERATOR, "!=", 13),
                                new Token(Token.Type.IDENTIFIER, "expr4", 15)
                        ),
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Binary("!=",
                                        new Ast.Expression.Binary("==",
                                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                                new Ast.Expression.Access(Optional.empty(), "expr2")
                                        ),
                                        new Ast.Expression.Access(Optional.empty(), "expr3")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr4")
                        )

                ),
                Arguments.of("Priority == then != then +",
                        Arrays.asList(
                                //expr1 == expr2 != expr3 + expr4
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "!=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11),
                                new Token(Token.Type.OPERATOR, "+", 13),
                                new Token(Token.Type.IDENTIFIER, "expr4", 15)
                        ),
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Binary("==",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "expr3"),
                                        new Ast.Expression.Access(Optional.empty(), "expr4")
                                )
                        )

                ),
                Arguments.of("Priority && then *",
                        Arrays.asList(
                                //expr1 && expr2 * expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "*", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "expr2"),
                                        new Ast.Expression.Access(Optional.empty(), "expr3")
                                )
                        )
                ),
                Arguments.of("Priority && then && then ||",
                        Arrays.asList(
                                //expr1 && expr2 && expr3 || expr4
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "&&", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11),
                                new Token(Token.Type.OPERATOR, "||", 13),
                                new Token(Token.Type.IDENTIFIER, "expr4", 15)
                        ),
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Binary("&&",
                                        new Ast.Expression.Binary("&&",
                                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                                new Ast.Expression.Access(Optional.empty(), "expr2")
                                        ),
                                        new Ast.Expression.Access(Optional.empty(), "expr3")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr4")
                        )

                ),
                Arguments.of("Priority - then +",
                        Arrays.asList(
                                //expr1 - expr2 + expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "-", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "+", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Binary("-",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Priority * then ^",
                        Arrays.asList(
                                //expr1 * expr2 ^ expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "^", 9),
                                new Token(Token.Type.IDENTIFIER, "expr3", 11)
                        ),
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFailBinaryExpression(String test, List<Token> tokens, ParseException expected) {
        test(tokens, null, Parser::parseExpression);
    }

    private static Stream<Arguments> testFailBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary Carrot Sign",
                        Arrays.asList(
                                //expr1 ^ expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "^", 6)
                        ),
                        new ParseException("parse exception", 6)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expression.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expression.Access(Optional.empty(), "name")
                ),
                Arguments.of("List Index Access",
                        Arrays.asList(
                                //list[expr]
                                new Token(Token.Type.IDENTIFIER, "list", 0),
                                new Token(Token.Type.OPERATOR, "[", 4),
                                new Token(Token.Type.IDENTIFIER, "expr", 5),
                                new Token(Token.Type.OPERATOR, "]", 9)
                        ),
                        new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")), "list")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expression.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                //name()
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        ))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFailFunctionExpression(String test, List<Token> tokens, ParseException expected) {
        test(tokens, null, Parser::parseExpression);
    }

    private static Stream<Arguments> testFailFunctionExpression() {
        return Stream.of(
                Arguments.of("Trailing Comma",
                        Arrays.asList(
                                //name(pinkCanoe,)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "pinkCanoe", 5),
                                new Token(Token.Type.OPERATOR, ",", 14),
                                new Token(Token.Type.OPERATOR, ")", 15)
                        ),
                        new ParseException("parse exception", 14)
                )
        );
    }

    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

}