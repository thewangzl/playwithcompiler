package craft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleCalculator {

    public static void main(String[] args){
        SimpleCalculator calculator = new SimpleCalculator();

        //测试变量声明语句的解析
        String script = "int a = b+3;";
        System.out.println("解析变量声明语句："+ script);
        SimpleLexer lexer = new SimpleLexer();
        TokenReader tokens = lexer.tokenize(script);
        try {
            SimpleASTNode node = calculator.intDeclare(tokens);
            calculator.dumpAST(node, "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //测试表达式
        script = "2+3*5";
        System.out.println("\n计算: "+ script + "，看上去一切正常。");
        calculator.evaluate(script);

        //测试语法错误
        script = "2+";
        try {
            calculator.evaluate(script);
        }catch (Exception e){
            System.err.println("\n计算："+ script + "，语法错误:" + e.getMessage());
        }

        script = "2+3+4+5";
        System.out.println("\n计算："+ script + "，结合性问题已经修复。");
        calculator.evaluate(script);
    }

    public void evaluate(String script) throws RuntimeException {
        try{
            ASTNode tree = this.parse(script);

            this.dumpAST(tree, "");
            this.evaluate(tree, "");
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * 对某个AST节点求值，并打印求值过程。
     * @param node
     * @param indent
     * @return
     */
    private int evaluate(ASTNode node, String indent){
        int result = 0;
        System.out.println(indent + "Calculating: " + node.getType());
        switch (node.getType()){
            case Program:
                for(ASTNode child : node.getChildren()){
                    result = this.evaluate(child, indent + "\t");
                }
                break;
            case Additive:
                ASTNode child1 = node.getChildren().get(0);
                int value1 = this.evaluate(child1, indent + "\t");
                ASTNode child2 = node.getChildren().get(1);
                int value2 = this.evaluate(child2, indent + "\t");
                if(node.getText().equalsIgnoreCase("+")){
                    result = value1 + value2;
                }else {
                    result = value1 - value2;
                }
                break;
            case Multiplicative:
                child1 = node.getChildren().get(0);
                value1 = this.evaluate(child1, indent + "\t");
                child2 = node.getChildren().get(1);
                value2 = this.evaluate(child2, indent + "\t");
                if(node.getText().equalsIgnoreCase("*")){
                    result = value1 * value2;
                }else {
                    result = value1 / value2;
                }
                break;
            case IntLiteral:
                result = Integer.valueOf(node.getText()).intValue();
                break;
            default:
        }
        System.out.println(indent + "Result:" + result);
        return result;
    }

    /**
     * 解析脚本，并返回根节点
     * @param code
     * @return
     */
    private ASTNode parse(String code) throws Exception {
        SimpleLexer lexer = new SimpleLexer();
        TokenReader tokens = lexer.tokenize(code);

        ASTNode rootNode = this.prog(tokens);
        return rootNode;
    }

    /**
     * 语法解析：根节点
     * @param tokens
     * @return
     * @throws Exception
     */
    private SimpleASTNode prog(TokenReader tokens) throws Exception {
        SimpleASTNode node = new SimpleASTNode(ASTNodeType.Program, "Calculator");

        SimpleASTNode child = this.additive(tokens);
        if(child != null){
            node.addChild(child);
        }
        return node;
    }

    /**
     * 整型变量声明语句，如：
     * int a;
     * int b = 2* 3
     *
     * @param tokens
     * @return
     * @throws Exception
     */
    private SimpleASTNode intDeclare(TokenReader tokens) throws Exception {
        SimpleASTNode node = null;
        Token token = tokens.peek();    //预读
        if(token != null && token.getType() == TokenType.Int){ //匹配int
            token = tokens.read();  //消耗掉int
            if(tokens.peek().getType() == TokenType.Identifier){    //匹配标识符
                token = tokens.read();  //消耗掉标识符
                //创建当前节点，并把变量名记到AST节点的文本值中
                node = new SimpleASTNode(ASTNodeType.IntDeclaration, token.getText());
                token = tokens.peek();  //预读
                if(token != null && token.getType() == TokenType.Assignment){
                    tokens.read();  //消耗掉等号
                    SimpleASTNode child = this.additive(tokens);    //消耗一个表达式
                    if(child == null){
                        throw new Exception("invalide variable initialization,expecting an expression");
                    }else{
                        node.addChild(child);
                    }
                }
            }else{
                throw new Exception("variable name expected");
            }
            if(node != null){
                token = tokens.peek();
                if(token != null && token.getType() == TokenType.SemiColon){
                    tokens.read();
                }else{
                    throw new Exception("invalid statement, expecting semicolon");
                }
            }
        }
        return node;
    }

    /**
     * 语法解析：加法表达式
     * 规则:add -> mul (+ mul)*
     * 伪代码:mul();
     *       while(next token is +){
     *          mul()
     *          createAddNode
     *      }
     * @param tokens
     * @return
     */
    private SimpleASTNode additive(TokenReader tokens) throws Exception {
        SimpleASTNode child1 = this.multiplicative(tokens); //计算第一个子节点
        SimpleASTNode node = child1;    //
        if(child1 != null){
            while (true){           //循环应用 add'
                Token token = tokens.peek();
                if(token != null && (token.getType() == TokenType.Plus || token.getType() == TokenType.Minus)){
                    token = tokens.read();      //读出加号
                    SimpleASTNode child2 = this.multiplicative(tokens);       //计算下级节点
                    node = new SimpleASTNode(ASTNodeType.Additive, token.getText());
                    node.addChild(child1);          //注意，新节点在顶层，保证正确的结合性
                    node.addChild(child2);
                    child1 = node;
                }else{
                    break;
                }
            }
        }
        return node;
    }

    /**
     * 语法解析：乘法表达式
     * @param tokens
     * @return
     */
    private SimpleASTNode multiplicative(TokenReader tokens) throws Exception {
        SimpleASTNode child1 = this.primary(tokens);
        SimpleASTNode node = child1;

        Token token = tokens.peek();
        if(child1 != null && token != null){
            if(token.getType() == TokenType.Star || token.getType() == TokenType.Slash){
                token = tokens.read();
                SimpleASTNode child2 = this.primary(tokens);
                if(child1 != null){
                    node = new SimpleASTNode(ASTNodeType.Multiplicative, token.getText());
                    node.addChild(child1);
                    node.addChild(child2);
                }else{
                    throw new Exception("invalid multiplicative expression, expecting the right parts.");
                }
            }
        }
        return node;
    }

    /**
     * 语法解析：基础表达式
     * @param tokens
     * @return
     */
    private SimpleASTNode primary(TokenReader tokens) throws Exception{
        SimpleASTNode node = null;
        Token token = tokens.peek();
        if(token != null){
            if(token.getType() == TokenType.IntLiteral){
                token = tokens.read();
                node = new SimpleASTNode(ASTNodeType.IntLiteral, token.getText());
            } else if(token.getType() == TokenType.Identifier){
                token = tokens.read();
                node = new SimpleASTNode(ASTNodeType.Identifier, token.getText());
            } else if(token.getType() == TokenType.LeftParen){
                tokens.read();
                node = additive(tokens);
                if(node != null){
                    token = tokens.peek();
                    if(token != null && token.getType() == TokenType.RightParen){
                        tokens.read();
                    }else {
                        throw new Exception("expecting right parenthesis");
                    }
                }else{
                    throw new Exception("expecting an additive expression inside parenthesis");
                }
            }
        }
        return node;    //这个方法也做了AST的简化，就是不用构造一个primary节点，直接返回子节点。因为它只有一个子节点。
    }

    /**
     * 一个点单的AST节点的实现
     * 属性包括：类型、文本值、父节点、子节点。
     */
    private class SimpleASTNode implements  ASTNode{
        SimpleASTNode parent= null;
        List<ASTNode> children = new ArrayList<>();
        List<ASTNode> readonlyChildren = Collections.unmodifiableList(children);
        ASTNodeType nodeType = null;
        String text = null;

        public SimpleASTNode(ASTNodeType nodeType, String text){
            this.nodeType = nodeType;
            this.text = text;
        }

        @Override
        public ASTNode getParent() {
            return parent;
        }

        @Override
        public List<ASTNode> getChildren() {
            return readonlyChildren;
        }

        @Override
        public ASTNodeType getType() {
            return nodeType;
        }

        @Override
        public String getText() {
            return text;
        }

        public void addChild(SimpleASTNode child){
            children.add(child);
            child.parent = this;
        }
    }

    /**
     * 打印输出AST的树状结构
     * @param node
     * @param indent 缩进字符，由tab组成，每一级多一个tab
     */
    private void dumpAST(ASTNode node, String indent){
        System.out.println(indent + node.getType() + " " + node.getText());
        for(ASTNode child : node.getChildren()){
            this.dumpAST(child, indent + "\t");
        }
    }



}
