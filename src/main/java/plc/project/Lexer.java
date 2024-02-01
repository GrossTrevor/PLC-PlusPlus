package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    private ParseException parEx;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<Token>();
        Token temp = null;
        while(chars.has(0)){
            char cur = chars.get(0);
            if(cur == '\b' || cur == '\n' || cur == '\r' || cur == '\t'){
                chars.advance();
                chars.skip();
            }
            else {
                temp = lexToken();
                if(temp != null)
                    tokens.add(temp.getIndex(), temp);
            }
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        char cur = chars.get(0);
        Token token;
        if(Character.isDigit(cur) || peek("-", "[0-9]"))
            return lexNumber();
        else if(cur == '\'')
            return lexCharacter();
        else if(cur == '\"')
            return lexString();
        else if(Character.isLetter(cur) || cur == '@')
            return lexIdentifier();
            //lexEscape()
        else
            return lexOperator();
    }

    public Token lexIdentifier() {
        match("[@a-zA-Z]");
        while(peek("[a-zA-Z0-9_-]")){
            match("[a-zA-Z0-9_-]");
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        boolean decimal = false;
        if(peek("-")){
            match("-");
        }

        if(peek("0")){
            match("0");
            if(!peek(".", "[0-9]")){
                throw new ParseException("parse exception", 0);
            }
            else{
                decimal = true;
            }
        }

        while(peek("[.0-9]")){
            if(peek("[0-9]")){
                match("[0-9]");
            }
            else {
                match(".");
                if(!peek("[0-9]")) {
                    throw new ParseException("parse exception", 0);
                }
                if(decimal == true){
                    throw new ParseException("parse exception", 0);
                }
                decimal = true;
                match("[0-9]");
            }
        }

        if(decimal){
            return chars.emit(Token.Type.DECIMAL);
        }
        else{
            return chars.emit(Token.Type.INTEGER);
        }
    }

    public Token lexCharacter() {
        match("'");
        if(peek("[']")){
            throw new ParseException("parse exception", 0);
        }
        if(peek("\\\\", "[bnrt\\\'\"]")){
            lexEscape();
        }
        else if(!peek(".")){
            throw new ParseException("parse exception", 0);
        }
        else{
            match(".");
        }
        if(peek("[']")){
            match("'");
        }
        else{
            throw new ParseException("parse exception", 0);
        }
        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        match("\"");
        while(!peek("[\"]")){
            if(peek("[\\\n\r]")){
                throw new ParseException("parse exception", 0);
            }
            if(!peek(".")){
                throw new ParseException("parse exception", 0);
            }
            if(peek("\\\\", "[^bnrt\\\'\"]")){
                throw new ParseException("parse exception", 0);
            }
            if(peek("\\\\", "[bnrt\\\'\"]")){
                lexEscape();
            }
            else{
                match(".");
            }
        }
        match("\"");
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        if(peek("\\\\", "[bnrt\\\'\"]")){
            match("\\\\", "[bnrt\\\'\"]");
        }
    }

    public Token lexOperator() {
        if(peek("[!=]")){
            match("[!=]");
            if(peek("=")){
                match("[!=]");
            }
        }
        else if(peek("[|]")){
            match("[|]");
            if(peek("[|]")){
                match("[|]");
            }
        }
        else if(peek("&")){
            match("&");
            if(peek("&")){
                match("&");
            }
        }
        else if(peek("[~!@#$%^*()_-]")){
            match("[~!@#$%^*()_-]");
        }
        else if(peek("[+={}\\[\\];:?<>,.]")){
            match("[+={}\\[\\];:?<>,.]");
        }
        else{
            throw new ParseException("parse exception1", 0);
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for(int i = 0; i < patterns.length; i++){
            if(!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])){
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if(peek){
            for(int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
