package cn.schoolwow.quickhttp.document.parse;

public class HTMLToken {
    public int start;
    public int end;
    public String value;
    public TokenType tokenType;

    public String toString() {
        return value.replaceAll("\r\n", "换行符") + "[" + tokenType.name + "]";
    }

    public enum TokenType {
        openTag("开始标签"),
        tagName("标签名称"),
        attribute("标签属性"),
        openTagClose("开始标签结束"),
        textContent("标签文本内容"),
        closeTag("结束标签"),
        literal("在结束标签与开始标签之间的空白中"),
        commentTag("注释标签");

        private String name;

        TokenType(String name) {
            this.name = name;
        }
    }
}
