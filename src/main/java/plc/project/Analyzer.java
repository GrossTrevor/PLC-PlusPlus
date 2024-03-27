package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        for (Ast.Expression exp : ast.getValues()){
            requireAssignable(ast.getType(), exp.getType());
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target == Environment.Type.COMPARABLE && !(type == Environment.Type.CHARACTER || type == Environment.Type.DECIMAL || type == Environment.Type.INTEGER || type == Environment.Type.STRING))
            throw new RuntimeException("runtime exception, illegal assignment to target == COMPARABLE");
        if (target == Environment.Type.CHARACTER && !(type == Environment.Type.CHARACTER))
            throw new RuntimeException("runtime exception, illegal assignment to target == CHARACTER");
        if (target == Environment.Type.DECIMAL && !(type == Environment.Type.DECIMAL))
            throw new RuntimeException("runtime exception, illegal assignment to target == DECIMAL");
        if (target == Environment.Type.INTEGER && !(type == Environment.Type.INTEGER))
            throw new RuntimeException("runtime exception, illegal assignment to target == INTEGER");
        if (target == Environment.Type.STRING && !(type == Environment.Type.STRING))
            throw new RuntimeException("runtime exception, illegal assignment to target == STRING");
        if (target == Environment.Type.BOOLEAN && !(type == Environment.Type.BOOLEAN))
            throw new RuntimeException("runtime exception, illegal assignment to target == BOOLEAN");
    }

}
