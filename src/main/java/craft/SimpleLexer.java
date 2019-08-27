package craft;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimpleLexer {
	
	public static void main(String[] args) {
		SimpleLexer lexer = new SimpleLexer();
		
		String script = " age >= 45";
		System.out.println("parse:" + script);
		SimpleTokenReader tokenReader = lexer.tokenize(script);
		dump(tokenReader);

		script = " age > 45";
		System.out.println("parse:" + script);
		tokenReader = lexer.tokenize(script);
		dump(tokenReader);

		script = "int age = 45";
		System.out.println("parse:" + script);
		tokenReader = lexer.tokenize(script);
		dump(tokenReader);

		script = "inta age = 45";
		System.out.println("parse:" + script);
		tokenReader = lexer.tokenize(script);
		dump(tokenReader);

		script = "in age = 45";
		System.out.println("parse:" + script);
		tokenReader = lexer.tokenize(script);
		dump(tokenReader);
	}
	
	private StringBuffer tokenText = null;
	
	private List<Token> tokens = null;
	
	private SimpleToken token = null;
	
	private boolean isAlpha(int ch) {
		return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z';
	}
	
	private boolean isDigit(int ch) {
		return ch >= '0' && ch <= '9';
	}

	private boolean isBlank(int ch){
		return ch == ' ' || ch == '\t' || ch == '\n';
	}
	
	private DfaState initToken(char ch) {
		if(tokenText.length() > 0) {
			token.text = tokenText.toString();
			tokens.add(token);
			
			tokenText = new StringBuffer();
			token = new SimpleToken();
		}
		
		DfaState newState = DfaState.Initial;
		if(this.isAlpha(ch)) {	//第一个字符是字母
			if(ch == 'i'){
				newState =DfaState.Id_int1;
			}else {
				newState = DfaState.Id;//进入 Id状态
			}
			token.type = TokenType.Identifier;
			tokenText.append(ch);
		}else if(this.isDigit(ch)) {	// 第一个字符是数字
			newState = DfaState.IntLiteral;	//
			token.type = TokenType.IntLiteral;
			tokenText.append(ch);
		}else if(ch == '>') {
			newState = DfaState.GT;
			token.type = TokenType.GT;
			tokenText.append(ch);
		}else if(ch == '='){
			newState = DfaState.Assignment;
			token.type = TokenType.Assignment;
			tokenText.append(ch);
		}
		else {
			newState = DfaState.Initial;// skip all unknown pattern
		}
		
		
		return newState;
	}
	
	public SimpleTokenReader tokenize(String code) {
		tokens = new ArrayList<>();
		CharArrayReader reader = new CharArrayReader(code.toCharArray());
		tokenText = new StringBuffer();
		token = new SimpleToken();
		int ich = 0;
		char ch = 0;
		DfaState state = DfaState.Initial;
		
		try {
			while((ich = reader.read()) != -1) {
				ch = (char) ich;
				switch (state) {
				case Initial:
					state = this.initToken(ch);//重新确定后续状态
					break;
				case Id:
					if(this.isAlpha(ch) || this.isDigit(ch)) {
						tokenText.append(ch);	//	保持标识符状态
					}else {
						state = this.initToken(ch); //退出标识符状态，并保存 Token
					}
					break;
				case GT:
					if(ch == '=') {
						token.type = TokenType.GE; //装换成GE
						state = DfaState.GE;
						tokenText.append(ch);
					}else {
						state = this.initToken(ch); //退出GT，并保存Token
					}
					break;
				case GE:
				case Assignment:
					state = this.initToken(ch);	//退出当前状态,并保存 Token
					break;
				case IntLiteral:
					if(this.isDigit(ch)) {
						tokenText.append(ch); //继续保持在数字字面量状态
					}else {
						state = this.initToken(ch);//退出当前状态,并保存 Token
					}
					break;
				case Id_int1:
					if(ch == 'n'){
						state = DfaState.Id_int2;
						tokenText.append(ch);
					}else if(isDigit(ch) || isAlpha(ch)){
						state = DfaState.Id;	//切换为Id状态
						tokenText.append(ch);
					}else{
						state = initToken(ch);
					}
					break;
				case Id_int2:
					if(ch == 't'){
						state = DfaState.Id_int3;
						tokenText.append(ch);
					}else if(isDigit(ch) || isAlpha(ch)){
						state = DfaState.Id;	//切换为Id状态
						tokenText.append(ch);
					}else{
						state = initToken(ch);
					}
					break;
				case Id_int3:
					if(isBlank(ch)){
						token.type = TokenType.Int;
						state = initToken(ch);
					}else {
						state = DfaState.Id;	//切换为Id状态
						tokenText.append(ch);
					}
					break;
				default:
					break;
				}
			}
			// 把最后一个token送进去
			if(tokenText.length() > 0) {
				this.initToken(ch);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new SimpleTokenReader(tokens);
	}
	
	

	private final class SimpleToken implements Token{
		
		private TokenType type;

		private String text;
		
		@Override
		public TokenType getType() {
			return type;
		}

		@Override
		public String getText() {
			return text;
		}
	}
	
	/**
	 * 有限状态机的各种状态
	 * @author thewangzl
	 *
	 */
	private enum DfaState {
		Initial,
		
		Id_int1, Id_int2, Id_int3, Id, GT, GE,

		Assignment,

		IntLiteral
	}
	
	public static void dump(SimpleTokenReader tokenReader) {
		System.out.println("text\t\ttype");
		Token token = null;
		while((token = tokenReader.read()) != null) {
			System.out.println(token.getText() + "\t\t" + token.getType());
		}
	}
	
	/**
	 * 一个简单的Token流，是把一个Token列表进行了封装。
	 * @author thewangzl
	 *
	 */
	private class SimpleTokenReader implements TokenReader{

		List<Token> tokens;
		int pos = 0;
	
		public SimpleTokenReader(List<Token> tokens) {
			this.tokens = tokens;
		}

		@Override
		public Token read() {
			if(pos < tokens.size()) {
				return tokens.get(pos++);
			}
			return null;
		}

		@Override
		public Token peek() {
			if(pos < tokens.size()) {
				return tokens.get(pos);
			}
			return null;
		}

		@Override
		public void unread() {
			if(pos > 0) {
				pos--;
			}
		}

		@Override
		public int getPosition() {
			return pos;
		}

		@Override
		public void setPosition(int position) {
			if(position >= 0 && position < tokens.size() ) {
				pos = position;
			}
		}
		
	}
}
