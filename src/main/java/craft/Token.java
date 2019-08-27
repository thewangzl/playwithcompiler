package craft;

/**
 * 一个简单的Token.
 * 只有
 * @author thewangzl
 *
 */
public interface Token {

	/**
	 * Token的类型
	 * @return
	 */
	public TokenType getType();
	
	/**
	 * Token的文本值
	 * @return
	 */
	public String getText();
}
