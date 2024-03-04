package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if(ast.getValue().isPresent()){
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        }
        else{
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        //get list to work

        if(ast.getReceiver() instanceof Ast.Expression.Access){
            Environment.Variable temp = scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName());
            if(temp.getMutable()){
                if(((Ast.Expression.Access) ast.getReceiver()).getOffset().isPresent()){
                    //it's a list
                    BigInteger newVal = (BigInteger) visit(ast.getValue()).getValue();
                    BigInteger offset = (BigInteger) ((Ast.Expression.Literal) ((Ast.Expression.Access) ast.getReceiver()).getOffset().get()).getLiteral();
                    List vals = (List) temp.getValue().getValue();
                    vals.set(offset.intValue(), newVal.intValue());
                }
                scope.defineVariable(temp.getName(), true, visit(ast.getValue()));
                return Environment.NIL;
            }
            throw new RuntimeException("variable is not mutable");
        }
        throw new RuntimeException("Variable is not an Ast.Expression.Access");
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(Environment.create(ast));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null){
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        if(ast.getLeft() instanceof Ast.Expression.Binary){
            visit(ast.getLeft());
        }
        if(ast.getRight() instanceof Ast.Expression.Binary){
            visit(ast.getRight());
        }
        else if(ast.getOperator().equals("&&") || ast.getOperator().equals("||")){

        }
        else if(ast.getOperator().equals(">") || ast.getOperator().equals("<")){

        }
        else if(ast.getOperator().equals("==") || ast.getOperator().equals("!=")){
            if(ast.getLeft().equals(ast.getRight())){
                if(ast.getOperator().equals("==")){
                    return Environment.create(true);
                }
                return Environment.create(false);
            }
            if(ast.getOperator().equals("!=")){
                return Environment.create(true);
            }
            return Environment.create(false);
        }
        else if(ast.getOperator().equals("+")){

        }
        else if(ast.getOperator().equals("-") || ast.getOperator().equals("*")){

        }
        else if(ast.getOperator().equals("/")){

        }
        else if(ast.getOperator().equals("^")){

        }
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Environment.Variable var = scope.lookupVariable(ast.getName());
        if (ast.getOffset().equals(Optional.empty())){
            return var.getValue();
        }
        else{
            Ast.Expression.Literal lit = (Ast.Expression.Literal) ast.getOffset().get();
            List arr = (List) var.getValue().getValue();
            return Environment.create(arr.get(((BigInteger) lit.getLiteral()).intValue()));
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> valsList = ast.getValues();
        List<Object> vals = new ArrayList<>();

        for (Ast.Expression i : valsList) {
            vals.add(((Ast.Expression.Literal) i).getLiteral());
        }

        return Environment.create(vals);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        public Return(Environment.PlcObject value) {
            this.value = value;
        }
        //return to private?
    }

}
