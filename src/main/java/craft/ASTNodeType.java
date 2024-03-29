package craft;

/**
 * AST节点的类型
 */
public enum ASTNodeType {

    Program,    //程序入口

    IntDeclaration,     //整型变量声明
    ExpressionStmt,     //表达式语句，即表达式后面跟个分号
    AssignmentStmt,     //赋值语句

    Primary,            //基础表达式
    Multiplicative,     //乘法表达式
    Additive,           //加法表达式

    Identifier,         //标志符
    IntLiteral          //整型字面量

}
