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
        ast.getGlobals().forEach(this::visit);
        for(Ast.Function func : ast.getFunctions()){
            visit(func);
        }

        Environment.Function main = scope.lookupFunction("main", 0);
        return main.invoke(List.of());
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        if(ast.getValue().isPresent()){
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        }
        else{
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            scope = new Scope(scope);

            List<Environment.PlcObject> params = new ArrayList();

            for (int i = 0; i < ast.getParameters().size(); i++) {
                scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
                params.add(scope.lookupVariable(ast.getParameters().get(i)).getValue());
            }

            Ast.Expression ret = null;
            for (Ast.Statement stmt : ast.getStatements()) {
                if (stmt instanceof Ast.Statement.Return) {
                    ret = (Ast.Expression) ((Ast.Statement.Return) new Return(visit(stmt)).value.getValue()).getValue();
                    return visit(ret);
                }
                else
                    visit(stmt);
            }
            return Environment.NIL;
        });

        //scope = scope.getParent();
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        return visit(ast.getExpression());
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
        if(ast.getReceiver() instanceof Ast.Expression.Access){
            Environment.Variable temp = scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName());
            if(temp.getMutable()){
                if(((Ast.Expression.Access) ast.getReceiver()).getOffset().isPresent()){
                    //it's a list
                    Object newVal = visit(ast.getValue()).getValue();
                    BigInteger offset = (BigInteger) ((Ast.Expression.Literal) ((Ast.Expression.Access) ast.getReceiver()).getOffset().get()).getLiteral();
                    List<Object> vals = (List<Object>) temp.getValue().getValue();
                    vals.set(offset.intValue(), newVal);
                }
                else{
                    temp.setValue(visit(ast.getValue()));
                }
                return Environment.NIL;
            }
            throw new RuntimeException("variable is not mutable");
        }
        throw new RuntimeException("Variable is not an Ast.Expression.Access");
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        if(requireType(Boolean.class, visit(ast.getCondition()))){
            try{
                scope = new Scope(scope);
                ast.getThenStatements().forEach(this::visit);
            }
            finally{
                scope = scope.getParent();
            }
        }
        else if(!requireType(Boolean.class, visit(ast.getCondition()))){
            try{
                scope = new Scope(scope);
                ast.getElseStatements().forEach(this::visit);
            }
            finally{
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        boolean found = false;
        Environment.Variable temp = scope.lookupVariable(((Ast.Expression.Access) ast.getCondition()).getName());
        Ast.Statement.Case last_case = new Ast.Statement.Case(Optional.empty(),new ArrayList<>());

        for(Ast.Statement.Case stmt : ast.getCases()){
            if(stmt.getValue().isPresent()){
                if(temp.getValue().getValue().equals(((Ast.Expression.Literal) stmt.getValue().get()).getLiteral())){
                    visit(stmt);
                    found = true;
                    break;
                }
            }
            last_case = stmt;
        }

        if(!found){
            //run default
            visit(last_case);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        try{
            scope = new Scope(scope);
            ast.getStatements().forEach(this::visit);
        }
        finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while(requireType(Boolean.class, visit(ast.getCondition()))){
            try{
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
//                for(Ast.Statement stmt : ast.getStatements()){
//                    visit(stmt);
//                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        return new Return(Environment.create(ast)).value;
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
        return visit(ast.getExpression());
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
            //if left is true and OR
            if(requireType(Boolean.class, visit(ast.getLeft())) && ast.getOperator().equals("||")){
                return Environment.create(true);
            }
            //if left is true and AND
            else if(requireType(Boolean.class, visit(ast.getLeft())) && ast.getOperator().equals("&&")){
                //right is true
                if(requireType(Boolean.class, visit(ast.getRight()))){
                    return Environment.create(true);
                }
                //right is false
                else if(!requireType(Boolean.class, visit(ast.getRight()))){
                    return Environment.create(false);
                }
                //right is not bool
                else{
                    throw new RuntimeException("Right side of binary is not boolean");
                }
            }
            //if left is false and OR
            else if(!requireType(Boolean.class, visit(ast.getLeft())) && ast.getOperator().equals("||")){
                //if right is true
                if(requireType(Boolean.class, visit(ast.getRight()))){
                    return Environment.create(true);
                }
                //if right is false
                else if(!requireType(Boolean.class, visit(ast.getRight()))){
                    return Environment.create(false);
                }
                //right is not bool
                else{
                    throw new RuntimeException("Right side of binary is not boolean");
                }
            }
            //if left is false and AND
            else if(!requireType(Boolean.class, visit(ast.getLeft())) && ast.getOperator().equals("&&")){
                //if right is bool
                if(requireType(Boolean.class, visit(ast.getRight())) || !requireType(Boolean.class, visit(ast.getRight()))){
                    return Environment.create(false);
                }
                //right is not bool
                else{
                    throw new RuntimeException("Right side of binary is not boolean");
                }
            }
            // left is not a bool
            else{
                throw new RuntimeException("Left side of binary is not boolean");
            }
        }
        else if(ast.getOperator().equals(">") || ast.getOperator().equals("<")){
            if(Comparable.class.isInstance(visit(ast.getLeft()).getValue()) && Comparable.class.isInstance(visit(ast.getRight()).getValue())){
                // both sides are the same type of class
                Comparable temp1;
                Comparable temp2;

                try{
                    temp1 = requireType(Comparable.class, visit(ast.getLeft()));
                    temp2 = requireType(Comparable.class, visit(ast.getRight()));
                }
                catch(Exception e){
                    throw new RuntimeException("sides are not the same type");
                }

                if(temp1.compareTo(temp2) < 0){
                    if(ast.getOperator().equals(">")){
                        return Environment.create(false);
                    }
                    else{
                        return Environment.create(true);
                    }
                }
                else if(temp1.compareTo(temp2) > 0){
                    if(ast.getOperator().equals(">")){
                        return Environment.create(true);
                    }
                    else{
                        return Environment.create(false);
                    }
                }
                else{
                    return Environment.create(false);
                }
            }
            else{
                throw new RuntimeException("arguments aren't comparable");
            }
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
            if(String.class.isInstance(visit(ast.getLeft()).getValue())){
                String temp1 = requireType(String.class, visit(ast.getLeft()));
                String temp2 = visit(ast.getRight()).getValue().toString();
                return Environment.create(temp1 + temp2);
            }
            else if(String.class.isInstance(visit(ast.getRight()).getValue())){
                String temp1 = visit(ast.getLeft()).getValue().toString();
                String temp2 = requireType(String.class, visit(ast.getRight()));
                return Environment.create(temp1 + temp2);
            }
            else if(BigInteger.class.isInstance(visit(ast.getLeft()).getValue()) && BigInteger.class.isInstance(visit(ast.getRight()).getValue())){
                BigInteger temp1 = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger temp2 = requireType(BigInteger.class, visit(ast.getRight()));
                return Environment.create(temp1.add(temp2));
            }
            else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue()) && BigDecimal.class.isInstance(visit(ast.getRight()).getValue())){
                BigDecimal temp1 = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal temp2 = requireType(BigDecimal.class, visit(ast.getRight()));
                return Environment.create(temp1.add(temp2));
            }
            else{
                throw new RuntimeException("types of the sides of the equation do not match");
            }
        }
        else if(ast.getOperator().equals("-")){
            if(BigInteger.class.isInstance(visit(ast.getLeft()).getValue()) && BigInteger.class.isInstance(visit(ast.getRight()).getValue())){
                BigInteger temp1 = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger temp2 = requireType(BigInteger.class, visit(ast.getRight()));
                return Environment.create(temp1.subtract(temp2));
            }
            else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue()) && BigDecimal.class.isInstance(visit(ast.getRight()).getValue())){
                BigDecimal temp1 = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal temp2 = requireType(BigDecimal.class, visit(ast.getRight()));
                return Environment.create(temp1.subtract(temp2));
            }
            else{
                throw new RuntimeException("types of the sides of the equation do not match");
            }
        }
        else if(ast.getOperator().equals("*")){
            if(BigInteger.class.isInstance(visit(ast.getLeft()).getValue()) && BigInteger.class.isInstance(visit(ast.getRight()).getValue())){
                BigInteger temp1 = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger temp2 = requireType(BigInteger.class, visit(ast.getRight()));
                return Environment.create(temp1.multiply(temp2));
            }
            else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue()) && BigDecimal.class.isInstance(visit(ast.getRight()).getValue())){
                BigDecimal temp1 = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal temp2 = requireType(BigDecimal.class, visit(ast.getRight()));
                return Environment.create(temp1.multiply(temp2));
            }
            else{
                throw new RuntimeException("types of the sides of the equation do not match");
            }
        }
        else if(ast.getOperator().equals("/")){
            if(BigInteger.class.isInstance(visit(ast.getLeft()).getValue()) && BigInteger.class.isInstance(visit(ast.getRight()).getValue())){
                BigInteger temp1 = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger temp2 = requireType(BigInteger.class, visit(ast.getRight()));
                if(temp2.equals(BigInteger.ZERO)){
                    throw new RuntimeException("cannot divide by zero");
                }
                return Environment.create(temp2.divide(temp1));
            }
            else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue()) && BigDecimal.class.isInstance(visit(ast.getRight()).getValue())){
                BigDecimal temp1 = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal temp2 = requireType(BigDecimal.class, visit(ast.getRight()));
                if(temp2.equals(BigDecimal.ZERO)){
                    throw new RuntimeException("cannot divide by zero");
                }
                System.out.println(temp1 + " and " + temp2);
                return Environment.create(temp1.divide(temp2, RoundingMode.HALF_EVEN));
            }
            else{
                throw new RuntimeException("types of the sides of the equation do not match");
            }
        }
        else if(ast.getOperator().equals("^")){
            if(BigInteger.class.isInstance(visit(ast.getLeft()).getValue()) && BigInteger.class.isInstance(visit(ast.getRight()).getValue())){
                BigInteger temp1 = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger temp2 = requireType(BigInteger.class, visit(ast.getRight()));

                //BigInteger result = BigInteger.ONE;
                // temp2 > 0
                if(temp2.compareTo(BigInteger.ZERO) > 0){
                    while(!temp2.equals(BigInteger.ONE)){
                        temp1 = temp1.multiply(temp1);
                        temp2 = temp2.subtract(BigInteger.ONE);
                    }
                    return Environment.create(temp1);
                }
                // temp2 < 0
                else if(temp2.compareTo(BigInteger.ZERO) < 0){
                    while(!temp2.equals(BigInteger.ONE.negate())){
                        temp1 = temp1.multiply(temp1);
                        temp2 = temp2.add(BigInteger.ONE);
                    }
                    temp1 = BigInteger.ONE.divide(temp1);
                    return Environment.create(temp1);
                }
            }
        }
        throw new RuntimeException("operator is not the right type");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Environment.Variable var = scope.lookupVariable(ast.getName());
        if (ast.getOffset().equals(Optional.empty())) {
            return var.getValue();
        }
        else {
            Ast.Expression.Literal lit = (Ast.Expression.Literal) ast.getOffset().get();
            List arr = (List) var.getValue().getValue();
            return Environment.create(arr.get(((BigInteger) lit.getLiteral()).intValue()));
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        int arity = ast.getArguments().size();
        Environment.Function function = scope.lookupFunction(ast.getName(), arity);

        List<Environment.PlcObject> args = new ArrayList();

        for (Ast.Expression exp : ast.getArguments())
            args.add(visit(exp));

        return function.invoke(args);
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
