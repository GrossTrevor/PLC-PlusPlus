package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("At Symbol", "@thelegend27", true),
                Arguments.of("Hyphenated", "Please-Work", true),
                Arguments.of("Single Character", "f", true),
                Arguments.of("Underscore in Word", "Hello_hi", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Special Character Not Allowed", "@fi@ve", false),
                Arguments.of("Dollar Sign", "fi$ve", false),
                Arguments.of("Leading Bracket", "]five", false),
                Arguments.of("Just Number", "2", false),
                Arguments.of("Just Space", " ", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Underscores", "_hello", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "0", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Leading Zeros", "000001", false),
                Arguments.of("Decimal", "5.1", false),
                Arguments.of("Comma Separated", "1,999", false),
                Arguments.of("Negative String", "-a", false),
                Arguments.of("Negative Zero", "-0", false),
                Arguments.of("Leading Zeros with Negative", "-000001", false),
                Arguments.of("Negative Symbols", "-&%$", false),
                Arguments.of("Number then String", "2345u777", false),
                Arguments.of("Escape Characters", "\\n\n", false),
                Arguments.of("Number then Symbols", "234.5++77", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Single Decimal", "3.0", true),
                Arguments.of("Negative Decimal Start with Zero", "-0.1", true),
                Arguments.of("Trailing Zeros", "123.000", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Single Digit", "1", false),
                Arguments.of("Double Decimal", "1..0", false),
                Arguments.of("Double Decimal with Num Between", "1.0.9.8", false),
                Arguments.of("Negative String", "-ajhg", false),
                Arguments.of("Leading Zeros with Negative", "-000.001", false),
                Arguments.of("Negative Symbols", "-+&%$", false),
                Arguments.of("Number then String", "234.5u777", false),
                Arguments.of("Escape Characters", "\\n\n", false),
                Arguments.of("Number then Symbols", "234.5++77", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false),
                Arguments.of("Missing Last Quote", "\'", false),
                Arguments.of("Newline Without Escape", "\'\n\'", false),
                Arguments.of("Escape r", "\'\r\'", false),
                Arguments.of("Missing Last Quote with Char", "\'n", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Symbols", "\"!@#$%^&*()\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Newline Without Escape", "\"wordsssss\n\"", false),
                Arguments.of("Newline Unterminated", "\"\n", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison Not Equals", "!=", true),
                Arguments.of("Comparison Equals", "==", true),
                Arguments.of("Assign", "=", true),
                Arguments.of("Compound Comparison OR", "||", true),
                Arguments.of("Compound Comparison AND", "&&", true),
                Arguments.of("Plus Sign", "+", true),
                Arguments.of("Dollar Sign", "$", true),
                Arguments.of("End of Line", ";", true),
                Arguments.of("Comma", ",", true),
                Arguments.of("Greater Than", ">", true),
                Arguments.of("Less Than", "<", true),
                Arguments.of("Single And Sign", "&", true),
                Arguments.of("Single Or Sign", "|", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Greater Than and Equal to", ">=", false),
                Arguments.of("Less Than and Equal to", "<=", false),
                Arguments.of("Less Than and Equal to", "-98", false),
                Arguments.of("Negative String", "-a", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Comparison Equals", "======", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    //make test case about 'm and "str
    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example 3", "print(\"Hello,\\n World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello,\\n World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 23),
                        new Token(Token.Type.OPERATOR, ";", 24)
                )),
                Arguments.of("Example 4", "VAR i = -1 : Integer;\nVAL inc = 2 : Integer;\nFUN foo() DO\n    WHILE i != 1 DO\n        IF i > 0 DO\n            print(\"bar\");\n        END\n        i = i + inc;\n    END\nEND", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "VAR", 0),
                        new Token(Token.Type.IDENTIFIER, "i", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "-1", 8),
                        new Token(Token.Type.OPERATOR, ":", 11),
                        new Token(Token.Type.IDENTIFIER, "Integer", 13),
                        new Token(Token.Type.OPERATOR, ";", 20),
                        new Token(Token.Type.IDENTIFIER, "VAL", 22),
                        new Token(Token.Type.IDENTIFIER, "inc", 26),
                        new Token(Token.Type.OPERATOR, "=", 30),
                        new Token(Token.Type.INTEGER, "2", 32),
                        new Token(Token.Type.OPERATOR, ":", 34),
                        new Token(Token.Type.IDENTIFIER, "Integer", 36),
                        new Token(Token.Type.OPERATOR, ";", 43),
                        new Token(Token.Type.IDENTIFIER, "FUN", 45),
                        new Token(Token.Type.IDENTIFIER, "foo", 49),
                        new Token(Token.Type.OPERATOR, "(", 52),
                        new Token(Token.Type.OPERATOR, ")", 53),
                        new Token(Token.Type.IDENTIFIER, "DO", 55),
                        new Token(Token.Type.IDENTIFIER, "WHILE", 62),
                        new Token(Token.Type.IDENTIFIER, "i", 68),
                        new Token(Token.Type.OPERATOR, "!=", 70),
                        new Token(Token.Type.INTEGER, "1", 73),
                        new Token(Token.Type.IDENTIFIER, "DO", 75),
                        new Token(Token.Type.IDENTIFIER, "IF", 86),
                        new Token(Token.Type.IDENTIFIER, "i", 89),
                        new Token(Token.Type.OPERATOR, ">", 91),
                        new Token(Token.Type.INTEGER, "0", 93),
                        new Token(Token.Type.IDENTIFIER, "DO", 95),
                        new Token(Token.Type.IDENTIFIER, "print", 110),
                        new Token(Token.Type.OPERATOR, "(", 115),
                        new Token(Token.Type.STRING, "\"bar\"", 116),
                        new Token(Token.Type.OPERATOR, ")", 121),
                        new Token(Token.Type.OPERATOR, ";", 122),
                        new Token(Token.Type.IDENTIFIER, "END", 132),
                        new Token(Token.Type.IDENTIFIER, "i", 144),
                        new Token(Token.Type.OPERATOR, "=", 146),
                        new Token(Token.Type.IDENTIFIER, "i", 148),
                        new Token(Token.Type.OPERATOR, "+", 150),
                        new Token(Token.Type.IDENTIFIER, "inc", 152),
                        new Token(Token.Type.OPERATOR, ";", 155),
                        new Token(Token.Type.IDENTIFIER, "END", 161),
                        new Token(Token.Type.IDENTIFIER, "END", 165)
                )),
                Arguments.of("Muliple Spaces", "one   two;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 6),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Equals Combination", "!======", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "!=", 0),
                        new Token(Token.Type.OPERATOR, "==", 2),
                        new Token(Token.Type.OPERATOR, "==", 4),
                        new Token(Token.Type.OPERATOR, "=", 6)
                )),
                Arguments.of("Not Whitespace", "one\btwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 4)
                )),
                Arguments.of("Double Decimals", "0.09.7", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "0.09", 0),
                        new Token(Token.Type.OPERATOR, ".", 4),
                        new Token(Token.Type.INTEGER, "7", 5)
                )),
                Arguments.of("Weird Quotes", "\'\"\'string\"\'\"", Arrays.asList(
                        new Token(Token.Type.CHARACTER, "\'\"\'", 0),
                        new Token(Token.Type.IDENTIFIER, "string", 3),
                        new Token(Token.Type.STRING, "\"\'\"", 9)
                )),
                Arguments.of("Leading Zeros are Integers", "0007", Arrays.asList(
                        new Token(Token.Type.INTEGER, "0", 0),
                        new Token(Token.Type.INTEGER, "0", 1),
                        new Token(Token.Type.INTEGER, "0", 2),
                        new Token(Token.Type.INTEGER, "7", 3)
                )),
                Arguments.of("Multiple Decimals ", "0...7", Arrays.asList(
                        new Token(Token.Type.INTEGER, "0", 0),
                        new Token(Token.Type.OPERATOR, ".", 1),
                        new Token(Token.Type.OPERATOR, ".", 2),
                        new Token(Token.Type.OPERATOR, ".", 3),
                        new Token(Token.Type.INTEGER, "7", 4)
                )),
                Arguments.of("Integer with Decimal at End", "0.", Arrays.asList(
                        new Token(Token.Type.INTEGER, "0", 0),
                        new Token(Token.Type.OPERATOR, ".", 1)
                )),
                Arguments.of("Negative Zero", "-0", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "-", 0),
                        new Token(Token.Type.INTEGER, "0", 1)
                )),
                Arguments.of("Negative Zero Decimal", "-0.0", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "-0.0", 0)
                )),
                Arguments.of("Negative Leading Zero Decimal", "-09.0", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "-", 0),
                        new Token(Token.Type.INTEGER, "0", 1),
                        new Token(Token.Type.DECIMAL, "9.0", 2)
                )),
                Arguments.of("Negative Zero Decimal Multiple Trailing Zeros", "-0.0000000", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "-0.0000000", 0)
                )),
                Arguments.of("Plus with Numbers", "7+3", Arrays.asList(
                        new Token(Token.Type.INTEGER, "7", 0),
                        new Token(Token.Type.OPERATOR, "+", 1),
                        new Token(Token.Type.INTEGER, "3", 2)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated 'f'").lex());
        Assertions.assertEquals(17, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}