package craft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一个点单的语法解析器。
 * 能够解析简单的表达式、变量声明和初始化语句、赋值语句。
 * 它支持的语法规则为：
 * program -> intDeclare | expressionStatement | assignmentStatement
 * intDeclare -> 'int' Id ( = additive) ';'
 * expressionStatement -> additive ';'
 * additive -> multiplicative ( + | -) muliplicative)*
 * multiplicative -> primary ( (* | /) primary)*
 * primary -> IntLiteral | Id | (additive)
 */
public class SimpleParser {

    public static void main(String[] args){
        SimpleParser parser = new SimpleParser();
        String script = null;
        ASTNode tree = null;

        try {
            script = "int age = 45 + 2; age = 20; age + 10 *2;";
            System.out.println("解析：" + script);
            tree = parser.parse(script);
            parser.dumpAST(tree, "");
        }catch (Exception e){
            System.err.println(e.getMessage());
        }

        //测试异常语法
        try {
            script = "2+3+;";
            System.out.println("解析：" + script);
            tree = parser.parse(script);
            parser.dumpAST(tree, "");
        }catch (Exception e){
            System.err.println(e.getMessage());
        }

        //测试异常语法
        try {
            script = "2+3*;";
            System.out.println("解析：" + script);
            tree = parser.parse(script);
            parser.dumpAST(tree, "");
        }catch (Exception e){
            System.err.println(e.getMessage());
        }

    }



    /**
     * 解析脚本，并返回根节点
     * @param code
     * @return
     */
    public ASTNode parse(String code) throws Exception {
        SimpleLexer lexer = new SimpleLexer();
        TokenReader tokens = lexer.tokenize(code);

        ASTNode rootNode = this.prog(tokens);
        return rootNode;
    }

    /**
     * 解析的入口
     * @param tokens
     * @return
     * @throws Exception
     */
    private SimpleASTNode prog(TokenReader tokens) throws Exception {
        SimpleASTNode node = new SimpleASTNode(ASTNodeType.Program, "pwc");

        while (tokens.peek() != null){
            SimpleASTNode child = this.intDeclare(tokens);

            if(child == null){
                child = this.expressionStatement(tokens);
            }
            if(child == null){
                child = this.assignmentStatement(tokens);
            }
            if(child != null){
                node.addChild(child);
            }else{
                throw new Exception("unknown statement");
            }
        }
        return node;
    }

    /**
     * 表达式语句，即表达式后面跟个分号。
     * @param tokens
     * @return
     * @throws Exception
     */
    private SimpleASTNode expressionStatement(TokenReader tokens) throws Exception{
        int pos = tokens.getPosition();     //记下初始位置
        SimpleASTNode node = this.additive(tokens);     //匹配加法规则
        if(node != null){
            Token token = tokens.peek();
            if(token != null && token.getType() == TokenType.SemiColon){    //要求一定要以分号结尾
                tokens.read();
            }else{
                node = null;
                tokens.setPosition(pos);
            }
        }
        return node;    //  直接返回子节点，简化了AST
    }

    /**
     * 赋值语句
     * @param tokens
     * @return
     * @throws Exception
     */
    private SimpleASTNode assignmentStatement(TokenReader tokens) throws Exception {
        SimpleASTNode node = null;
        Token token = tokens.peek();    //预读，看看下面是不是标识符
        if(token != null && token.getType() == TokenType.Identifier){
            token = tokens.read();  //读入标识符
            node = new SimpleASTNode(ASTNodeType.AssignmentStmt,token.getText());
            token = tokens.peek();  //预读，看看下面是不是等号
            if(token != null && token.getType() == TokenType.Assignment){
                tokens.read();  //取出等号
                SimpleASTNode child = this.additive(tokens);
                if(child == null){  //出错，等号右边没有一个合法的表达式
                    throw new Exception("invaid assignment statememt, expecting an expression");
                }else{
                    node.addChild(child);       //添加子节点
                    token = tokens.peek();      //预读，看看后面是不是分号
                    if(token != null && token.getType() == TokenType.SemiColon){
                        tokens.read();
                    }else{          //缺少分号，报错
                        throw new Exception("invalid statement, expecting semicolon");
                    }
                }
            }
        }else{
            tokens.unread();        //回溯，吐出之前消化掉的标识符
            node = null;
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
                    if(child2 != null) {
                        node = new SimpleASTNode(ASTNodeType.Additive, token.getText());
                        node.addChild(child1);          //注意，新节点在顶层，保证正确的结合性
                        node.addChild(child2);
                        child1 = node;
                    }else{
                        throw new Exception("invalid additive expression, expecting the right part.");
                    }
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
                if(child2 != null){
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
    public void dumpAST(ASTNode node, String indent){
        System.out.println(indent + node.getType() + " " + node.getText());
        for(ASTNode child : node.getChildren()){
            this.dumpAST(child, indent + "\t");
        }
    }



}
