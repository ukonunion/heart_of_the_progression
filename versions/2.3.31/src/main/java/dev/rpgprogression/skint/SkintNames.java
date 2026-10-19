package dev.rpgprogression.skint;

/** Plain player-authored labels, with no formatting codes or control characters. */
public final class SkintNames {
    public static final int MAX_LENGTH=64;
    public static String clean(String text){
        if(text==null)return "";
        var out=new StringBuilder();
        for(int i=0;i<text.length();){
            int c=text.codePointAt(i);i+=Character.charCount(c);
            if(c==0xA7){if(i<text.length())i+=Character.charCount(text.codePointAt(i));continue;}
            if(Character.isISOControl(c)||Character.getType(c)==Character.FORMAT||Character.getType(c)==Character.SURROGATE)continue;
            if(out.length()+Character.charCount(c)>MAX_LENGTH)break;out.appendCodePoint(c);
        }
        return out.toString().strip();
    }
    private SkintNames(){}
}
