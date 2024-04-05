package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() instanceof Boolean)
            print(ast.getLiteral().toString().toLowerCase());
        if (ast.getLiteral() instanceof Character)
            print("'" + ast.getLiteral() + "'");
        if (ast.getLiteral() instanceof String)
            print("\"" + ast.getLiteral() + "\"");
        if (ast.getLiteral() instanceof BigInteger)
            print(((BigInteger) ast.getLiteral()).toString());
        if (ast.getLiteral() instanceof BigDecimal)
            print(((BigDecimal) ast.getLiteral()).toString());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if(ast.getOperator().equals("^")){
            print("Math.pow(");
            visit(ast.getLeft());
            print(", ");
            visit(ast.getRight());
            print(")");
        }
        else{
            visit(ast.getLeft());
            print(" ");
            print(ast.getOperator());
            print(" ");
            visit(ast.getRight());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());
        if(ast.getOffset().isPresent()){
            print("[");
            print(ast.getOffset());
            print("]");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName());
        print("(");
        for (Ast.Expression exp : ast.getArguments()){
            if (!exp.equals(ast.getArguments().getFirst()))
                print(", ");
            visit(exp);
        }
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");
        for (Ast.Expression exp : ast.getValues()) {
            if (!exp.equals(ast.getValues().getFirst()))
                print(", ");
            visit(exp);
        }
        print("}");
        return null;
    }

}
