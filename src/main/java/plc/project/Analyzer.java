package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
        String name = ast.getName();
        List<Environment.Type> typesList = new ArrayList<>();
        Environment.Type returnType = Environment.Type.NIL;

        for (String type : ast.getParameterTypeNames())
            typesList.add(Environment.getType(type));
        if (ast.getReturnTypeName().isPresent())
            returnType = Environment.getType(ast.getReturnTypeName().get());

        ast.setFunction(scope.defineFunction(name, name, typesList, returnType, args -> Environment.NIL));
        function = ast;

        scope = new Scope(scope);

        for (Ast.Statement stmt : ast.getStatements()) {
            visit(stmt);
            if (stmt instanceof Ast.Statement.Return)
                requireAssignable(returnType, ((Ast.Statement.Return) stmt).getValue().getType());
        }
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        if(!(ast.getExpression() instanceof Ast.Expression.Function)){
            throw new RuntimeException("expected ast.expression.function");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if(ast.getValue().isPresent()){
            visit(ast.getValue().get());
        }
        if(ast.getTypeName().isPresent()){
            if(ast.getValue().isPresent()){
                requireAssignable(Environment.getType(ast.getTypeName().get()), ast.getValue().get().getType());
            }
            ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName().get()),true, Environment.NIL));
        }
        else if(ast.getValue().isPresent())
            ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(),true, Environment.NIL));
        else
            throw new RuntimeException("missing variable type or initial condition");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        visit(ast.getValue());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        if(!(ast.getReceiver() instanceof Ast.Expression.Access)){
            throw new RuntimeException("expected ast.expression.access");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        if(ast.getThenStatements().equals(Arrays.asList())){
            throw new RuntimeException("if statement does not have the correct format");
        }
        try{
            scope = new Scope(scope);
            for(Ast.Statement stmt : ast.getThenStatements()){
                visit(stmt);
            }
            for(Ast.Statement stmt : ast.getElseStatements()){
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }
        return null;
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
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try{
            scope = new Scope(scope);
            for(Ast.Statement stmt :ast.getStatements()){
                visit(stmt);
            }
        }
        finally{
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        requireAssignable(function.getFunction().getReturnType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() instanceof Boolean){
            ast.setType(Environment.Type.BOOLEAN);
            return null;
        }
        if (ast.getLiteral() instanceof Character){
            ast.setType(Environment.Type.CHARACTER);
            return null;
        }
        if (ast.getLiteral() instanceof String){
            ast.setType(Environment.Type.STRING);
            return null;
        }
        if (ast.getLiteral() instanceof BigInteger) {
            if ((((BigInteger) ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MAX_VALUE))) == 1)
                throw new RuntimeException("runtime exception, big integer value greater than integer max value");
            ast.setType(Environment.Type.INTEGER);
            return null;
        }
        if (ast.getLiteral() instanceof BigDecimal) {
            if ((((BigDecimal) ast.getLiteral()).compareTo(BigDecimal.valueOf(Double.MAX_VALUE))) == 1)
                throw new RuntimeException("runtime exception, big decimal value greater than integer max value");
            ast.setType(Environment.Type.DECIMAL);
            return null;
        }
        ast.setType(Environment.Type.NIL);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Binary))
            throw new RuntimeException("runtime exception, group expression not a binary");
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        if(ast.getOperator().equals("&&") || ast.getOperator().equals("||")){
            if(ast.getLeft().getType().equals(Environment.Type.BOOLEAN) && ast.getRight().getType().equals(Environment.Type.BOOLEAN))
                ast.setType(Environment.Type.BOOLEAN);
            else
                throw new RuntimeException("expected boolean for && and ||");
        }
        else if(ast.getOperator().equals(">") || ast.getOperator().equals("<") || ast.getOperator().equals("==") || ast.getOperator().equals("!=")){
            requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
            requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(ast.getOperator().equals("+")){
            if(ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING))
                ast.setType(Environment.Type.STRING);
            else if(ast.getLeft().getType().equals(Environment.Type.INTEGER) && ast.getRight().getType().equals(Environment.Type.INTEGER))
                ast.setType(Environment.Type.INTEGER);
            else if(ast.getLeft().getType().equals(Environment.Type.DECIMAL) && ast.getRight().getType().equals(Environment.Type.DECIMAL))
                ast.setType(Environment.Type.DECIMAL);
            else
                throw new RuntimeException("expected string, integer, or decimal for +");
        }
        else if(ast.getOperator().equals("-") || ast.getOperator().equals("*") || ast.getOperator().equals("/")){
            if(ast.getLeft().getType().equals(Environment.Type.INTEGER) && ast.getRight().getType().equals(Environment.Type.INTEGER))
                ast.setType(Environment.Type.INTEGER);
            else if(ast.getLeft().getType().equals(Environment.Type.DECIMAL) && ast.getRight().getType().equals(Environment.Type.DECIMAL))
                ast.setType(Environment.Type.DECIMAL);
            else
                throw new RuntimeException("expected integer or decimal for -, *, and /");
        }
        else if(ast.getOperator().equals("^")){
            if(ast.getLeft().getType().equals(Environment.Type.INTEGER) && ast.getRight().getType().equals(Environment.Type.INTEGER))
                ast.setType(Environment.Type.INTEGER);
            else
                throw new RuntimeException("expected integer for ^");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isPresent() && ast.getOffset().get().getType() != Environment.Type.INTEGER)
            throw new RuntimeException("runtime exception, offset of access not an integer");
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        for (Ast.Expression exp : ast.getArguments())
            visit(exp);
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
        return null;
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
