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
        print("public class Main {");
        newline(0);
        indent++;
        for (Ast.Global stmt : ast.getGlobals()){
            newline(indent);
            visit(stmt);
        }
        newline(indent);
        print("public static void main(String[] args) {");
        indent++;
        newline(indent);
        print("System.exit(new Main().main());");
        indent--;
        newline(indent);
        print("}");
        newline(0);
        for (Ast.Function stmt : ast.getFunctions()){
            newline(indent);
            visit(stmt);
        }
        newline(0);
        indent--;
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if (ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList) {
            print(ast.getVariable().getType().getJvmName() + "[] " + ast.getName() + " = ");
            visit(ast.getValue().get());
        }
        else {
            if (!ast.getMutable())
                print("final ");
            print(ast.getVariable().getType().getJvmName() + " " + ast.getName());
            if (ast.getValue().isPresent()) {
                print(" = " + ast.getValue().get());
            }
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        print(ast.getFunction().getReturnType().getJvmName() + " " + ast.getName() + "(");

        for (int i = 0; i < ast.getParameters().size(); i++) {
            if (i != 0)
                print(", ");
            print(ast.getFunction().getParameterTypes().get(i).getJvmName() + " " + ast.getParameters().get(i));
        }

        print(") {");
        indent++;

        for (Ast.Statement stmt : ast.getStatements()) {
            newline(indent);
            visit(stmt);
        }

        indent--;
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName() + " ");
        print(ast.getName());
        if(ast.getValue().isPresent()){
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");
        indent++;
        for (Ast.Statement stmt : ast.getThenStatements()){
            newline(indent);
            visit(stmt);
        }
        indent--;
        newline(indent);
        print("}");
        if(!ast.getElseStatements().isEmpty()){
            print(" else {");
            indent++;
            for (Ast.Statement stmt : ast.getElseStatements()){
                newline(indent);
                visit(stmt);
            }
            indent--;
            newline(indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (");
        visit(ast.getCondition());
        print(") {");
        indent++;
        for (Ast.Statement stmt : ast.getCases()){
            newline(indent);
            visit(stmt);
        }
        indent--;
        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if(ast.getValue().isPresent()){
            print("case ");
            visit(ast.getValue().get());
            print(":");
            indent++;
            for (Ast.Statement stmt : ast.getStatements()){
                newline(indent);
                visit(stmt);
            }
            newline(indent);
            print("break;");
            indent--;
        }
        else{
            print("default:");
            indent++;
            for (Ast.Statement stmt : ast.getStatements()){
                newline(indent);
                visit(stmt);
            }
            indent--;
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");
        if(ast.getStatements().isEmpty()){
            print("}");
        }
        else{
            indent++;
            for (Ast.Statement stmt : ast.getStatements()){
                newline(indent);
                visit(stmt);
            }
            indent--;
            newline(indent);
            print("}");
        }

        return null;
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
