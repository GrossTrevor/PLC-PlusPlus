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
        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functs = new ArrayList<>();
        Ast.Global tempG = null;
        Ast.Function tempF = null;
        while(true){
            tempG = parseGlobal();
            if (tempG != null)
                globals.add(tempG);
            else
                break;
        }
        while(true){
            tempF = parseFunction();
            if (tempF != null)
                functs.add(tempF);
            else
                break;
        }
        return new Ast.Source(globals, functs);
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        Ast.Global glob = null;
        if (peek("LET")){
            match("LET");
            glob = parseList();
        }
        else if (peek("VAR")){
            match("VAR");
            glob = parseMutable();
        }
        else if (peek("VAL")){
            match("VAL");
            glob = parseImmutable();
        }
        else
            return null;

        if(peek(";")){
            match(";");
            return glob;
        }
        else if (tokens.has(0))
            throw new ParseException("parse exception, no semicolon in global", tokens.get(0).getIndex());
        throw new ParseException("parse exception, no semicolon in global", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        String name = "";
        List<Ast.Expression> expressionList = new ArrayList<>();
        if (peek(Token.Type.IDENTIFIER)){
            name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        }
        else
            throw new ParseException("parse exception, no identifier in list", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());

        if (!peek("=")){
            throw new ParseException("parse exception, no equals in list", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        match("=");

        if (!peek("[")){
            throw new ParseException("parse exception, no open bracket in list", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        match("[");

        expressionList.add(parseExpression());

        while (peek(",")){
            match(",");
            expressionList.add(parseExpression());
        }

        if(!peek("]")){
            throw new ParseException("parse exception, no close bracket in list", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }

        Ast.Expression PLCList = new Ast.Expression.PlcList(expressionList);
        return new Ast.Global(name, false, Optional.of(PLCList));
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        String name = "";
        if (peek(Token.Type.IDENTIFIER)){
            name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        }
        else
            throw new ParseException("parse exception, no identifier in mutable", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        Ast.Expression exp = null;
        if (peek("=")){
            match("=");
            exp = parseExpression();
            return new Ast.Global(name, true, Optional.of(exp));
        }
        else
            return new Ast.Global(name, true, Optional.empty());
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        String name = "";
        if (peek(Token.Type.IDENTIFIER)){
            name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        }
        else
            throw new ParseException("parse exception, no identifier in immutable", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        Ast.Expression exp = null;
        if (peek("=")){
            match("=");
            exp = parseExpression();
            return new Ast.Global(name, false, Optional.of(exp));
        }
        else
            throw new ParseException("parse exception, no equals sign in immutable", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        return null;
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
        Ast.Expression temp1 = null;

        if(tokens.has(0)){
            temp1 = parseExpression();
            if(peek("=")){
                match("=");
                if(peek(";")){
                    throw new ParseException("parse exception, incomplete assignment", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
                return new Ast.Statement.Assignment(temp1, parseExpression());
            }
            if(!peek(";")){
                throw new ParseException("parse exception, no semicolon", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            else{
                match(";");
                return new Ast.Statement.Expression(temp1);
            }
        }
        throw new ParseException("parse exception, no input", 0);
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
        int binary = 0;
        Ast.Expression temp1 = null;
        String temp2 = "";
        Ast.Expression temp3 = null;
        Ast.Expression.Binary tempBin = null;

        if((tokens.tokens.size() - tokens.index) > 3){
            int off = 0;

            //loop to the last binary of equal level
            while(tokens.has(0)){
                off--;
                if(tokens.has(0) && (tokens.get(0).getLiteral() == "&&" || tokens.get(0).getLiteral() == "||")){
                    if(tokens.index + off >= 0){
                        temp1 = tempBin;
                        temp2 = tokens.get(0).getLiteral();
                        match(Token.Type.OPERATOR);
                        temp3 = parseComparisonExpression();
                        tempBin = new Ast.Expression.Binary(temp2, tempBin, temp3);
                        binary = 2;
                    }
                }
                else if(tokens.index + off < 0 && tokens.has(1) && (tokens.get(1).getLiteral() == "&&" || tokens.get(1).getLiteral() == "||") && tokens.has(3) && (tokens.get(3).getLiteral() == "&&" || tokens.get(3).getLiteral() == "||")){
                    temp1 = parseComparisonExpression();
                    temp2 = tokens.get(0).getLiteral();
                    match(Token.Type.OPERATOR);
                    temp3 = parseComparisonExpression();
                    tempBin = new Ast.Expression.Binary(temp2, temp1, temp3);
                }
                else if(binary==2){
                    return new Ast.Expression.Binary(temp2, temp1, temp3);
                }
                else{
                    break;
                }
            }

        }
        if(tokens.has(0)){
            temp1 = parseComparisonExpression();
            if(!tokens.has(0)){
                return temp1;
            }
        }
        if(peek("&&") || peek("||")){
            temp2 = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            binary++;
        }
        if(tokens.has(0) && peek(Token.Type.IDENTIFIER)){
            temp3 = parseComparisonExpression();
            binary++;
        }
        else if(tokens.has(0) && peek(Token.Type.OPERATOR)){
            return temp1;
        }
        if(binary==2){
            return new Ast.Expression.Binary(temp2, temp1, temp3);
        }

        return parseComparisonExpression();
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        int binary = 0;
        Ast.Expression temp1 = null;
        String temp2 = "";
        Ast.Expression temp3 = null;
        Ast.Expression.Binary tempBin = null;

        if((tokens.tokens.size() - tokens.index) > 3){
            int off = 0;

            //loop to the last binary of equal level
            while(tokens.has(0)){
                off--;
                if(tokens.has(0) && (tokens.get(0).getLiteral() == "==" || tokens.get(0).getLiteral() == "!=" || tokens.get(0).getLiteral() == ">" || tokens.get(0).getLiteral() == "<")){
                    if(tokens.index + off >= 0){
                        temp1 = tempBin;
                        temp2 = tokens.get(0).getLiteral();
                        match(Token.Type.OPERATOR);
                        temp3 = parseAdditiveExpression();
                        tempBin = new Ast.Expression.Binary(temp2, tempBin, temp3);
                        binary = 2;
                    }
                }
                else if(tokens.index + off < 0 && tokens.has(1) && (tokens.get(1).getLiteral() == "==" || tokens.get(1).getLiteral() == "!=" || tokens.get(1).getLiteral() == ">" || tokens.get(1).getLiteral() == "<") && tokens.has(3) && (tokens.get(3).getLiteral() == "==" || tokens.get(3).getLiteral() == "!=" || tokens.get(3).getLiteral() == ">" || tokens.get(3).getLiteral() == "<")){
                    temp1 = parseAdditiveExpression();
                    temp2 = tokens.get(0).getLiteral();
                    match(Token.Type.OPERATOR);
                    temp3 = parseAdditiveExpression();
                    tempBin = new Ast.Expression.Binary(temp2, temp1, temp3);
                }
                else if(binary==2){
                    return new Ast.Expression.Binary(temp2, temp1, temp3);
                }
                else{
                    break;
                }
            }

        }
        if(tokens.has(0)){
            temp1 = parseAdditiveExpression();
            if(!tokens.has(0)){
                return temp1;
            }
        }
        if(peek("==") || peek("!=")){
            temp2 = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            binary++;
        }
        else if(peek(">") || peek("<")){
            temp2 = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            binary++;
        }
        if(tokens.has(0) && peek(Token.Type.IDENTIFIER)){
            temp3 = parseAdditiveExpression();
            binary++;
        }
        else if(tokens.has(0) && peek(Token.Type.OPERATOR)){
            return temp1;
        }
        if(binary==2){
            return new Ast.Expression.Binary(temp2, temp1, temp3);
        }
        return parseAdditiveExpression();
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        int binary = 0;
        Ast.Expression temp1 = null;
        String temp2 = "";
        Ast.Expression temp3 = null;
        Ast.Expression.Binary tempBin = null;

        if((tokens.tokens.size() - tokens.index) > 3){
            int off = 0;

            //loop to the last binary of equal level
            while(tokens.has(0)){
                off--;
                if(tokens.has(0) && (tokens.get(0).getLiteral() == "+" || tokens.get(0).getLiteral() == "-")){
                    if(tokens.index + off >= 0){
                        temp1 = tempBin;
                        temp2 = tokens.get(0).getLiteral();
                        match(Token.Type.OPERATOR);
                        temp3 = parseMultiplicativeExpression();
                        tempBin = new Ast.Expression.Binary(temp2, tempBin, temp3);
                        binary = 2;
                    }
                }
                else if(tokens.index + off < 0 && tokens.has(1) && (tokens.get(1).getLiteral() == "+" || tokens.get(1).getLiteral() == "-") && tokens.has(3) && (tokens.get(3).getLiteral() == "+" || tokens.get(3).getLiteral() == "-")){
                    temp1 = parseMultiplicativeExpression();
                    temp2 = tokens.get(0).getLiteral();
                    match(Token.Type.OPERATOR);
                    temp3 = parseMultiplicativeExpression();
                    tempBin = new Ast.Expression.Binary(temp2, temp1, temp3);
                }
                else if(binary==2){
                    return new Ast.Expression.Binary(temp2, temp1, temp3);
                }
                else{
                    break;
                }
            }

        }
        if(tokens.has(0)){
            temp1 = parseMultiplicativeExpression();
            if(!tokens.has(0)){
                return temp1;
            }
        }
        if(peek("+") || peek("-")){
            temp2 = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            binary++;
        }
        if(tokens.has(0) && peek(Token.Type.IDENTIFIER)){
            temp3 = parseMultiplicativeExpression();
            binary++;
        }
        else if(tokens.has(0) && peek(Token.Type.OPERATOR)){
            return temp1;
        }
        if(binary==2){
            return new Ast.Expression.Binary(temp2, temp1, temp3);
        }
        return parseMultiplicativeExpression();
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        int binary = 0;
        Ast.Expression temp1 = null;
        String temp2 = "";
        Ast.Expression temp3 = null;
        Ast.Expression.Binary tempBin = null;

        if((tokens.tokens.size() - tokens.index) > 3){
            int off = 0;

            //loop to the last binary of equal level
            while(tokens.has(0)){
                off--;
                if(tokens.has(0) && (tokens.get(0).getLiteral() == "*" || tokens.get(0).getLiteral() == "/" || tokens.get(0).getLiteral() == "^")){
                    if(tokens.index + off >= 0){
                        temp1 = tempBin;
                        temp2 = tokens.get(0).getLiteral();
                        match(Token.Type.OPERATOR);
                        temp3 = parsePrimaryExpression();
                        tempBin = new Ast.Expression.Binary(temp2, tempBin, temp3);
                        binary = 2;
                    }
                }
                else if(tokens.index + off < 0 && tokens.has(1) && (tokens.get(1).getLiteral() == "*" || tokens.get(1).getLiteral() == "/" || tokens.get(1).getLiteral() == "^") && tokens.has(3) && (tokens.get(3).getLiteral() == "*" || tokens.get(3).getLiteral() == "/" || tokens.get(3).getLiteral() == "^")){
                    temp1 = parsePrimaryExpression();
                    temp2 = tokens.get(0).getLiteral();
                    match(Token.Type.OPERATOR);
                    temp3 = parsePrimaryExpression();
                    tempBin = new Ast.Expression.Binary(temp2, temp1, temp3);
                }
                else if(binary==2){
                    return new Ast.Expression.Binary(temp2, temp1, temp3);
                }
                else{
                    break;
                }
            }
        }
        if(tokens.has(0)){
            temp1 = parsePrimaryExpression();
            if(!tokens.has(0)){
                return temp1;
            }
        }
        if(peek("*") || peek("/") || peek("^")){
            temp2 = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            binary++;
        }
        if(tokens.has(0) && peek(Token.Type.IDENTIFIER)){
            temp3 = parsePrimaryExpression();
            binary++;
        }
        else if(tokens.has(0) && peek(Token.Type.OPERATOR)){
            return temp1;
        }
        if(binary==2){
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
        else if (tokens.has(0)){
            throw new ParseException("parse exception, not a primary", tokens.get(0).getIndex());
        }
        else {
            throw new ParseException("parse exception, not a primary", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
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
        s = replaceEscapes(s);
        match(Token.Type.CHARACTER);
        char c = s.charAt(0);
        return parseLiteral(c);
    }

    public Ast.Expression parseString() throws ParseException{
        String s = tokens.get(0).getLiteral().substring(1, tokens.get(0).getLiteral().length()-1);
        s = replaceEscapes(s);
        match(Token.Type.STRING);
        return parseLiteral(s);
    }

    public String replaceEscapes(String s){
        s = s.replace("\\n", "\n");
        s = s.replace("\\r", "\r");
        s = s.replace("\\t", "\t");
        s = s.replace("\\b", "\b");
        s = s.replace("\\f", "\f");
        s = s.replace("\\u000B", "\u000B");
        return s;
    }

    public Ast.Expression parseLiteral(Object obj){
        return new Ast.Expression.Literal(obj);
    }

    public Ast.Expression parseGroup(){
        match("(");
        Ast.Expression exp = new Ast.Expression.Group(parseExpression());

        if (!peek(")")){
            throw new ParseException("parse exception, unclosed group", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
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
            throw new ParseException("parse exception, unclosed access", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
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
                throw new ParseException("parse exception, invalid exfunc comma", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            exps.add(i, parseExpression());
            i++;
            if (peek(",")){
                match(",");
                if (peek(")")){
                    throw new ParseException("parse exception, invalid exfunc close", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }
            else if (!peek(")")){
                throw new ParseException("parse exception, unclosed exfunc", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
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