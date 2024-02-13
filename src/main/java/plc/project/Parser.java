package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if(tokens.has(0)){
            parseExpression();
            //temp = new Ast.Statement.Expression(parseExpression());
            if(peek("=")){
                match("=");
                parseExpression();
                //temp = temp + new Ast.Statement.Expression(parseExpression());
            }
            if(!peek(";")){
                throw new ParseException("parse exception, no semicolon", tokens.index + 1);
            }
            else{
                match(";");
                return new Ast.Statement.Expression(parseExpression());
            }
        }
        throw new ParseException("parse exception", tokens.index + 1);
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        boolean binary = false;
        Ast.Expression temp1 = null;
        String temp2 = "";
        Ast.Expression temp3 = null;

        if(tokens.has(0)){
            temp1 = parseComparisonExpression();
            if (!tokens.has(0)){
                return temp1;
            }
        }
        if(peek("&", "&") || peek("[|]", "[|]")){
            temp2 = tokens.get(0).getLiteral() + tokens.get(1).getLiteral();
            match(".", ".");
            binary = true;
        }
        if(tokens.has(0)){
            temp3 = parseComparisonExpression();
        }
        if(binary){
            return new Ast.Expression.Binary(temp2, temp1, temp3);
        }
        return parseComparisonExpression();
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        boolean binary = false;
        Ast.Expression temp1 = null;
        String temp2 = "";
        Ast.Expression temp3 = null;

        if(tokens.has(0)){
            temp1 = parseAdditiveExpression();
            if (!tokens.has(0)){
                return temp1;
            }
        }
        if(peek("=", "=") || peek("!", "=")){
            temp2 = tokens.get(0).getLiteral() + tokens.get(1).getLiteral();
            match(".", "=");
            binary = true;
        }
        else if(peek(">") || peek("<")){
            temp2 = tokens.get(0).getLiteral();
            match(".");
            binary = true;
        }
        if(tokens.has(0)){
            temp3 = parseAdditiveExpression();
        }
        if(binary){
            return new Ast.Expression.Binary(temp2, temp1, temp3);
        }
        return parseAdditiveExpression();
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        boolean binary = false;
        Ast.Expression temp1 = null;
        String temp2 = "";
        Ast.Expression temp3 = null;

        if(tokens.has(0)){
            temp1 = parseMultiplicativeExpression();
            if (!tokens.has(0)){
                return temp1;
            }
        }
        if(peek("[+]") || peek("-")){
            temp2 = tokens.get(0).getLiteral();
            match(".");
            binary = true;
        }
        if(tokens.has(0)){
            temp3 = parseMultiplicativeExpression();
        }
        if(binary){
            return new Ast.Expression.Binary(temp2, temp1, temp3);
        }
        return parseMultiplicativeExpression();
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        boolean binary = false;
        Ast.Expression temp1 = null;
        String temp2 = "";
        Ast.Expression temp3 = null;

        if(tokens.has(0)){
            temp1 = parsePrimaryExpression();
            if (!tokens.has(0)){
                return temp1;
            }
        }
        if(peek("[*]") || peek("/") || peek("^")){
            temp2 = tokens.get(0).getLiteral();
            match(".");
            binary = true;
        }
        if(tokens.has(0)){
            temp3 = parsePrimaryExpression();
        }
        if(binary){
            return new Ast.Expression.Binary(temp2, temp1, temp3);
        }
        return parsePrimaryExpression();
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        System.out.println(tokens.get(0).getType());
        System.out.println(tokens.get(0).getLiteral());
        if (peek(Token.Type.IDENTIFIER)){
            return parseIdentifier();
        }
        else if (peek(Token.Type.INTEGER)){
            return parseInteger();
        }
        else if (peek(Token.Type.DECIMAL)){
            return parseDecimal();
        }
        else if (peek(Token.Type.CHARACTER)){
            return parseCharacter();
        }
        else if (peek(Token.Type.STRING)){
            return parseString();
        }
        else if (peek("(")){
            return parseGroup();
        }
        else {
            throw new ParseException("parse exception primary", tokens.index);
        }
    }

    public Ast.Expression parseIdentifier() throws ParseException{
        if (peek("NIL")){
            match("NIL");
            return parseLiteral(null);
        }
        else if (peek("TRUE")){
            match("TRUE");
            return parseLiteral(Boolean.TRUE);
        }
        else if (peek("FALSE")){
            match("FALSE");
            return parseLiteral(Boolean.FALSE);
        }
        else {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (peek("[")) {
                return parseAccess(false, name);
            }
            else if (peek("(")) {
                return parseExFunction(name);
            }
            else {
                return parseAccess(true, name);
            }
        }
    }

    public Ast.Expression parseInteger() throws ParseException{
        BigInteger bigInteger;
        bigInteger = new BigInteger(tokens.get(0).getLiteral());
        match(Token.Type.INTEGER);
        return parseLiteral(bigInteger);
    }

    public Ast.Expression parseDecimal() throws ParseException{
        BigDecimal bigDecimal;
        bigDecimal = new BigDecimal(tokens.get(0).getLiteral());
        match(Token.Type.DECIMAL);
        return parseLiteral(bigDecimal);
    }

    public Ast.Expression parseCharacter() throws ParseException{
        String s = tokens.get(0).getLiteral().substring(1, tokens.get(0).getLiteral().length()-1);
        // s.replace('\\n', '\n');
        match(Token.Type.CHARACTER);
        return parseLiteral(s);
    }

    public Ast.Expression parseString() throws ParseException{
        String s = tokens.get(0).getLiteral().substring(1, tokens.get(0).getLiteral().length()-1);
        // s.replace('\\n', '\n');
        match(Token.Type.STRING);
        return parseLiteral(s);
    }

    public Ast.Expression parseLiteral(Object obj){
        return new Ast.Expression.Literal(obj);
    }

    public Ast.Expression parseGroup(){
        match("(");
        Ast.Expression exp = new Ast.Expression.Group(parseExpression());

        if (!peek(")")){
            throw new ParseException("parse exception", tokens.index);
        }
        match(")");
        return exp;
    }

    public Ast.Expression parseAccess(boolean single, String name){
        if (single){
            return new Ast.Expression.Access(Optional.empty(), name);
        }
        match("[");
        Ast.Expression exp = new Ast.Expression.Access(Optional.of(parseExpression()), name);

        if (!peek("]")){
            throw new ParseException("parse exception", tokens.index);
        }
        match("]");
        return exp;

    }

    public Ast.Expression parseExFunction(String name){
        match("(");
        List<Ast.Expression> exps = new ArrayList<Ast.Expression>();
        int i = 0;
        while (!peek(")")){
            if (peek(",")){
                throw new ParseException("parse exception", tokens.index);
            }
            exps.add(i, parseExpression());
            i++;
            if (peek(",")){
                match(",");
                if (peek(")")){
                    throw new ParseException("parse exception", tokens.index);
                }
            }
            else if (!peek(")")){
                throw new ParseException("parse exception", tokens.index);
            }
        }
        match(")");
        return new Ast.Expression.Function(name, exps);
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type){
                if (patterns[i] != tokens.get(i).getType()){
                    return false;
                }
            }
            else if (patterns[i] instanceof String){
                if (!patterns[i].equals(tokens.get(i).getLiteral())){
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek){
            for (int i = 0; i < patterns.length; i++){
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
